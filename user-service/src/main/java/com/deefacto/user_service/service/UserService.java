package com.deefacto.user_service.service;

import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.common.exception.NotFound;
import com.deefacto.user_service.domain.dto.UserLoginDto;
import com.deefacto.user_service.secret.jwt.TokenGenerator;
import com.deefacto.user_service.secret.jwt.dto.TokenDto;
import org.springframework.data.redis.core.RedisTemplate;
import com.deefacto.user_service.config.SecurityConfig.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deefacto.user_service.domain.repository.UserRepository;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.deefacto.user_service.domain.dto.UserRegisterDto;
import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.Enum.UserRole;
import com.deefacto.user_service.domain.dto.UserChangePasswordDto;

import jakarta.servlet.http.HttpServletRequest;

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

    // API Gateway에서 이미 토큰을 검증하고 X-Employee-Id 헤더로 전달하므로
    // extractToken 메서드는 더 이상 필요하지 않음

    /**
     * 새로운 사용자를 등록하는 메서드
     * 
     * 처리 과정:
     * 1. DTO를 엔티티로 변환
     * 2. 비밀번호를 BCrypt로 암호화
     * 3. 기본 권한 설정 (role이 null인 경우 USER로 설정)
     * 4. 데이터베이스에 저장
     * 
     * @param userRegisterDto 사용자 등록 정보 DTO
     */
    @Transactional
    public void registerUser(UserRegisterDto userRegisterDto) {
        // DTO를 엔티티로 변환
        User user = userRegisterDto.toEntity();
        
        // 비밀번호를 BCrypt로 암호화하여 저장
        // 원본 비밀번호는 복호화할 수 없고, 로그인 시 matches()로 검증
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 기본 권한 설정 (role이 null인 경우 USER로 설정)
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        // 데이터베이스에 사용자 정보 저장
        userRepository.save(user);
        
        log.info("새로운 사용자가 등록되었습니다: {}", user.getEmployeeId());
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급하는 메서드
     * 
     * 처리 과정:
     * 1. 사원번호로 사용자 조회
     * 2. 비밀번호 검증 (BCrypt matches 사용)
     * 3. 중복 로그인 확인 (Redis 기반)
     * 4. 액세스 토큰과 리프레시 토큰 발급
     * 5. Redis에 사용자별 토큰 저장 (후입/선입 차단 정책)
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
        
        // 사원번호로 사용자 조회
        User user = userRepository.findByEmployeeId(loginDto.getEmployeeId());
        if (user == null) {
            log.warn("존재하지 않는 사용자: 사원번호 {}", loginDto.getEmployeeId());
            throw new NotFound("User/Password is incorrect");
        }
        
        log.info("사용자 발견: {}, 저장된 비밀번호: {}", user.getEmployeeId(), user.getPassword());
        log.info("입력된 비밀번호: {}", loginDto.getPassword());
        
        // BCrypt를 사용하여 비밀번호 검증
        // encode()된 비밀번호와 원본 비밀번호를 비교
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("비밀번호 불일치: 사원번호 {}", loginDto.getEmployeeId());
            throw new BadParameter("User/Password is incorrect");
        }

        // 중복 로그인 확인 (후입/선입 차단 정책)
        checkDuplicateLogin(loginDto.getEmployeeId());

        log.info("로그인 성공: 사원번호 {}", loginDto.getEmployeeId());
        
        // 로그인 성공 시 액세스 토큰과 리프레시 토큰 발급
        TokenDto.AccessRefreshToken token = tokenGenerator.generateAccessRefreshToken(loginDto.getEmployeeId());
        
        // Redis에 사용자별 토큰 저장 (후입/선입 차단 정책)
        saveUserTokenToRedis(loginDto.getEmployeeId(), token);
        
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
        userRepository.save(user);
        log.info("비밀번호 변경 완료: 사원번호 {}", employeeId);
    }
    
}
