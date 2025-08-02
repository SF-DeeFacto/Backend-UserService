package com.deefacto.user_service.domain.Enum;

/**
 * 사용자 권한 열거형
 * 
 * 사용자의 시스템 권한을 관리합니다.
 * 데이터베이스에는 문자열로 저장됩니다.
 */
public enum UserRole {
    USER,   // 일반 사용자
    ADMIN   // 관리자
}
