package com.deefacto.user_service.controller;

import com.deefacto.user_service.common.dto.ApiResponseDto;
import com.deefacto.user_service.common.exception.BadParameter;
import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.dto.UserChangePasswordDto;
import com.deefacto.user_service.domain.dto.UserDeleteDto;
import com.deefacto.user_service.domain.dto.UserSearchDto;
import com.deefacto.user_service.domain.dto.UserInfoResponseDto;
import com.deefacto.user_service.domain.repository.UserRepository;
import com.deefacto.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

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
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    // 사용자 비즈니스 로직 처리 서비스
    private final UserService userService;
    
    // 사용자 데이터 접근을 위한 리포지토리
    private final UserRepository userRepository;
    
    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회하는 API
     * 
     * @param employeeId API Gateway에서 파싱한 사용자 사원번호 (X-Employee-Id 헤더)
     * @param role API Gateway에서 파싱한 사용자 역할 (X-Role 헤더)
     * @return 사용자 프로필 정보 (사원번호, 역할, 이름, 이메일)
     * @throws BadParameter 필수 헤더가 누락된 경우
     */
    @GetMapping("/info/profile")
    public ApiResponseDto<UserInfoResponseDto> getProfile(
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
        
        // UserInfoResponseDto로 변환
        UserInfoResponseDto profile = UserInfoResponseDto.from(user);
        
        return ApiResponseDto.createOk(profile, "프로필 조회 성공");
    }
    
    
    /**
     * 전체 사용자 목록을 페이징으로 조회하는 API
     * 
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param name 검색할 이름 (선택사항)
     * @param email 검색할 이메일 (선택사항)
     * @return 페이징된 사용자 목록
     */
    @GetMapping("/info/search")
    public ApiResponseDto<Page<UserInfoResponseDto>> searchUsers(
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "email", required = false) String email
    ) {
        // 검색 조건 DTO 생성 (employeeId는 null로 설정하여 전체 조회)
        UserSearchDto searchDto = new UserSearchDto(page, size, name, email, null);
        
        // 사용자 검색 실행
        Page<UserInfoResponseDto> result = userService.searchUsers(searchDto);
        
        return ApiResponseDto.createOk(result, "사용자 목록 조회 성공");
    }

    // 사용자 정보 변경
    @PostMapping("info/change")
    public ApiResponseDto<String> changeUserInfo(
        @RequestHeader(value = "X-Employee-Id", required = false) String adminEmployeeId,
        @RequestHeader(value = "X-Role", required = false) String role,
        @RequestBody @Valid UserInfoResponseDto userInfoResponseDto
    ) {
        if (adminEmployeeId == null || adminEmployeeId.isEmpty()) {
            throw new BadParameter("X-Employee-Id header is required");
        }
        if (role == null || role.isEmpty()) {
            throw new BadParameter("X-Role header is required");
        }
        if (!role.equals("ADMIN")) {
            throw new BadParameter("You are not authorized to change user info");
        }
        userService.changeUserInfo(userInfoResponseDto, adminEmployeeId);
        return ApiResponseDto.createOk(null, "사용자 정보 변경 성공");
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
    @PostMapping("/info/password")
    public ApiResponseDto<String> changePassword(
        @RequestHeader(value = "X-Employee-Id", required = false) String employeeId,
        @RequestHeader(value = "X-Role", required = false) String role,
        @RequestBody @Valid UserChangePasswordDto userChangePasswordDto
    ) {
        // API Gateway에서 전달받은 헤더 검증
        if (employeeId == null || employeeId.isEmpty()) {
            throw new BadParameter("X-Employee-Id header is required");
        }
        
        // 본인 또는 ADMIN만 비밀번호 변경 가능
        if (!employeeId.equals(userChangePasswordDto.getEmployeeId()) && 
            !"ADMIN".equals(role)) {
            throw new BadParameter("You can only change your own password or need ADMIN role");
        }
        
        userService.changePassword(employeeId, userChangePasswordDto);
        
        return ApiResponseDto.createOk(null, "비밀번호 변경 성공");
    }

    // 사용자 삭제
    @PostMapping("/delete")
    public ApiResponseDto<String> deleteUser(
        @RequestHeader(value = "X-Employee-Id", required = false) String adminEmployeeId,
        @RequestHeader(value = "X-Role", required = false) String role,
        @RequestBody @Valid UserDeleteDto userDeleteDto
    ) {
        if (adminEmployeeId == null || adminEmployeeId.isEmpty()) {
            throw new BadParameter("X-Employee-Id header is required");
        }
        if (role == null || role.isEmpty()) {
            throw new BadParameter("X-Role header is required");
        }
        if (!role.equals("ADMIN")) {
            throw new BadParameter("You are not authorized to delete user");
        }
        userService.deleteUser(userDeleteDto);
        return ApiResponseDto.createOk(null, "사용자 삭제 성공");
    }

} 