package com.deefacto.user_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 사용자 검색을 위한 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto {
    
    /**
     * 페이지 번호 (0부터 시작)
     */
    private Integer page = 0;
    
    /**
     * 페이지 크기
     */
    private Integer size = 10;
    
    /**
     * 검색할 이름 (선택사항)
     */
    private String name;
    
    /**
     * 검색할 이메일 (선택사항)
     */
    private String email;
    
    /**
     * 검색할 사원번호 (선택사항)
     */
    private String employeeId;
} 