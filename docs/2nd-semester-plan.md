# 2학기 Spring 서버 작업 계획

> 기준 아키텍처: 2026-08 팀 확정안 (5차 수정본)
> 담당: 남정현 (BackEnd — Spring)
> 최종 갱신: 2026-08-27

---

## 0. 한 줄 요약

Spring은 **인증 · 알림 · 챗봇**만 담당하고, 상품·리뷰·분석은 전부 Data 서버로 넘긴다.
이번 학기 Spring 작업은 **도메인 덜어내기**와 **LLM 챗봇 신규 구현** 두 가지다.

---

## 1. 확정 아키텍처

```
                    외부 LLM API                       외부 쇼핑몰 API
                         │                                   │
                    챗봇 대화                                 │
                         ↓                                   ↓
  Flutter  ⇄  Spring ────── ① 요청·조회·진행률 ──────→  Data  ──② 리뷰 배치─→  AI
             인증·알림·챗봇  ←──────── ④ 완료 웹훅 ────────  수집·오케스트라 ←──③ 결과──  순수 분석 함수
                  │                                          │
                  ↓                                          ↓
             users · chats                          PostgreSQL
                                                 reviews · results · jobs
```

### 서버별 책임

| 서버 | 책임 | 소유 데이터 |
|------|------|-----------|
| **Spring** | 인증·인가, 알림, LLM 챗봇, 사용자 도메인 | `users`, `chats` |
| **Data** | 수집, 오케스트레이션, 조회 API, 완료 웹훅 발신 | `reviews`, `results`, `jobs` |
| **AI** | 리뷰 배치를 받아 분석 결과를 반환 (상태 없음) | 없음 |

### 흐름

1. Flutter가 Spring에 분석을 요청 → Spring이 Data에 전달
2. Data가 리뷰를 수집해 AI에 배치 단위로 전달
3. AI가 분석 결과를 반환, Data가 PostgreSQL에 저장
4. Data가 Spring에 완료 웹훅 → Spring이 알림 발송

**Spring은 AI 서버의 존재를 모른다.** 나가는 호출은 Data 하나, 들어오는 것은 웹훅 하나뿐이다.

---

## 2. 이전 계획 대비 달라진 점

이 문서의 이전 판은 계획서 2.5(프론트가 3개 서버를 각각 호출, FastAPI가 JWT 검증)를 기준으로 작성했다.
확정 아키텍처는 **Spring 단일 관문** 방식이므로 아래 항목이 바뀐다.

| 항목 | 이전 계획 | 확정안 |
|------|----------|--------|
| 프론트 통신 대상 | 3개 서버 각각 | **Spring 하나** |
| JWT 검증 주체 | FastAPI가 사용자 JWT 검증 | **Spring만 검증** |
| RS256 전환 · JWKS | 최우선 과제 | **불필요** |
| 서버 간 인증 | 사용자 JWT 공유 | **서비스 토큰** |
| 상품·리뷰 소유 | FastAPI | Data |
| 분석 호출 | Spring이 AI 직접 호출 | Spring → Data → AI |

> **RS256 전환과 JWKS 엔드포인트 작업은 취소한다.**
> 사용자 JWT가 Spring 밖으로 나가지 않으므로 대칭키(HS256)를 유지해도 문제가 없다.
> 대신 Spring ↔ Data 구간을 서비스 토큰으로 보호한다.

---

## 3. 현재 Spring 도메인 현황

```
src/main/java/com/example/fireview/domain/
├── admin         ├── ai          ├── auth        ├── cart
├── dashboard     ├── feedback    ├── landing     ├── notification
├── onboarding    ├── product     ├── report      ├── review
├── search        ├── user        └── wishlist
```

컨트롤러 17개, 엔티티 26개.

---

## 4. 도메인 분류

### 4-1. 제거 — Data 서버로 이관

| 도메인 | 현재 Spring이 가진 것 |
|--------|---------------------|
| `product` | Product 엔티티, ProductController, 네이버 상품 Redis 캐시 |
| `review` | Review 엔티티, ReviewController |
| `search` | NaverSearchController, 네이버 쇼핑 검색 연동 |
| `ai` | AiAnalysisController, AiServerClient, RTI 반영 로직 |
| `dashboard` | SearchKeyword, ViewHistory (상품 기반 통계) |

제거 대상 엔티티: `Product`, `Review`, `PlatformLink`, `Category`, `MajorCategory`,
`TrustGrade`, `SearchKeyword`, `ViewHistory`

`ai` 도메인이 가장 무겁다. 지금은 Spring이 AI 서버를 호출해 결과를 자기 DB에 저장하는 구조인데
(`naverProductId` 매핑, 리뷰 동기화, trustSignals 계산 — PR #67·#79·#83·#85),
확정안에서는 이 경로 전체가 사라진다. Spring은 Data에 조회만 한다.

### 4-2. 잔류 — Spring 담당

| 도메인 | 내용 |
|--------|------|
| `auth` | 회원가입, 로그인, OAuth2(구글·네이버), JWT 발급, 비밀번호 재설정 |
| `user` | 프로필, 사용자 설정(14개 필드), 이용 통계 |
| `onboarding` | 관심 카테고리 |
| `wishlist` | 찜 — 상품 참조 방식 변경 필요 (5-4 참고) |
| `notification` | 알림 — 완료 웹훅의 수신처 |
| `landing` | 랜딩 |
| **`chat`** | **신규 — LLM 챗봇 (5-3 참고)** |

### 4-3. 미결 — 팀 합의 필요

| 도메인 | 쟁점 |
|--------|------|
| `report` | 신고. 확정 아키텍처 그림에 명시 없음 |
| `feedback` | 분석 피드백. 확정 아키텍처 그림에 명시 없음 |
| `admin` | 의심 리뷰·모델 성능(Data) vs 회원 관리(Spring) — 분할 필요 |
| `cart` | 계획서·아키텍처 어디에도 언급 없음. 유지/폐기 결정 |

**권고: `report`·`feedback`은 Spring에 남긴다.**
둘 다 사용자 계정에 묶인 데이터이고 이미 구현이 끝났다(PR #103·#104·#109·#127).
다만 대상 리뷰는 Data 소유이므로 `reviewId` 참조만 저장하고 본문은 Data에서 조회한다.

---

## 5. 신규 작업

### 5-1. 서비스 토큰 + 완료 웹훅 수신 — ✅ 수신 측 구현 완료

**서비스 토큰** — Spring → Data 호출과 Data → Spring 웹훅 양방향에 적용한다.
사용자 JWT와는 별개의 고정 토큰이며 `.env.prod`의 `SERVICE_TOKEN`으로 주입한다.

**웹훅 수신 엔드포인트** — `POST /api/internal/webhooks/analysis-complete`
규격은 `docs/webhook-contract.md` 참고. 구현된 것:

- `ServiceTokenFilter` — `/api/internal/**` 에 `X-Service-Token` 검증, 미설정 시 전부 거부(fail-closed)
- `ProcessedWebhookStore` / `RedisProcessedWebhookStore` — `jobId` 기준 멱등성, 24시간 TTL
- `WebhookService` — 완료/실패에 따라 `ANALYSIS_COMPLETE` / `ANALYSIS_FAILED` 알림 발송
- 처리 중 예외 시 표시를 되돌려 Data 서버 재전송이 다시 처리되게 함

남은 것: Spring → Data 방향 서비스 토큰 적용 (5-2 조회 프록시와 함께)

웹훅은 중복 전송과 유실이 모두 정상 동작 범위다. 한 번만 온다고 가정하면 안 된다.

### 5-2. Data 조회 프록시

Spring이 Data의 조회 API를 감싸 프론트에 제공한다. 응답에 **진행률**이 포함되어야 한다.

- 사용자별 개인화(찜 여부 등)를 합쳐야 하는 응답은 Spring에서 조합
- 단순 통과만 하는 응답은 얇게 유지 (불필요한 가공 금지)
- Data 장애 시 폴백 정책 필요 — 빈 값인지 에러인지 명시

### 5-3. LLM 챗봇 (10주차)

계획서 일정표 10주차 결과물: `Spring LLM Chat Bot Service`
현재 코드에 관련 의존성이 전혀 없다. **이번 학기 Spring의 유일한 신규 기능이자 가장 큰 작업이다.**

#### 스레드 모델 — 설계를 미리 정할 것

Spring 안에서 부하 성격이 둘로 갈린다.

```
인증·조회 프록시  →  빠르고 가볍다 (수십 ms)
챗봇 LLM 호출    →  느리고 무겁다 (수 초 ~ 수십 초)
```

현재 `spring-boot-starter-web`(블로킹 MVC)이므로 LLM 응답을 기다리는 동안
Tomcat 워커 스레드가 계속 점유된다. 챗봇 사용자 수십 명이면
**로그인·찜 같은 무관한 API까지 대기열에 밀린다.**

| 단계 | 조치 |
|------|------|
| 최소 | 챗봇 전용 스레드 풀 분리 (일반 API 풀과 격리) |
| 권장 | + 타임아웃 + 서킷 브레이커 (LLM 장애 격리) |
| 필요 시 | 챗봇 경로만 WebFlux 분리 |

스레드 모델은 나중에 바꾸기 가장 어려운 축이므로 구현 전에 정해둔다.

#### 프롬프트 조립 책임

계획서 2.7의 챗봇 기능("RTI가 낮아진 이유를 자연어로 제공", "상품별 주의 요소 요약")을
수행하려면 리뷰와 분석 결과가 프롬프트에 들어가야 한다. 그 데이터는 Data에 있다.

```
Flutter → Spring → Data (리뷰·결과 조회)
                 → 프롬프트 조립       ← Spring 책임
                 → 외부 LLM API
```

정해야 할 것:

- 리뷰를 몇 건까지 프롬프트에 넣을 것인가
- 토큰 예산 초과 시 자르는 기준
- 대화 이력이 길어질 때의 요약 전략
- 동일 질의 캐싱 (계획서 2.7: "같은 분석 요청은 캐시하여 API 비용을 절감")

리뷰 1000건을 통째로 넣으면 대화 한 번에 상당한 요금이 나간다. 비용이 직결되는 지점이다.

#### 기타

- `chats` 테이블 설계 (세션 단위, 사용자별)
- 스트리밍 응답 여부 결정 — SSE를 쓰면 위 스레드 문제가 커진다
- LLM API 키는 `.env.prod` 환경변수로 관리 (계획서 2.7)
- 실패 시 폴백 — 챗봇이 죽어도 나머지 기능은 정상 동작해야 한다

### 5-4. 찜·신고의 원격 참조

상품·리뷰가 Data 소유가 되면 `Wishlist`, `Report`가 엔티티를 FK로 물 수 없다.

- Spring은 외부 ID(`productId`, `reviewId`)만 저장
- 표시용 상품 정보 조합 주체 결정 필요
  - (A) 프론트가 Spring에서 ID를 받고 Data에서 상세 조회
  - (B) Spring이 Data를 호출해 합쳐서 응답
- Data에서 원본이 사라졌을 때의 정리 규칙 필요 (FK로 막을 수 없음)

확정 아키텍처가 Spring 단일 관문이므로 **(B)가 일관성 있다.**

---

## 6. 시급도순 작업 목록

| 순위 | 작업 | 시기 |
|------|------|------|
| 1 | Spring/Data API 명세 확정 (조회·진행률·웹훅 규격) | 3~4주차 |
| 2 | 서비스 토큰 규격 합의 및 적용 | 3~4주차 |
| 3 | **테스트 환경 복구** (7절) | 4주차 이전 |
| 4 | 완료 웹훅 수신 + 알림 연동 | 5~6주차 |
| 5 | `product`·`review`·`search`·`ai`·`dashboard` 도메인 제거 | 5~8주차 |
| 6 | Data 조회 프록시 구현, 찜·신고 참조 방식 변경 | 5~8주차 |
| 7 | **LLM 챗봇 — 스레드 모델 설계** | 9주차까지 |
| 8 | **LLM 챗봇 구현** | 10주차 |
| 9 | 기능 테스트 / 통합 테스트 | 12주차 |

---

## 7. 선결 과제 — 테스트 환경 복구 — ✅ 완료

`src/test/resources/application.properties`에 더미 시크릿이 이미 있어 컨텍스트 로드가 통과했고,
CI의 `-x test`를 제거해 PR마다 테스트가 돌도록 했다.

현재 테스트: 컨텍스트 로드 1건 + 웹훅 관련 14건(필터 단위, 서비스 단위, MockMvc 배선).
6절 5번(도메인 5개 제거) 전에 제거 대상 도메인의 주요 경로에 테스트를 보강하는 게 좋다.

---

## 8. 미결 안건 (팀 합의 필요)

1. `report`·`feedback`의 소속 — Spring 잔류 권고
2. `cart`(장바구니) 유지 여부 — 계획서·아키텍처 모두 언급 없음
3. `admin` 분할 기준 — 의심 리뷰/모델 성능(Data) vs 회원 관리(Spring)
4. 찜 목록의 상품 정보 조합 주체 — (B) Spring 조합 권고
5. `users`/`chats`와 Data의 PostgreSQL을 같은 RDS 인스턴스에 둘지
   → 학기 프로젝트 규모에서는 **한 인스턴스 안에서 스키마만 분리**하는 것을 권고
6. 서비스 토큰 회전 주기 및 관리 주체

---

## 9. 타 팀 전달 사항

**Data 담당자에게**

Data가 수집·오케스트레이션·DB 소유·조회 API·웹훅 발신을 모두 맡게 되어 부하 성격이 섞였다.
크롤링은 느리고 외부 차단으로 자주 실패하는 작업이고, 조회 API는 빠른 응답이 필요하다.
한 프로세스에 두면 크롤링이 밀릴 때 조회까지 느려진다.
프로세스 분리가 정석이나 학기 규모에는 과하므로, **최소한 크롤링과 조회의 스레드 풀 분리**를 권한다.

**AI 담당자에게**

AI를 순수 분석 함수로 정리한 것은 좋은 결정이다. 상태가 없으므로 재시도가 안전하고
실패해도 부분 결과만 버리면 된다. 이 성질을 유지하려면 AI가 DB에 직접 쓰지 않아야 한다.
