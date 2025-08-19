package com.deefacto.user_service.service;

import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.common.exception.NotFound;
import com.deefacto.user_service.domain.dto.*;
import com.deefacto.user_service.remote.service.UserCacheService;
import com.deefacto.user_service.secret.jwt.TokenGenerator;
import com.deefacto.user_service.secret.jwt.dto.TokenDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import com.deefacto.user_service.config.SecurityConfig.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deefacto.user_service.domain.repository.UserRepository;

import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.deefacto.user_service.domain.Entitiy.User;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 
 * 주요 기능:
 * 1. 사용자 등록 (회원가입)
 * 2. 사용자 로그인 및 토큰 발급
 * 3. 사용자 로그아웃 처리
 * 4. 비밀번호 변경 (예정)
 * 
 * API Gateway 아키텍처에 맞춰 JWT 토큰 검증은 API Gateway에서 처리하고,
 * 이 서비스에서는 토큰 생성과 사용자 관리에만 집중합니다.
 */
@Service
@Slf4j 
@RequiredArgsConstructor
public class UserService {
    
    // 사용자 데이터 접근을 위한 리포지토리
    private final UserRepository userRepository;
    
    // JWT 토큰 생성 및 검증을 위한 컴포넌트
    private final TokenGenerator tokenGenerator;
    
    // 비밀번호 암호화를 위한 인코더
    private final PasswordEncoder passwordEncoder;
    
    // 로그아웃 토큰 관리를 위한 Redis 템플릿
    private final RedisTemplate<String, String> redisTemplate;

    // Redis 유저 정보 사용을 위한 서비스
    private final UserCacheService userCacheService;

    // Redis 사용을 위한 ObjectMapper
    private final ObjectMapper objectMapper;

    // Redis에 저장되는 유저정보 TTL
    private final long USER_CACHE_TTL_MIN = 60;

    // API Gateway에서 이미 토큰을 검증하고 X-Employee-Id 헤더로 전달하므로
    // extractToken 메서드는 더 이상 필요하지 않음

    /**
     * 새로운 사용자를 등록하는 메서드
     * 
     * 처리 과정:
     * 1. 중복 사번 체크 (이미 존재하는 사번인지 확인)
     * 2. DTO를 엔티티로 변환 (데이터 전송 객체 → 데이터베이스 엔티티)
     * 3. 비밀번호를 BCrypt로 암호화 (단방향 해시, 복호화 불가)
     * 4. 기본값 설정 (권한, 활성여부, 근무시간)
     * 5. 등록자 정보 기록 (감사 로그용)
     * 6. 데이터베이스에 저장
     * 
     * @param userRegisterDto 사용자 등록 정보 DTO (사원번호, 비밀번호, 이름, 이메일 등)
     * @param createdBy 사용자를 등록한 관리자의 사원번호
     * @throws BadParameter 중복 사번으로 등록 시도하는 경우
     */
    @Transactional
    public void registerUser(UserRegisterDto userRegisterDto, String createdBy) {
        // 중복 사번 체크 (unique 제약조건 위반 방지)
        User existingUser = userRepository.findByEmployeeId(userRegisterDto.getEmployeeId());
        if (existingUser != null) {
            log.warn("중복 사번 등록 시도: {}", userRegisterDto.getEmployeeId());
            throw new BadParameter("이미 존재하는 사번입니다.");
        }
        
        // DTO를 엔티티로 변환 (데이터 전송 객체 → 데이터베이스 엔티티)
        User user = userRegisterDto.toEntity();
        
        // 비밀번호를 BCrypt로 암호화하여 저장
        // 원본 비밀번호는 복호화할 수 없고, 로그인 시 matches()로 검증
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 기본 권한 설정 (role이 null인 경우 USER로 설정)
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        
        // 활성 여부 설정 (새로 등록된 사용자는 기본적으로 활성 상태)
        user.setActive(true);

        // 기본 근무시간 설정 (shift가 null인 경우 기본값 "A" 설정)
        if (user.getShift() == null) {
            user.setShift("A");
        }

        // 등록자 및 수정자 정보 기록 (감사 로그용)
        user.setCreated_pr(createdBy);
        user.setUpdated_pr(createdBy);

        // 데이터베이스에 사용자 정보 저장
        userRepository.save(user);
        
        log.info("새로운 사용자가 등록되었습니다: {}", user.getEmployeeId());
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급하는 메서드
     * 
     * 처리 과정:
     * 1. 사원번호로 사용자 조회 (데이터베이스에서 사용자 정보 확인)
     * 2. 비밀번호 검증 (BCrypt matches로 암호화된 비밀번호와 비교)
     * 3. 중복 로그인 확인 (Redis에서 기존 토큰 확인)
     * 4. 액세스 토큰과 리프레시 토큰 발급 (JWT 생성)
     * 5. Redis에 사용자별 토큰 저장 (후입/선입 차단 정책 적용)
     * 
     * @param loginDto 로그인 정보 DTO (사원번호, 비밀번호)
     * @return 액세스 토큰과 리프레시 토큰이 포함된 DTO
     * @throws NotFound 사용자가 존재하지 않는 경우
     * @throws BadParameter 비밀번호가 일치하지 않는 경우
     * @throws BadParameter 중복 로그인으로 인한 차단 (후입/선입 차단 정책)
     */
    @Transactional
    public TokenDto.AccessRefreshToken login(UserLoginDto loginDto) {
        log.info("로그인 시도: 사원번호 {}", loginDto.getEmployeeId());
        
        // 사원번호로 사용자 조회 (데이터베이스에서 사용자 정보 확인)
        User user = userRepository.findByEmployeeId(loginDto.getEmployeeId());
        if (user == null) {
            log.warn("존재하지 않는 사용자: 사원번호 {}", loginDto.getEmployeeId());
            throw new NotFound("User/Password is incorrect");
        }
        
        // 디버깅을 위한 비밀번호 정보 로깅 (개발 환경에서만 사용)
        log.info("사용자 발견: {}, 저장된 비밀번호: {}", user.getEmployeeId(), user.getPassword());
        log.info("입력된 비밀번호: {}", loginDto.getPassword());
        
        // BCrypt를 사용하여 비밀번호 검증
        // encode()된 비밀번호와 원본 비밀번호를 비교 (단방향 해시 검증)
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("비밀번호 불일치: 사원번호 {}", loginDto.getEmployeeId());
            throw new BadParameter("User/Password is incorrect");
        }

        // 중복 로그인 확인 (후입/선입 차단 정책 - Redis 기반)
        checkDuplicateLogin(loginDto.getEmployeeId());

        log.info("로그인 성공: 사원번호 {}", loginDto.getEmployeeId());
        
        // 로그인 성공 시 액세스 토큰과 리프레시 토큰 발급 (JWT 생성)
        // JWT 내부에 employeeId 뿐 아니라 userId, role, shift 정보 포함 필요
        TokenDto.AccessRefreshToken token = tokenGenerator.generateAccessRefreshToken(loginDto.getEmployeeId());
        
        // Redis에 사용자별 토큰 저장 (후입/선입 차단 정책 적용)
        saveUserTokenToRedis(loginDto.getEmployeeId(), token);

        // Redis에 필요 유저 정보 저장
        userCacheService.saveUser(user, USER_CACHE_TTL_MIN);
        
        return token;
    }

    /**
     * 사용자 로그아웃을 처리하는 메서드
     * 
     * 처리 과정:
     * 1. 토큰 유효성 검증
     * 2. 토큰 만료 확인
     * 3. 액세스 토큰인지 확인 (리프레시 토큰은 로그아웃 불가)
     * 4. Redis에서 사용자별 토큰 삭제 (중복 로그인 방지 해제)
     * 5. Redis에 로그아웃 토큰 저장 (토큰 만료 시간까지)
     * 
     * @param token 로그아웃할 JWT 토큰
     * @throws BadParameter 토큰이 유효하지 않거나 만료되었거나 액세스 토큰이 아닌 경우
     */
    public void logout(String token) {
        // 토큰 유효성 검증
        if (!tokenGenerator.validateToken(token)) {
            throw new BadParameter("Invalid token");
        }
        
        // 토큰 만료 확인
        if (tokenGenerator.isTokenExpired(token)) {
            throw new BadParameter("Expired token");
        }
        
        // 액세스 토큰만 로그아웃 처리 가능 (리프레시 토큰은 로그아웃 불가)
        String tokenType = tokenGenerator.getTokenType(token);
        if (!"access".equals(tokenType)) {
            throw new BadParameter("Only access tokens can be logged out");
        }
        
        // 토큰에서 사용자 ID 추출
        String employeeId = tokenGenerator.getEmployeeIdFromToken(token);
        
        // 토큰의 남은 만료 시간을 계산
        long expirationTime = tokenGenerator.getExpirationTime(token);
        
        // Redis에서 사용자별 토큰 삭제 (중복 로그인 방지 해제)
        String userTokenKey = "user_token:" + employeeId;
        redisTemplate.delete(userTokenKey);
        
        // Redis에 로그아웃 토큰을 저장
        // 토큰이 만료될 때까지 "logout" 상태로 유지
        redisTemplate.opsForValue().set(token, "logout", expirationTime, TimeUnit.MILLISECONDS);
        
        log.info("로그아웃 처리 완료: 사원번호 {}, 토큰 타입 {}, 만료 시간 {}ms", employeeId, tokenType, expirationTime);
    }

    /**
     * 중복 로그인을 확인하는 메서드 (후입/선입 차단 정책)
     * 
     * Redis에서 사용자별 토큰을 확인하여 기존 로그인이 있는지 검사합니다.
     * 기존 로그인이 있으면 예외를 발생시켜 후입을 차단합니다.
     * 
     * @param employeeId 사용자 사원번호
     * @throws BadParameter 기존 로그인이 존재하는 경우
     */
    private void checkDuplicateLogin(String employeeId) {
        String userTokenKey = "user_token:" + employeeId;
        String existingToken = redisTemplate.opsForValue().get(userTokenKey);
        
        if (existingToken != null) {
            log.warn("중복 로그인 감지: 사원번호 {} (후입/선입 차단 정책 적용)", employeeId);
            throw new BadParameter("User is already logged in. Please logout from other devices first.");
        }
        
        log.debug("중복 로그인 없음: 사원번호 {}", employeeId);
    }
    
    /**
     * 사용자별 토큰을 Redis에 저장하는 메서드
     * 
     * 사용자 사원번호를 키로 하여 액세스 토큰을 저장합니다.
     * 토큰 만료 시간과 동일하게 Redis 키도 만료되도록 설정합니다.
     * 
     * @param employeeId 사용자 사원번호
     * @param token 발급된 토큰 DTO
     */
    private void saveUserTokenToRedis(String employeeId, TokenDto.AccessRefreshToken token) {
        String userTokenKey = "user_token:" + employeeId;
        String accessToken = token.getAccess().getToken();
        long expirationTime = token.getAccess().getExpiresIn() * 1000L; // 밀리초 단위
        
        // 사용자별 토큰 저장 (후입/선입 차단용)
        redisTemplate.opsForValue().set(userTokenKey, accessToken, expirationTime, TimeUnit.MILLISECONDS);
        
        // 토큰별 상태 저장 (로그아웃 감지용)
        redisTemplate.opsForValue().set(accessToken, "login", expirationTime, TimeUnit.MILLISECONDS);
        
        log.info("사용자 토큰 저장 완료: 사원번호 {}, 만료 시간 {}ms", employeeId, expirationTime);
    }
    
    /**
     * 사용자 비밀번호를 변경하는 메서드 (구현 예정)
     * 
     * @param token 현재 사용자의 JWT 토큰
     * @param oldPassword 현재 비밀번호
     * @param newPassword 새로운 비밀번호
     */
    public void changePassword(String employeeId, UserChangePasswordDto userChangePasswordDto) {
        // 1. 현재 비밀번호 검증
        User user = userRepository.findByEmployeeId(employeeId);
        if (user == null) {
            throw new NotFound("User not found");
        }
        if (!passwordEncoder.matches(userChangePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new BadParameter("Current password is incorrect");
        }
        // 2. 새 비밀번호 유효성 검증
        if (userChangePasswordDto.getNewPassword().length() < 8) {
            throw new BadParameter("New password must be at least 8 characters long");
        }
        if (userChangePasswordDto.getNewPassword().equals(userChangePasswordDto.getCurrentPassword())) {
            throw new BadParameter("New password cannot be the same as the current password");
        }
        // 3. 비밀번호 업데이트
        user.setPassword(passwordEncoder.encode(userChangePasswordDto.getNewPassword()));

        // 정보 변경 시점의 시간 기록
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("비밀번호 변경 완료: 사원번호 {}", employeeId);
    }

    public void deleteUser(UserDeleteDto userDeleteDto) {
        String deleteEmployeeId = userDeleteDto.getEmployeeId();
        User user = userRepository.findByEmployeeId(deleteEmployeeId);
        if (user == null) {
            throw new NotFound("User not found");
        }
        userRepository.delete(user);
        log.info("사용자 삭제 완료: 사원번호 {}", deleteEmployeeId);
    }
    
    /**
     * 사용자 목록을 페이징으로 검색하는 메서드
     * 
     * @param searchDto 검색 조건 DTO
     * @return 검색 결과 페이지 (UserInfoResponseDto로 변환)
     */
    public Page<UserInfoResponseDto> searchUsers(UserSearchDto searchDto) {
        // 페이징 정보 생성
        Pageable pageable = PageRequest.of(
            searchDto.getPage() != null ? searchDto.getPage() : 0,
            searchDto.getSize() != null ? searchDto.getSize() : 10
        );
        
        // 검색 조건에서 null 값 처리
        String name = searchDto.getName() != null && !searchDto.getName().trim().isEmpty() 
            ? searchDto.getName().trim() : null;
        String email = searchDto.getEmail() != null && !searchDto.getEmail().trim().isEmpty() 
            ? searchDto.getEmail().trim() : null;
        String employeeId = searchDto.getEmployeeId() != null && !searchDto.getEmployeeId().trim().isEmpty() 
            ? searchDto.getEmployeeId().trim() : null;
        
        log.info("사용자 검색: 페이지={}, 크기={}, 이름={}, 이메일={}, 사원번호={}", 
            pageable.getPageNumber(), pageable.getPageSize(), name, email, employeeId);
        
        // 조건부 검색 실행
        Page<User> userPage = userRepository.findByConditions(name, email, employeeId, pageable);
        
        // User 엔티티를 UserInfoResponseDto로 변환
        Page<UserInfoResponseDto> result = userPage.map(UserInfoResponseDto::from);
        
        log.info("검색 결과: 총 {}개 중 {}개 조회", result.getTotalElements(), result.getContent().size());
        
        return result;
    }

    /**
     * 사용자 정보를 변경하는 메서드
     * 
     * 처리 과정:
     * 1. 사원번호로 사용자 조회
     * 2. 변경할 정보가 null이 아닌 경우에만 해당 필드 업데이트
     * 3. 변경 시점의 시간과 변경자 정보 기록
     * 4. 데이터베이스에 변경사항 저장
     * 
     * @param userInfoResponseDto 변경할 사용자 정보 DTO
     * @param updatedBy 변경을 수행한 관리자의 사원번호
     * @throws NotFound 사용자가 존재하지 않는 경우
     */
    @Transactional
    public void changeUserInfo(UserInfoResponseDto userInfoResponseDto, String updatedBy) {
        // 사원번호로 사용자 조회
        User user = userRepository.findByEmployeeId(userInfoResponseDto.getEmployeeId());
        if (user == null) {
            throw new NotFound("User not found");
        }
        
        // 디버깅을 위한 변경 전후 정보 로깅
        log.info("변경 전 사용자 정보: {}", user);
        log.info("변경할 정보: {}", userInfoResponseDto);
        
        // 각 필드별 null 체크 후 업데이트 (부분 업데이트 지원)
        if (userInfoResponseDto.getName() != null) {
            user.setName(userInfoResponseDto.getName());
            log.info("이름 변경: {}", userInfoResponseDto.getName());
        }
        if (userInfoResponseDto.getEmail() != null) {
            user.setEmail(userInfoResponseDto.getEmail());
            log.info("이메일 변경: {}", userInfoResponseDto.getEmail());
        }
        if (userInfoResponseDto.getGender() != null) {
            user.setGender(userInfoResponseDto.getGender());
            log.info("성별 변경: {}", userInfoResponseDto.getGender());
        }
        if (userInfoResponseDto.getDepartment() != null) {
            user.setDepartment(userInfoResponseDto.getDepartment());
            log.info("부서 변경: {}", userInfoResponseDto.getDepartment());
        }
        if (userInfoResponseDto.getPosition() != null) {
            user.setPosition(userInfoResponseDto.getPosition());
            log.info("직급 변경: {}", userInfoResponseDto.getPosition());
        }
        if (userInfoResponseDto.getRole() != null) {
            user.setRole(userInfoResponseDto.getRole());
            log.info("권한 변경: {}", userInfoResponseDto.getRole());
        }
        if (userInfoResponseDto.getShift() != null) {
            user.setShift(userInfoResponseDto.getShift());
            log.info("근무시간 변경: {}", userInfoResponseDto.getShift());
        }
        
        // boolean 필드는 null 체크 없이 직접 설정 (기본값 false)
        user.setActive(userInfoResponseDto.isActive());
        log.info("활성여부 변경: {}", userInfoResponseDto.isActive());
        
        // 정보 변경 시점의 시간과 변경자 기록 (감사 로그용)
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdated_pr(updatedBy);
        
        // 변경된 사용자 정보를 데이터베이스에 저장
        User savedUser = userRepository.save(user);
        log.info("변경 후 사용자 정보: {}", savedUser);
        
        log.info("사용자 정보 변경 완료: 사원번호 {}, 변경자 {}", userInfoResponseDto.getEmployeeId(), updatedBy);
    }
}
