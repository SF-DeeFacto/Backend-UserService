package com.deefacto.user_service.domain.Entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 
 * 사용자의 기본 정보와 인증 정보를 관리합니다.
 * JPA를 통해 데이터베이스의 users 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "user")
public class User {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true)
    @Getter @Setter
    private String employeeId; // 사번

    @Column(name = "password", nullable = false)
    @Getter @Setter
    private String password; // 암호화 비밀번호

    @Column(name = "name", nullable = false)
    @Getter @Setter
    private String name; // 이름

    @Column(name = "email", nullable = false, unique = true)
    @Getter @Setter
    private String email; // 이메일

    @Column(name = "gender", nullable = false)
    @Getter @Setter
    private String gender; // 성별

    @Column(name = "department", nullable = false)
    @Getter @Setter
    private String department; // 부서

    @Column(name = "position", nullable = false)
    @Getter @Setter
    private String position; // 직급

    @Column(name = "role", nullable = false)
    @Getter @Setter
    private String role; // 권한 (ROOT, ADMIN, USER)

    @Column(name = "scope", nullable = false)
    @Getter @Setter
    private String scope; // 구역 범위 (a,b,c)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Getter @Setter
    private LocalDateTime updatedAt; // 수정일

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    @Getter @Setter
    private boolean isActive; // 활성 여부

    @Column(name = "shift")
    @Getter @Setter
    private String shift; // 근무 시간 (DAY, NIGHT)

    @Column(name = "created_pr", nullable = false)
    @Getter @Setter
    private String created_pr; // 등록자

    @Column(name = "updated_pr")
    @Getter @Setter
    private String updated_pr; // 수정자

    
}
