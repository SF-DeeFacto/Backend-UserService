package com.deefacto.user_service.controller;

import com.deefacto.user_service.common.dto.ApiResponseDto;
import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.repository.UserRepository;
import com.deefacto.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관련 API 엔드포인트를 제공하는 컨트롤러
 * 
 * API Gateway 아키텍처에 맞춰 다음과 같이 동작:
 * 1. API Gateway에서 JWT 토큰을 검증하고 파싱
 * 2. X-Employee-Id, X-Role 헤더를 추가하여 User Service로 전달
 * 3. 이 컨트롤러에서는 @RequestHeader로 직접 헤더 값을 받아 사용
 * 
 * 주요 기능:
 * - 사용자 프로필 조회
 * - 현재 사용자 정보 조회
 * - 비밀번호 변경 (예정)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    // 사용자 비즈니스 로직 처리 서비스
    private final UserService userService;
    
    // 사용자 데이터 접근을 위한 리포지토리
    private final UserRepository userRepository;
    
    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회하는 API
     * 
     * API Gateway에서 전달받은 헤더를 사용하여 사용자 정보를 조회합니다.
     * 
     * @param employeeId API Gateway에서 파싱한 사용자 사원번호 (X-Employee-Id 헤더)
     * @param role API Gateway에서 파싱한 사용자 역할 (X-Role 헤더)
     * @return 사용자 프로필 정보 (사원번호, 역할, 이름, 이메일)
     * @throws BadParameter 필수 헤더가 누락된 경우
     */
    @GetMapping("/profile")
    public ApiResponseDto<Map<String, Object>> getProfile(
        @RequestHeader(value = "X-Employee-Id", required = false) String employeeId,
        @RequestHeader(value = "X-Role", required = false) String role
    ) {
        // API Gateway에서 전달받은 헤더 검증
        if (employeeId == null || employeeId.isEmpty()) {
            throw new BadParameter("X-Employee-Id header is required");
        }
        if (role == null || role.isEmpty()) {
            throw new BadParameter("X-Role header is required");
        }
        
        // API Gateway에서 이미 파싱된 정보를 바로 사용하여 데이터베이스 조회
        User user = userRepository.findByEmployeeId(employeeId);
        
        // 응답 데이터 구성
        Map<String, Object> profile = new HashMap<>();
        profile.put("employeeId", employeeId);
        profile.put("role", role);
        profile.put("name", user != null ? user.getName() : "Unknown");
        profile.put("email", user != null ? user.getEmail() : "Unknown");
        
        return ApiResponseDto.createOk(profile, "프로필 조회 성공");
    }
    
    /**
     * 현재 로그인한 사용자의 기본 정보를 조회하는 API
     * 
     * @param employeeId API Gateway에서 파싱한 사용자 사원번호 (X-Employee-Id 헤더)
     * @return 현재 사용자 기본 정보
     * @throws BadParameter X-Employee-Id 헤더가 누락된 경우
     */
    @GetMapping("/me")
    public ApiResponseDto<Map<String, String>> getCurrentUser(
        @RequestHeader(value = "X-Employee-Id", required = false) String employeeId
    ) {
        // API Gateway에서 전달받은 헤더 검증
        if (employeeId == null || employeeId.isEmpty()) {
            throw new BadParameter("X-Employee-Id header is required");
        }
        
        // 이미 파싱된 employeeId를 바로 사용하여 응답 구성
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("employeeId", employeeId);
        userInfo.put("message", "현재 로그인한 사용자 정보");
        
        return ApiResponseDto.createOk(userInfo, "현재 사용자 정보 조회 성공");
    }
    
    /**
     * 사용자 비밀번호를 변경하는 API (구현 예정)
     * 
     * @param employeeId API Gateway에서 파싱한 사용자 사원번호 (X-Employee-Id 헤더)
     * @param oldPassword 현재 비밀번호
     * @param newPassword 새로운 비밀번호
     * @return 비밀번호 변경 결과
     * @throws BadParameter X-Employee-Id 헤더가 누락된 경우
     */
    @PostMapping("/change-password")
    public ApiResponseDto<String> changePassword(
        @RequestHeader(value = "X-Employee-Id", required = false) String employeeId,
        @RequestParam String oldPassword,
        @RequestParam String newPassword
    ) {
        // API Gateway에서 전달받은 헤더 검증
        if (employeeId == null || employeeId.isEmpty()) {
            throw new BadParameter("X-Employee-Id header is required");
        }
        
        // API Gateway에서 이미 검증된 employeeId 사용
        // TODO: 비밀번호 변경 로직 구현
        // 1. 현재 비밀번호 검증
        // 2. 새 비밀번호 유효성 검증
        // 3. 비밀번호 업데이트
        
        return ApiResponseDto.createOk(null, "비밀번호 변경 성공");
    }
} 