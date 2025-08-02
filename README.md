# User Service

## 📋 프로젝트 개요

User Service는 API Gateway 아키텍처에서 사용자 인증 및 관리 기능을 담당하는 마이크로서비스입니다.

## 🏗️ 아키텍처

### API Gateway와의 역할 분담

- **API Gateway**: JWT 토큰 검증, 파싱, 헤더 추가
- **User Service**: 토큰 생성, 사용자 관리, 로그아웃 처리

### 주요 기능

- 사용자 회원가입/로그인
- JWT 토큰 발급 (액세스 토큰 + 리프레시 토큰)
- 사용자 프로필 조회
- 로그아웃 처리 (Redis 기반 토큰 무효화)

## 🚀 시작하기

### 1. 환경 변수 설정

```bash
# env.example 파일을 .env로 복사
cp env.example .env

# .env 파일에서 실제 값으로 수정
# 예시:
SERVER_PORT=8081
DB_HOST=localhost
DB_PASSWORD=your_password
JWT_SECRET_KEY=your_secret_key
```

### 2. 인프라 서비스 실행

```bash
# Docker Compose로 MySQL, Redis 실행
docker-compose up -d

# 서비스 상태 확인
docker-compose ps
```

### 3. 애플리케이션 실행

#### 방법 1: 스크립트 사용 (권장)
```bash
# .env 파일을 자동으로 로드하고 애플리케이션 실행
./run-with-env.sh
```

#### 방법 2: Gradle 직접 실행
```bash
# 환경 변수를 수동으로 로드 후 실행
export $(cat .env | grep -v '^#' | xargs) && cd user-service && ./gradlew bootRun
```

#### 방법 3: IDE에서 실행
- **VSCode**: F5 키 또는 Run and Debug 패널에서 "UserServiceApplication" 선택
- **IntelliJ IDEA**: Run/Debug Configurations에서 "UserServiceApplication" 선택

> 💡 **참고**: IDE 설정에서 .env 파일이 자동으로 로드되도록 구성되어 있습니다.

## ⚙️ 환경 변수 설정

### 필수 환경 변수

| 변수명 | 설명 | 기본값 | 예시 |
|--------|------|--------|------|
| `SERVER_PORT` | 서버 포트 | 8081 | 8081 |
| `DB_HOST` | MySQL 호스트 | localhost | localhost |
| `DB_PORT` | MySQL 포트 | 3306 | 3306 |
| `DB_NAME` | 데이터베이스 이름 | deefacto_db | deefacto_db |
| `DB_USERNAME` | 데이터베이스 사용자명 | deefacto | deefacto |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | deefacto1234 | your_password |
| `REDIS_HOST` | Redis 호스트 | localhost | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 | 6379 |
| `JWT_SECRET_KEY` | JWT 시크릿 키 | (기본값) | your_secret_key |

### 선택적 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `JWT_REFRESH_TOKEN_EXPIRES_IN` | 리프레시 토큰 만료 시간 (초) | 86400 (24시간) |
| `JWT_ACCESS_TOKEN_EXPIRES_IN` | 액세스 토큰 만료 시간 (초) | 900 (15분) |
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 | dev |
| `FLYWAY_ENABLED` | Flyway 활성화 여부 | false |
| `LOGGING_LEVEL_USER_SERVICE` | 로그 레벨 | info |

### 개발 환경 전용 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DEV_SHOW_SQL` | SQL 쿼리 로그 출력 | true |
| `DEV_FORMAT_SQL` | SQL 포맷팅 | true |
| `DEV_HIBERNATE_DDL_AUTO` | Hibernate DDL 자동 생성 | update |

## 📡 API 엔드포인트

### 인증 관련 API (공개)

- `POST /auth/register` - 사용자 회원가입
- `POST /auth/login` - 사용자 로그인 (JWT 토큰 발급)
- `POST /auth/logout` - 사용자 로그아웃

### 사용자 관련 API (인증 필요)

- `GET /users/profile` - 사용자 프로필 조회
- `GET /users/me` - 현재 사용자 정보 조회
- `POST /users/change-password` - 비밀번호 변경

## 🔧 기술 스택

- **Framework**: Spring Boot 3.5.4
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Security**: Spring Security + JWT
- **Build Tool**: Gradle
- **Container**: Docker & Docker Compose

## 📁 프로젝트 구조

```
user-service/
├── src/main/java/com/deefacto/user_service/
│   ├── config/           # 설정 클래스
│   ├── controller/       # REST API 컨트롤러
│   ├── service/          # 비즈니스 로직
│   ├── domain/           # 도메인 모델
│   ├── secret/jwt/       # JWT 관련 클래스
│   └── common/           # 공통 클래스
├── src/main/resources/
│   ├── application.yml   # 기본 설정
│   ├── application-dev.yml   # 개발 환경 설정
│   ├── application-prod.yml  # 운영 환경 설정
│   └── db/migration/     # 데이터베이스 마이그레이션
├── docker-compose.yml    # 인프라 서비스 설정
├── env.example          # 환경 변수 예시
└── build.gradle         # 빌드 설정
```

## 🔒 보안 고려사항

1. **환경 변수 사용**: 민감한 정보는 환경 변수로 관리
2. **JWT 시크릿 키**: 운영 환경에서는 강력한 랜덤 키 사용
3. **데이터베이스 비밀번호**: 환경별로 다른 비밀번호 사용
4. **CORS 설정**: 허용된 도메인만 접근 가능하도록 설정

## 🐛 문제 해결

### 일반적인 문제들

1. **포트 충돌**: `SERVER_PORT` 환경 변수로 포트 변경
2. **데이터베이스 연결 실패**: Docker Compose 서비스 상태 확인
3. **Redis 연결 실패**: Redis 컨테이너 상태 확인

### 로그 확인

```bash
# 애플리케이션 로그
./gradlew bootRun

# Docker 컨테이너 로그
docker-compose logs mysql
docker-compose logs redis
```

## 📝 개발 가이드

### 새로운 기능 추가

1. 도메인 모델 정의 (`domain/` 패키지)
2. 서비스 로직 구현 (`service/` 패키지)
3. REST API 엔드포인트 추가 (`controller/` 패키지)
4. 테스트 코드 작성

### 환경별 설정

- **개발 환경**: `application-dev.yml` 사용
- **운영 환경**: `application-prod.yml` 사용
- **환경 변수**: `.env` 파일 또는 시스템 환경 변수 사용

## 🤝 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

