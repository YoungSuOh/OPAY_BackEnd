# OPay Backend

> **주문·결제·지갑·거래 원장**을 중심으로 한 이커머스 백엔드  
> 검색은 **Elasticsearch**, 운영 관측은 **Spring Actuator + Prometheus**를 고려한 구조

---

## 한 줄 요약

단순 CRUD를 넘어, **결제 재시도·네트워크 지연·중복 요청**이 실제로 발생하는 환경을 가정하고  
**멱등성(Idempotency)·상태 머신·원자적 잔액 차감·거래(원장) 기록**을 코드 레벨에서 다룬 프로젝트입니다.

---

## 왜 이 프로젝트인가?

### 문제 의식

| 영역 | 이슈 |
|------|------|
| 결제 | 동일 요청이 두 번 오면 **이중 결제** 위험 |
| 결제 | 타임아웃 후 재시도 시 **상태 불명(UNKNOWN)** 처리 필요 |
| 주문·결제 | 클라이언트 금액을 믿으면 **조작·불일치** 위험 |
| 검색 | DB `LIKE`·복합 조건 검색은 **지연·확장** 한계 |
| 동시성 | 동시 결제 시 **잔액 음수·경쟁 상태** 가능 |

### 접근 요약

- **결제**: `idempotency_key` + DB `UNIQUE`로 **의도 단위 중복 생성 방지**, 승인 단계에서 **키 검증·이미 성공 시 조기 반환**
- **금액**: 주문의 **총액·이미 결제된 금액** 대비 **잔여 금액 초과 불가**
- **원장**: `Transaction`을 **PENDING → SUCCESS/FAIL**로 두고, 지갑 차감과 함께 **감사 가능한 흐름** 설계
- **지갑**: **원자적 차감**(조건부 UPDATE 등)으로 **음수 잔액·동시성** 완화
- **검색**: **Elasticsearch**로 검색 부하·조건을 DB와 분리

---

## 핵심: 결제 시스템 설계

### 1) 2단계 결제 API (`request` → `approve`)

의도적으로 **“결제 생성”**과 **“승인(자금 이동)”**을 나눴습니다.

| 단계 | 역할 | 구현 포인트 |
|------|------|-------------|
| `POST /payments/request` | `Payment`를 **READY**로 생성 | 멱등 키로 중복 생성 방지 |
| `POST /payments/approve` | **PAYING → SUCCESS/FAIL**, 지갑 차감, 주문 반영 | 동일 `paymentId` 재호출 시 **이미 SUCCESS면 그대로 반환** |

프론트는 `paymentId` 발급 후 승인을 호출하고, **상태 폴링(`GET /payments/{id}/status`)**으로 최종 결과를 맞출 수 있는 형태입니다.

### 2) 멱등성(Idempotency) — 이중 결제 방지

**Payment**

- `idempotency_key` 컬럼에 **`UNIQUE` 제약**
- `requestPayment`: 동일 키로 이미 있으면 **새로 만들지 않고 기존 `Payment` 반환**
- `approvePayment`: 요청 키와 저장된 키가 다르면 **거부**

**Transaction**

- 결제 승인 시 `idempotencyKey + "_transaction"` 형태로 **별도 멱등 키**를 두어, 거래 레코드도 **중복 생성 방지**

### 3) 금액 정합성 — 서버가 최종 권한

- 금액이 **0 이하**면 거부
- `amount > order.getTotalAmount() - order.getPaidAmount()` 이면 거부 (**주문 잔액 초과 불가**)

### 4) 결제 상태 머신

`Payment` 상태: `READY` → `PAYING` → `SUCCESS` / `FAIL`, `CANCELED` 등

- 재시도는 **READY / FAIL**에서 가능하도록 `canRetry()` 등으로 명시
- `approvePayment` 시작 시 **이미 SUCCESS면 동일 객체 반환** → 멱등한 승인 응답

### 5) 승인 시 처리 순서 (트랜잭션 경계 내)

1. `Transaction` **PENDING** 생성 (멱등 키 별도)
2. `WalletService.deductBalance` — **원자적 차감**
3. 성공 시 `Transaction` **SUCCESS**, `Payment` **SUCCESS**, 주문 **`paidAmount` 누적**

### 6) 지갑·동시성

`WalletService`에서 **낙관적 동시성 / 버전 기반** 및 **조건부 UPDATE**로 동시 결제 시 **음수 잔액**을 방지하는 방향으로 구현되어 있습니다.

### 결제 범위에 대한 한 줄

- **내부 지갑(선불 충전형) 기반**의 승인 흐름에 가깝고, **외부 PG API 직접 연동·웹훅 검증**은 별도 레이어로 확장 가능하도록 설계했습니다.  
  **멱등·원장·동시성**은 실제 PG 연동 시에도 동일하게 요구되는 요소입니다.

---

## 검색 (Elasticsearch)

- 상품 검색은 **DB 부하·풀텍스트 한계**를 피하기 위해 **Elasticsearch**를 사용합니다.
- 앱 기동 시 **DB → ES 동기화** 같은 전략을 취할 수 있도록 구성했습니다.

---

## 아키텍처 개요

```
Client
  → Controller
      → Service (주문 / 결제 / 지갑 / 거래 / 검색)
          → Repository → MySQL
          → Elasticsearch (검색)
```

- **Layered Architecture**: Controller / Service / Repository
- **DTO + Validation**: `jakarta.validation`
- **도메인 패키지**: `order`, `payment`, `transaction`, `wallet`, `search` 등

---

## 기술 스택

| 구분 | 사용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Web / Security | Spring Web, Spring Security, JWT |
| Persistence | Spring Data JPA (MySQL, H2 for dev) |
| Search | Spring Data Elasticsearch |
| Cache / Infra | Redis (의존성·확장 포인트) |
| Storage | AWS S3 SDK |
| Observability | Actuator, Micrometer Prometheus |

---

## 설정 및 실행

### 설정 파일

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

**필수 점검**: 데이터소스, JWT, AWS S3 등 — **비밀 값은 환경 변수**로 분리하는 것을 권장합니다.

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export SPRING_PROFILES_ACTIVE=dev
```

### 애플리케이션 실행

**Windows**

```powershell
cd OPAY_BackEnd
.\gradlew.bat bootRun
```

**macOS / Linux**

```bash
cd OPAY_BackEnd
./gradlew bootRun
```

| 항목 | 값 |
|------|-----|
| 기본 포트 | `8080` |
| 컨텍스트 경로 | `/api` |
| Actuator | `/api/actuator` (예: Prometheus — `/api/actuator/prometheus`) |

### 모니터링 (선택)

프로젝트 루트 **`monitoring/`** 에서 Prometheus + Grafana를 실행할 수 있습니다. 자세한 내용은 [`../monitoring/README.md`](../monitoring/README.md)를 참고하세요.

### Elasticsearch

```text
http://localhost:9200
```

### 테스트

```bash
./gradlew test
```

---

## API 스냅샷 (결제)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/payments/request` | 결제 생성(READY), 멱등 |
| POST | `/api/payments/approve` | 승인, 지갑·거래·주문 반영 |
| GET | `/api/payments/{id}/status` | 상태 조회 (폴링) |
| GET | `/api/payments/{id}` | 단건 조회 |
| DELETE | `/api/payments/{id}` | 취소 (정책에 따름) |

---

## 개선 로드맵 (Roadmap)

- **Redis**: 조회 캐시, 분산 락(재고·쿠폰 확장 시)
- **메시지 큐**: 결제 후처리, 알림, 감사 로그 비동기화
- **PG 연동**: 외부 승인·웹훅·서명 검증 레이어
- **SAGA / 보상 트랜잭션**: 서비스 분리 시

---

## 배운 점

- 결제는 **“한 번만 성공”**이 아니라 **“여러 번 와도 한 번만 성공”**이어야 함 → **멱등성 설계**
- 금액·상태는 **도메인 규칙 + DB 제약**으로 이중 방어
- 지갑·원장은 **동시성·감사 추적**을 함께 고려해야 함
- 검색은 **읽기 트래픽**을 DB에서 분리하는 것이 확장의 기본

---

## Summary

> **Elasticsearch 검색**과 **결제 도메인(멱등성·상태·원장·동시성)**을 한 코드베이스에서  
> 실서비스에 가까운 관점으로 고민한 백엔드 프로젝트입니다.

---

## 보안

⚠️ `application.yml` 및 `application-*.yml`에 **비밀번호·API 키를 커밋하지 마세요.**

---

## Author

- GitHub: [@YoungSuOh](https://github.com/YoungSuOh)
