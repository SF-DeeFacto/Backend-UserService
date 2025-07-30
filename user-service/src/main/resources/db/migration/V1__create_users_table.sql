-- 사용자 테이블 생성

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    employee_id VARCHAR(20) NOT NULL UNIQUE,     -- 사번 (로그인 ID)
    password VARCHAR(255) NOT NULL,              -- 암호화된 비밀번호
    name VARCHAR(50) NOT NULL,                   -- 이름
    contact VARCHAR(20) NOT NULL,                -- 연락처
    email VARCHAR(100) NOT NULL,                 -- 이메일
    gender VARCHAR(10) NOT NULL,                 -- 성별
    department VARCHAR(30) NOT NULL,             -- 부서
    position VARCHAR(30) NOT NULL,               -- 직급
    role VARCHAR(10) NOT NULL,                   -- 권한 (USER / ADMIN)

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);