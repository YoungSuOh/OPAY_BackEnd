# OPAY Backend

OPAY 프로젝트의 Spring Boot 기반 백엔드 애플리케이션입니다.

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - 인증 및 권한 관리
- **Spring Data JPA** - 데이터베이스 접근
- **JWT (JSON Web Token)** - 토큰 기반 인증
- **MySQL** - 프로덕션 데이터베이스
- **H2** - 개발용 인메모리 데이터베이스
- **Gradle** - 빌드 도구

## 프로젝트 구조

```
src/main/java/com/opay/
├── OpayBackendApplication.java    # 메인 애플리케이션
├── config/
│   └── JpaConfig.java              # JPA 설정
├── domain/
│   └── user/
│       ├── entity/
│       │   └── User.java           # User 엔티티
│       ├── repository/
│       │   └── UserRepository.java # User 리포지토리
│       ├── dto/
│       │   ├── SignupRequest.java  # 회원가입 요청 DTO
│       │   ├── LoginRequest.java   # 로그인 요청 DTO
│       │   └── AuthResponse.java  # 인증 응답 DTO
│       ├── service/
│       │   └── UserService.java    # User 서비스
│       └── controller/
│           └── AuthController.java # 인증 컨트롤러
└── security/
    ├── SecurityConfig.java         # Spring Security 설정
    └── jwt/
        ├── JwtTokenProvider.java   # JWT 토큰 생성/검증
        └── JwtAuthenticationFilter.java # JWT 인증 필터
```

## 인증 시스템

### JWT 토큰 기반 인증

- **Access Token**: HTTP 헤더의 `Authorization: Bearer {token}` 형식으로 전달
- **Refresh Token**: HttpOnly 쿠키에 저장 (7일 유효)
- **토큰 갱신**: `/api/auth/refresh` 엔드포인트를 통해 Access Token 갱신

### API 엔드포인트

#### 회원가입
```
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-1234-5678" (선택)
}
```

#### 로그인
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

#### 토큰 갱신
```
POST /api/auth/refresh
Cookie: refreshToken={refresh_token}
```

#### 로그아웃
```
POST /api/auth/logout
Cookie: refreshToken={refresh_token}
```

## 설정

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/opay
    username: root
    password: root

jwt:
  secret: your-secret-key
  access-token-expiration: 3600000  # 1시간
  refresh-token-expiration: 604800000  # 7일
```

### 개발 환경 (application-dev.yml)

H2 인메모리 데이터베이스 사용:
- H2 Console: http://localhost:8080/api/h2-console

## 실행 방법

1. MySQL 데이터베이스 생성 (프로덕션)
   ```sql
   CREATE DATABASE opay;
   ```

2. Gradle 빌드
   ```bash
   ./gradlew build
   ```

3. 애플리케이션 실행
   ```bash
   ./gradlew bootRun
   ```

4. 개발 환경 실행 (H2 사용)
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

## CORS 설정

현재 허용된 Origin:
- http://localhost:5173 (Vite 개발 서버)
- http://localhost:3000 (기타 개발 서버)

## 보안 고려사항

1. **프로덕션 환경**:
   - `jwt.secret`을 강력한 랜덤 문자열로 변경
   - `spring.datasource.password`를 안전하게 관리
   - HTTPS 사용 시 `refreshTokenCookie.setSecure(true)` 설정

2. **비밀번호**:
   - BCrypt로 해싱하여 저장
   - 최소 8자 이상 권장

3. **토큰**:
   - Access Token은 짧은 만료 시간 (1시간)
   - Refresh Token은 HttpOnly 쿠키로 저장하여 XSS 공격 방지

## 다음 단계

- [ ] 상품(Product) 도메인 구현
- [ ] 장바구니(Cart) 도메인 구현
- [ ] 주문(Order) 도메인 구현
- [ ] 결제(Payment) 도메인 구현
- [ ] 리뷰(Review) 도메인 구현
- [ ] 최근 본 상품(RecentProduct) 도메인 구현
