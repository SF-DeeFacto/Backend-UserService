-- ========================================
-- 사용자 테이블 생성 마이그레이션
-- ========================================
-- 
-- 파일명: V1__create_users_table.sql
-- 설명: User Service의 핵심 테이블인 users 테이블을 생성합니다.
-- 
-- Flyway 마이그레이션 규칙:
-- - V1: 버전 1 (첫 번째 마이그레이션)
-- - __: 구분자
-- - create_users_table: 마이그레이션 설명
-- - .sql: SQL 파일 확장자
-- 
-- 실행 시점: 애플리케이션 시작 시 자동 실행 (Flyway enabled: true인 경우)

-- 사용자 테이블 생성
CREATE TABLE user (
    -- 기본 키 (자동 증가)
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 사용자 식별 정보
    employee_id VARCHAR(20) NOT NULL UNIQUE,     -- 사번 (로그인 ID로 사용, 중복 불가)
    password VARCHAR(255) NOT NULL,              -- BCrypt로 암호화된 비밀번호 (최대 255자)
    name VARCHAR(50) NOT NULL,                   -- 사용자 이름 (최대 50자)
    email VARCHAR(100) NOT NULL,                 -- 이메일 주소 (최대 100자)
    gender VARCHAR(10) NOT NULL,                 -- 성별 (MALE/FEMALE/OTHER)
    department VARCHAR(30) NOT NULL,             -- 소속 부서 (최대 30자)
    position VARCHAR(30) NOT NULL,               -- 직급 (최대 30자)
    role VARCHAR(10) NOT NULL,                   -- 사용자 권한 (USER/ADMIN)

    -- 생성 및 수정 시간 (자동 관리)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 레코드 생성 시간
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- 레코드 수정 시간,
    
    -- 체크 제약조건 추가
    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN'))
);

-- 인덱스 생성 (성능 최적화)
-- 사번으로 빠른 조회를 위한 인덱스 (UNIQUE 제약조건으로 인해 자동 생성됨)
-- 이메일로 빠른 조회를 위한 인덱스 (필요시 추가)
-- CREATE INDEX idx_users_email ON users(email);