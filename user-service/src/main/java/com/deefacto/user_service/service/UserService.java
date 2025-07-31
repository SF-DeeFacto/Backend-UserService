package com.deefacto.user_service.service;

import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.common.exception.NotFound;
import com.deefacto.user_service.domain.dto.UserLoginDto;
import com.deefacto.user_service.secret.jwt.TokenGenerator;
import com.deefacto.user_service.secret.jwt.dto.TokenDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deefacto.user_service.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.deefacto.user_service.domain.dto.UserRegisterDto;
import com.deefacto.user_service.domain.Entitiy.User;



@Service
@Slf4j 
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void registerUser(UserRegisterDto userRegisterDto) {
        User user = userRegisterDto.toEntity();
        // TODO: 비밀번호 암호화 처리
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        
        // TODO: 사용자 권한 처리
        // TODO: 사용자 생성일 처리
        // TODO: 사용자 수정일 처리
        // TODO: 사용자 삭제일 처리
        // TODO: 사용자 삭제 여부 처리
        // TODO: 사용자 삭제 여부 처리
        userRepository.save(user);
    }

    @Transactional
    public TokenDto.AccessRefreshToken login(UserLoginDto loginDto) {
        log.info("Login attempt for employeeId: {}", loginDto.getEmployeeId());
        
        User user = userRepository.findByEmployeeId(loginDto.getEmployeeId());
        if (user == null) {
            log.warn("User not found for employeeId: {}", loginDto.getEmployeeId());
            throw new NotFound("User/Password is incorrect");
        }
        
        log.info("User found: {}, stored password: {}", user.getEmployeeId(), user.getPassword());
        log.info("Input password: {}", loginDto.getPassword());
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            log.warn("Password mismatch for employeeId: {}", loginDto.getEmployeeId());
            throw new BadParameter("User/Password is incorrect");
        }

        log.info("Login successful for employeeId: {}", loginDto.getEmployeeId());
        return tokenGenerator.generateAccessRefreshToken(loginDto.getEmployeeId());
    }

    // TODO: 토큰 검증 관련 서비스 메서드들 구현
    // - validateToken(String token): 토큰 유효성 검증
    // - refreshAccessToken(String refreshToken): 리프레시 토큰으로 새 액세스 토큰 발급
    // - getCurrentUser(String token): 토큰으로 현재 사용자 정보 조회
    // - changePassword(String token, String oldPassword, String newPassword): 비밀번호 변경
}
