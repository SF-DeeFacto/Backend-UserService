#!/bin/bash

# ========================================
# .env 파일을 로드하고 Java 애플리케이션 실행 스크립트
# ========================================

# 스크립트가 있는 디렉토리로 이동
cd "$(dirname "$0")"

# .env 파일이 존재하는지 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일을 찾을 수 없습니다."
    echo "env.example 파일을 .env로 복사하고 값을 수정해주세요."
    exit 1
fi

# .env 파일에서 환경 변수 로드 (주석 제외)
echo "📋 .env 파일에서 환경 변수를 로드합니다..."
export $(cat .env | grep -v '^#' | xargs)

# 환경 변수 확인
echo "✅ 환경 변수 로드 완료:"
echo "   JWT_SECRET_KEY_DEV: ${JWT_SECRET_KEY_DEV:0:20}..."
echo "   JWT_SECRET_KEY_PROD: ${JWT_SECRET_KEY_PROD:0:20}..."

# Java 애플리케이션 실행
echo "🚀 Java 애플리케이션을 실행합니다..."
cd user-service
./gradlew bootRun 