package com.deefacto.user_service.controller;

import com.deefacto.user_service.domain.dto.UserLoginDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deefacto.user_service.common.dto.ApiResponseDto;
import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.domain.dto.UserRegisterDto;
import com.deefacto.user_service.secret.jwt.dto.TokenDto;
import com.deefacto.user_service.service.UserService;

import jakarta.servlet.http.HttpServletRequest;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 인증 관련 API 엔드포인트를 제공하는 컨트롤러
 * 
 * 주요 기능:
 * 1. 사용자 회원가입 (/auth/register)
 * 2. 사용자 로그인 (/auth/login) - JWT 토큰 발급
 * 3. 사용자 로그아웃 (/auth/logout) - 토큰 무효화
 * 
 * 이 컨트롤러의 엔드포인트들은 인증이 필요하지 않은 공개 API입니다.
 * API Gateway에서 /auth/** 경로는 인증 없이 접근 가능하도록 설정되어 있습니다.
 */
@RestController
@RequestMapping("/auth")
// @RequestMapping("/")
@RequiredArgsConstructor
public class UserAuthController {
    
    // 사용자 인증 관련 비즈니스 로직 처리 서비스
    private final UserService userService;

    // 임시 데이터 저장용 Map (실제로는 불필요하지만 예시를 위해 유지)
    Map<String, String> data = new HashMap<>();

    /**
     * 새로운 사용자를 등록하는 API
     * 
     * 처리 과정:
     * 1. 요청 데이터 유효성 검증 (@Valid)
     * 2. UserService를 통한 사용자 등록
     * 3. 등록 성공 응답 반환
     * 
     * @param userRegisterDto 사용자 등록 정보 (사원번호, 비밀번호, 이름, 이메일 등)
     * @return 등록된 사용자의 사원번호
     */
    @PostMapping("/register")
    public ApiResponseDto<Map<String, String>> registerUser(
        @RequestBody @Valid UserRegisterDto userRegisterDto,
        @RequestHeader(value = "X-Role", required = false) String role
    ) {
        if (role == null || role.isEmpty()) {
            throw new BadParameter("X-Role header is required");
        }
        if (!role.equals("ADMIN")) {
            throw new BadParameter("You are not authorized to register user");
        }

        // UserService를 통해 사용자 등록 처리
        userService.registerUser(userRegisterDto);
        
        // 응답 데이터 구성
        data.put("employeeId", userRegisterDto.getEmployeeId());

        return ApiResponseDto.createOk(data, "회원 등록 성공");
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급하는 API
     * 
     * 처리 과정:
     * 1. 요청 데이터 유효성 검증 (@Valid)
     * 2. UserService를 통한 로그인 처리 및 토큰 발급
     * 3. 액세스 토큰과 리프레시 토큰 반환
     * 
     * @param userLoginDto 로그인 정보 (사원번호, 비밀번호)
     * @return 액세스 토큰과 리프레시 토큰이 포함된 응답
     */
    @PostMapping("/login")
    public ApiResponseDto<TokenDto.AccessRefreshToken> loginUser(@RequestBody @Valid UserLoginDto userLoginDto) {
        // UserService를 통해 로그인 처리 및 토큰 발급
        TokenDto.AccessRefreshToken token = userService.login(userLoginDto);
        
        return ApiResponseDto.createOk(token, "로그인 성공");
    }

    /**
     * 사용자 로그아웃을 처리하는 API
     * 
     * 처리 과정:
     * 1. Authorization 헤더에서 Bearer 토큰 추출
     * 2. UserService를 통한 로그아웃 처리 (Redis에 토큰 저장)
     * 3. 로그아웃 성공 응답 반환
     * 
     * 참고: API Gateway에서 이미 토큰을 검증하고 X-Employee-Id 헤더로 전달하지만,
     * 로그아웃은 클라이언트에서 토큰을 전송해야 하므로 Authorization 헤더에서 직접 추출합니다.
     * 
     * @param request HTTP 요청 객체 (Authorization 헤더 포함)
     * @return 로그아웃 성공 응답
     */
    @PostMapping("/logout")
    public ApiResponseDto<String> logoutUser(HttpServletRequest request) {
        // API Gateway에서 이미 토큰을 검증하고 X-Employee-Id 헤더로 전달
        // 로그아웃은 클라이언트에서 토큰을 전송해야 하므로
        // Authorization 헤더에서 토큰을 추출하여 처리
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 접두사를 제거하여 실제 토큰만 추출
            String token = bearerToken.substring(7);
            userService.logout(token);
        }
        return ApiResponseDto.createOk(null, "로그아웃 성공");
    }
    
}
