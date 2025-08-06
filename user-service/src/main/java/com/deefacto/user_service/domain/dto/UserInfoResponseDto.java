package com.deefacto.user_service.domain.dto;

import com.deefacto.user_service.domain.Entitiy.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답을 위한 DTO 클래스
 * 
 * id와 password를 제외한 모든 사용자 정보를 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponseDto {
    
    /**
     * 사원번호
     */
    private String employeeId;
    
    /**
     * 이름
     */
    private String name;
    
    /**
     * 이메일
     */
    private String email;
    
    /**
     * 성별
     */
    private String gender;
    
    /**
     * 부서
     */
    private String department;
    
    /**
     * 직급
     */
    private String position;
    
    /**
     * 권한
     */
    private String role;
    
    /**
     * 생성일
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정일
     */
    private LocalDateTime updatedAt;

    /**
     * 근무 시간
     */
    private String shift;

    /**
     * 등록자
     */
    private String created_pr;

    /**
     * 활성 여부
     */
    private boolean isActive;

    /**
     * 수정자
     */
    private String updated_pr;
    
    /**
     * User 엔티티를 UserInfoResponseDto로 변환하는 정적 메서드
     * 
     * @param user 변환할 User 엔티티
     * @return UserInfoResponseDto 객체
     */
    public static UserInfoResponseDto from(User user) {
        if (user == null) {
            return null;
        }
        
        return UserInfoResponseDto.builder()
            .employeeId(user.getEmployeeId())
            .name(user.getName())
            .email(user.getEmail())
            .gender(user.getGender())
            .department(user.getDepartment())
            .position(user.getPosition())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .shift(user.getShift())
            .created_pr(user.getCreated_pr())
            .isActive(user.isActive())
            .updated_pr(user.getUpdated_pr())
            .build();
    }
} 