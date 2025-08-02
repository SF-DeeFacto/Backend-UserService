package com.deefacto.user_service.service;

import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.common.exception.NotFound;
import com.deefacto.user_service.domain.dto.UserLoginDto;
import com.deefacto.user_service.secret.jwt.TokenGenerator;
import com.deefacto.user_service.secret.jwt.dto.TokenDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deefacto.user_service.domain.repository.UserRepository;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.deefacto.user_service.domain.dto.UserRegisterDto;
import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.Enum.UserRole;

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
     * 3. 액세스 토큰과 리프레시 토큰 발급
     * 
     * @param loginDto 로그인 정보 DTO (사원번호, 비밀번호)
     * @return 액세스 토큰과 리프레시 토큰이 포함된 DTO
     * @throws NotFound 사용자가 존재하지 않는 경우
     * @throws BadParameter 비밀번호가 일치하지 않는 경우
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

        log.info("로그인 성공: 사원번호 {}", loginDto.getEmployeeId());
        
        // 로그인 성공 시 액세스 토큰과 리프레시 토큰 발급
        return tokenGenerator.generateAccessRefreshToken(loginDto.getEmployeeId());
    }

    /**
     * 사용자 로그아웃을 처리하는 메서드
     * 
     * 처리 과정:
     * 1. 토큰 유효성 검증
     * 2. 토큰 만료 확인
     * 3. 액세스 토큰인지 확인 (리프레시 토큰은 로그아웃 불가)
     * 4. Redis에 로그아웃 토큰 저장 (토큰 만료 시간까지)
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
        
        // 토큰의 남은 만료 시간을 계산
        long expirationTime = tokenGenerator.getExpirationTime(token);
        
        // Redis에 로그아웃 토큰을 저장
        // 토큰이 만료될 때까지 "logout" 상태로 유지
        redisTemplate.opsForValue().set(token, "logout", expirationTime, TimeUnit.MILLISECONDS);
        
        log.info("로그아웃 처리 완료: 토큰 타입 {}, 만료 시간 {}ms", tokenType, expirationTime);
    }

    /**
     * 사용자 비밀번호를 변경하는 메서드 (구현 예정)
     * 
     * @param token 현재 사용자의 JWT 토큰
     * @param oldPassword 현재 비밀번호
     * @param newPassword 새로운 비밀번호
     */
    public void changePassword(String token, String oldPassword, String newPassword) {
        // TODO: 비밀번호 변경 처리
        // 1. 토큰에서 사용자 ID 추출
        // 2. 현재 비밀번호 검증
        // 3. 새 비밀번호 유효성 검증
        // 4. 비밀번호 업데이트
    }
    
}
