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
# 특히 JWT 시크릿 키는 반드시 변경해야 합니다!
```

### 2. JWT 시크릿 키 생성 (중요!)

```bash
# 개발 환경용 시크릿 키 생성 (Base64 인코딩)
echo -n "your-dev-secret-key-here-minimum-32-characters-long" | base64

# 운영 환경용 시크릿 키 생성 (Base64 인코딩)
echo -n "your-production-secret-key-here-minimum-32-characters-long" | base64

# 생성된 키를 .env 파일의 JWT_SECRET_KEY_DEV, JWT_SECRET_KEY_PROD에 설정
```

### 3. 인프라 서비스 실행

```bash
# Docker Compose로 MySQL, Redis 실행
docker-compose up -d

# 서비스 상태 확인
docker-compose ps
```

### 4. 애플리케이션 실행

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

### 5. 환경 변수 테스트

```bash
# KeyTest 클래스로 환경 변수가 제대로 로드되는지 확인
export $(cat .env | grep -v '^#' | xargs) && java -cp user-service/build/classes/java/main com.deefacto.user_service.dummy.KeyTest
```

## ⚙️ 환경 변수 설정

### 필수 환경 변수

| 변수명 | 설명 | 기본값 | 예시 |
|--------|------|--------|------|
| `SERVER_PORT` | 서버 포트 | 8081 | 8081 |
| `DB_HOST` | MySQL 호스트 | localhost | localhost |
| `DB_PORT` | MySQL 포트 | 3306 | 3306 |
| `DB_NAME` | 데이터베이스 이름 | deefacto_db | deefacto_db |
| `DB_USERNAME` | 데이터베이스 사용자명 | deefacto | deefacto |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | - | your_password |
| `REDIS_HOST` | Redis 호스트 | localhost | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 | 6379 |
| `JWT_SECRET_KEY_DEV` | 개발 환경 JWT 시크릿 키 (Base64)  | - | your_secret_key_dev |
| `JWT_SECRET_KEY_PROD` | 운영 환경 JWT 시크릿 키 (Base64)  | - | your_secret_key_prod |

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

## 🔧 IDE 설정

### VSCode 설정
- `.vscode/settings.json`: .env 파일 자동 로드 설정
- `.vscode/launch.json`: 디버깅 시 .env 파일 사용 설정

### IntelliJ IDEA 설정
- `.idea/workspace.xml`: 실행 설정에 .env 파일 연결

### Spring Boot 설정
- `application.yml`: .env 파일 자동 로드 설정 포함

## 📡 API 엔드포인트

### 인증 관련 API (공개)

- `POST /auth/register` - 사용자 회원가입
- `POST /auth/login` - 사용자 로그인 (JWT 토큰 발급)


### 사용자 관련 API (인증 필요)

- `POST /auth/logout` - 사용자 로그아웃
- `GET /user/info/search?` - 사용자 프로필 조회
- `POST /user/delete` - 회원 삭제
- `POST /user/info/password` - 비밀번호 변경

### 헬스체크 API

- `GET /actuator/health` - 애플리케이션 상태 확인

## 🌐 주요 URL

| 유형     | URL                              |
|----------|-----------------------------------|
| Swagger  | http://localhost:8081/swagger-ui.html |
| Actuator | http://localhost:8081/actuator/health |
| Grafana  | (운영 환경) 환경 변수 참조        |

## 🔧 기술 스택

- **Framework**: Spring Boot 3.5.4
- **Language**: Java 17 (Amazon Corretto)
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **Security**: Spring Security + JWT
- **Build Tool**: Gradle
- **Container**: Docker & Docker Compose
- **Environment**: .env 파일 지원 (IDE 자동 로드)
- **MSA**: 마이크로서비스 아키텍처
- **CI/CD**: Jenkins + Docker + ArgoCD (예정)

## 📁 프로젝트 구조

```
Backend-UserService/
├── user-service/                    # Spring Boot 애플리케이션
│   ├── src/main/java/com/deefacto/user_service/
│   │   ├── config/                 # 설정 클래스
│   │   ├── controller/             # REST API 컨트롤러
│   │   ├── service/                # 비즈니스 로직
│   │   ├── domain/                 # 도메인 모델
│   │   │   ├── Entitiy/           # JPA 엔티티
│   │   │   ├── Enum/              # 열거형
│   │   │   ├── dto/               # 데이터 전송 객체
│   │   │   └── repository/        # 데이터 접근 계층
│   │   ├── secret/jwt/            # JWT 관련 클래스
│   │   ├── common/                # 공통 클래스
│   │   ├── advice/                # 예외 처리 및 검증
│   │   └── dummy/                 # 테스트용 클래스
│   ├── src/main/resources/
│   │   ├── application.yml        # 기본 설정
│   │   ├── application-dev.yml    # 개발 환경 설정
│   │   ├── application-prod.yml   # 운영 환경 설정
│   │   └── db/migration/          # 데이터베이스 마이그레이션
│   └── build.gradle               # 빌드 설정
├── docker-compose.yml             # 인프라 서비스 설정
├── env.example                    # 환경 변수 예시
├── .env                          # 실제 환경 변수 (gitignore됨)
├── run-with-env.sh               # 환경 변수 로드 스크립트
├── .vscode/                      # VSCode 설정
├── .idea/                        # IntelliJ IDEA 설정
└── README.md                     # 프로젝트 문서
```

## 🔒 보안 고려사항

1. **환경 변수 사용**: 민감한 정보는 환경 변수로 관리
2. **JWT 시크릿 키**: 
   - 운영 환경에서는 강력한 랜덤 키 사용
   - Base64 인코딩 필수
   - 최소 32자 이상 권장
3. **데이터베이스 비밀번호**: 환경별로 다른 비밀번호 사용
4. **CORS 설정**: 허용된 도메인만 접근 가능하도록 설정
5. **.env 파일**: 절대 Git에 커밋하지 않음
6. **인증서 파일**: `src/main/resources/certs/` 등 민감 파일은 **git에 커밋 금지**
7. **환경 변수/비밀키**: 운영 서버 또는 CI/CD에서 안전하게 주입

## 🐛 문제 해결

### 일반적인 문제들

1. **환경 변수가 null로 나오는 경우**:
   ```bash
   # .env 파일이 제대로 로드되는지 확인
   source .env && echo $JWT_SECRET_KEY_DEV
   
   # 또는 스크립트 사용
   ./run-with-env.sh
   ```

2. **포트 충돌**: `SERVER_PORT` 환경 변수로 포트 변경
3. **데이터베이스 연결 실패**: Docker Compose 서비스 상태 확인
4. **Redis 연결 실패**: Redis 컨테이너 상태 확인

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

### 📁 폴더 구조 및 설정 규칙

- 모든 설정은 `application.yml` 파일 사용 (`.properties ❌ 금지`)
- 새로운 설정 클래스는 `XXConfig.java` 네이밍 사용  
  예: `MqttConfig.java`, `KafkaConfig.java`

### ⚙️ 환경 변수 및 민감 정보 관리

- 모든 민감 정보는 환경 변수 또는 `.env` 파일로 관리
- 주요 환경 변수 예시:
  - `AWS_IAM_ACCESS_KEY`, `AWS_IAM_SECRET_KEY`
  - `GRAFANA_URL_OUTER`
  - `spring.datasource.*`, `spring.kafka.*` 등



## 🧪 테스트

- 테스트 코드는 `src/test/java` 디렉터리에 작성
- JUnit 5 기반 유닛/통합 테스트 구성

## 🧑‍💻 커밋 메시지 컨벤션 (`|` 구분자 사용)

```bash
[type] | sprint | JIRA-KEY | 기능 요약 | 담당자
```

- **type**: feat, fix, docs, config, refactor, test, chore, style 등
- **sprint**: sprint0, sprint1, ...
- **JIRA-KEY**: JIRA 이슈 번호 또는 없음
- **기능 요약**: 핵심 변경 내용
- **담당자**: 실명 또는 닉네임

### 📌 예시

```
feat    | sprint0 | 없음     | 센서 등록 API 구현         | KIM
feat    | sprint0 | IOT-123  | 센서 등록 API 구현         | KIM
fix     | sprint1 | IOT-210  | MQTT 수신 실패 예외 처리   | RAFA
config  | sprint0 | IOT-001  | H2 DB 설정 추가            | MO
docs    | sprint1 | IOT-999  | README 초안 작성           | JONE
```

### ✅ 추천 커밋 예시 (복붙용)

```bash
git commit -m "feat    | sprint1 | IOT-112 | 작업자 센서 조회 API 추가 | KIM"
git commit -m "fix     | sprint0 | IOT-009 | H2 연결 오류 수정         | RAFA"
git commit -m "config  | sprint0 | IOT-000 | Spring Boot 3.4.4 적용    | MO"
git commit -m "chore   | sprint1 | IOT-999 | 커밋 컨벤션 README 정리   | JONE"
```

## 🚧 기타 운영 참고

- Jenkins 및 ArgoCD 연동은 `Jenkinsfile` 참조
- 신규 설정 파일 추가 시 반드시 `XXConfig.java` 네이밍 유지

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

