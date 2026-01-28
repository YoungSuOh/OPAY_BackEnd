# OPAY Backend

Spring Boot 기반의 결제 시스템 백엔드 애플리케이션입니다.

## 설정 파일

### 환경 변수 설정

프로젝트를 실행하기 전에 설정 파일을 복사하고 실제 값으로 수정해야 합니다:

```bash
# 프로덕션 설정
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 개발 설정
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

### 필수 설정 항목

1. **데이터베이스 설정** (`application.yml`)
   - MySQL 연결 정보 (URL, username, password)

2. **JWT 설정** (`application.yml`)
   - JWT 시크릿 키 (프로덕션에서는 반드시 변경 필요)

3. **AWS S3 설정** (`application.yml`)
   - AWS 액세스 키 및 시크릿 키
   - S3 버킷 이름
   - 환경 변수 사용 권장: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`

### 환경 변수 사용 (권장)

민감한 정보는 환경 변수로 설정하는 것을 권장합니다:

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export SPRING_PROFILES_ACTIVE=dev
```

## 실행 방법

```bash
./gradlew bootRun
```

## 보안 주의사항

⚠️ **중요**: `application.yml` 및 `application-*.yml` 파일은 `.gitignore`에 포함되어 있습니다.
절대 실제 비밀번호나 키를 커밋하지 마세요!
