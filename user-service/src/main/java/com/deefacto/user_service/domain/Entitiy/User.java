package com.deefacto.user_service.domain.Entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.deefacto.user_service.domain.Enum.UserRole;
import com.deefacto.user_service.domain.Enum.UserGender;

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

    @Column(name = "email", nullable = false)
    @Getter @Setter
    private String email; // 이메일

    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    @Getter @Setter
    private UserGender gender; // 성별

    @Column(name = "department", nullable = false)
    @Getter @Setter
    private String department; // 부서

    @Column(name = "position", nullable = false)
    @Getter @Setter
    private String position; // 직급

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Getter @Setter
    private UserRole role; // 권한

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private LocalDateTime createdAt; // 생성일

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Getter @Setter
    private LocalDateTime updatedAt; // 수정일
}
