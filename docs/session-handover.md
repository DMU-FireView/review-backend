# 세션 인수인계 자료
> 최종 업데이트: 2026-06-02 | 프로젝트: FireView (review-backend)

---

## 🖥️ 인프라 정보

| 항목 | 값 |
|------|-----|
| EC2 서버 | `ec2-user@ip-172-31-44-47` |
| 백엔드 도메인 | `https://api.beens.kr` |
| 프론트엔드 도메인 | `https://beens.kr` |
| EC2 공인 IP | `3.39.78.175` |
| AI 서버 (Azure) | `http://20.249.211.171:8000` |
| DB | PostgreSQL RDS `fireview-db-1.c18oucqqk15z.ap-northeast-2.rds.amazonaws.com:5432/postgres` |
| DB 계정 | `fireview1` / `FireviewProd2026!` |
| Docker 컨테이너 | `fireview` |
| nginx | EC2에서 실행 중 (포트 443 → 8080 프록시) |
| Redis | EC2 호스트에서 실행 중 (`redis6`, 포트 6379) |

### EC2 서버 관리 명령어
```bash
# 로그 확인
docker logs --tail 50 fireview
docker logs --tail 50 fireview 2>&1 | grep -E "ERROR|Exception"

# 배포 완료 확인 (Started FireViewApplication 뜨면 완료)
docker logs --tail 5 fireview

# Redis 상태 확인
redis6-cli ping
redis6-cli keys "naver:product:*"

# DB 접속
PGPASSWORD='FireviewProd2026!' psql -h fireview-db-1.c18oucqqk15z.ap-northeast-2.rds.amazonaws.com -U fireview1 -d postgres
```

---

## 🔄 배포 플로우

```
브랜치 생성 → 작업 → PR → main 머지 → GitHub Actions 자동 배포
```
- **main 직접 커밋 금지**
- GitHub Actions CI/CD로 머지 즉시 자동 배포됨 (`docker-compose` 명령어 불필요)

---

## ✅ 이전 세션 완료 PR 목록

| PR | 내용 |
|----|------|
| #57 | 검색 결과 부족 개선 (멀티페이지 + DB 병합) |
| #58 | `/api/products?keyword` DB만 조회 문제 수정 |
| #59~#62 | 검색 500 오류 (LazyInit → JOIN FETCH 근본 해결) |
| #63~#64 | 비로그인 401 전역 포맷 통일 |
| #65 | AI 서버 타임아웃 + 병렬 호출 (CompletableFuture) |
| #66 | 네이버 상품에 productUrl 추가 |
| #67 | naverProductId 매핑으로 AI 분석 결과 DB 업데이트 활성화 |
| #68 | 대분류/중분류/소분류 3계층 카테고리 구조 도입 |
| #70 | NaverProductCache 인메모리 → Redis 전환 (TTL 24시간) |
| #71 | Docker 컨테이너 Redis 호스트 접근 문제 수정 (localhost → EC2 내부 IP) |
| #72 | Redis 역직렬화 오류 수정 (String 기반 JSON 직렬화로 교체) |

---

## ✅ 이번 세션(2026-06-02)에서 완료한 PR 목록

| PR | 브랜치 | 내용 |
|----|--------|------|
| #83 | `fix/review-id-collision` | AI review_id를 DB PK로 쓰던 충돌 버그 수정 |
| #84 | `feat/review-ratio-stats` | 실사용/광고성/반복표현 비율 통계 필드 추가 |
| #85 | `feat/trust-signals` | 주요 판단 신호(trustSignals) 계산 및 응답 추가 + 빌드 오류 수정 |
| #86 | `fix/data-initializer-null-product-id` | 네이버 API productId null 시 fallback ID로 서버 기동 오류 수정 |

---

## 🐛 이번 세션에서 해결한 버그

### 1. 리뷰 저장 안 되는 버그 (PR #83)
- **원인**: AI 서버가 반환하는 `review_id`("1", "3")를 DB PK로 직접 사용 → 다른 상품 리뷰와 ID 충돌 → `existsById` true 반환 → 저장 스킵
- **수정**: `Review.id`에 `@GeneratedValue(IDENTITY)` 추가, 중복 체크를 `(product_id, reviewerId, writtenAt)` 조합으로 변경
- **DB 초기화 필요**: `TRUNCATE TABLE review_reasons, reviews RESTART IDENTITY CASCADE`

### 2. 비율 통계 0% 표시 (PR #84)
- **원인**: AI 서버 MVP에서 percentage 필드 제외 → 백엔드에서 계산 안 함
- **수정**: `product-detail` 응답의 reason code 집계로 직접 계산
  - `realReviewRatio`: safe_count / total_reviews
  - `adSuspicionRatio`: AD 관련 코드(PURCHASE_NOT_VERIFIED 등) 보유 리뷰 비율
  - `repetitiveRatio`: REPETITIVE_KEYWORD 보유 리뷰 비율

### 3. 주요 판단 신호 미표시 (PR #85)
- **원인**: AI 서버 MVP에서 제외된 데이터 → 프론트 하드코딩 빈 배열
- **수정**: 백엔드에서 `TrustSignalDto` 생성 후 집계 계산
  - 구매인증 리뷰 비율 / 텍스트 다양성 / 반복 표현 비율 / 작성 시점 패턴

### 4. 서버 기동 실패 (PR #86)
- **원인**: `DataInitializer`에서 네이버 API productId null 시 `Product.id=null` persist 시도
- **수정**: fallback ID(900_000_000_000~) 사용, `@Profile("!test")` 추가

---

## 📊 현재 DB 상태

```
products: 33개 (naverProductId 정상 저장, id = naverProductId)
reviews: 1117개 (DataInitializer 생성 더미 데이터)
users: DataInitializer 생성 (admin@fireview.com, user@fireview.com)
```

### DB 전체 초기화가 필요할 때 순서
```bash
# 1. products 관련
TRUNCATE TABLE review_reasons, review_feedbacks, reviews, product_platform_links, products CASCADE;
# 2. users
TRUNCATE TABLE users CASCADE;
# 3. 기타
TRUNCATE TABLE search_keywords CASCADE;
# 4. 서버 재시작 → DataInitializer 자동 실행
docker restart fireview
```

---

## 🤖 AI 서버 연동 현황

### 연결 상태: ✅ 정상

### AI 서버 엔드포인트 (모두 POST, TriggerRequest 공통 사용)
| 엔드포인트 | 설명 | 응답 DTO |
|-----------|------|---------|
| `/api/internal/ai/products/product-list` | 상품 RTI 요약 | `AiProductListResponse` |
| `/api/internal/ai/reviews/product-detail` | 개별 리뷰 분석 (reason code 포함) | `AiProductDetailResponse` |
| `/api/internal/ai/products/rti-trend` | 30일 추이 | `AiRtiTrendResponse` |
| `/api/internal/ai/reviews/report` | 리뷰 상세 리포트 | `AiReviewReportResponse` |
| `/api/internal/ai/products/risk-report` | 상품 위험도 리포트 + sample_reviews | `AiProductRiskReportResponse` |

### ⚠️ 중요: AI 서버 데이터 있는 상품
- `53530143052` → AI 서버에 데이터 있음 (테스트용)
- 대부분의 상품은 AI 서버에 데이터 없음 → `product-detail: []` 반환 → 비율/신호 계산 불가

### 페이지 로딩 흐름
1. 페이지 진입 → DB 기존 데이터로 먼저 표시 (비율 0%)
2. 백그라운드에서 AI 분석 트리거 (수초 소요)
3. 분석 완료 → 실제 값으로 업데이트 (정상 동작)

---

## 📡 백엔드 API 응답 구조

### `POST /api/analysis/product`
```json
{
  "productId": "53530143052",
  "averageRti": 78.33,
  "level": "warn",
  "reviewCount": 3,
  "safeCount": 1,
  "warnCount": 2,
  "dangerCount": 0,
  "reviews": [ { "reviewId", "content", "author", "date", "rti", "level", "textScore", "behaviorScore", "networkScore", "reasons" } ],
  "trend": [ ... ],
  "realReviewRatio": 33.3,
  "adSuspicionRatio": 66.7,
  "repetitiveRatio": 33.3,
  "trustSignals": [
    { "label": "구매인증 리뷰 비율", "value": "보통", "isPositive": false },
    { "label": "텍스트 다양성",     "value": "보통", "isPositive": false },
    { "label": "반복 표현 비율",    "value": "보통", "isPositive": false },
    { "label": "작성 시점 패턴",    "value": "자연스러움", "isPositive": true }
  ]
}
```

### `GET /api/products/{productId}/reviews`
```json
{
  "id": 4471,
  "productId": 53530143052,
  "reviewerNickname": "reviewer_0099",
  "content": "...",
  "rating": 5,
  "trustGrade": "SUSPICIOUS",
  "trustGradeLabel": "의심",
  "trustGradeColor": "#EAB308",
  "reasons": [ "반복 표현 탐지: '대박' 2회", ... ],
  "writtenAt": "2026-05-25T00:00:00",
  "isVerifiedPurchase": false,
  "reviewerAtiScore": 55.0
}
```

---

## 📦 카테고리 구조

3계층으로 구성됨.

```
대분류 (MajorCategory enum, 14개): 디지털/가전, 패션의류, 패션잡화, 뷰티, 식품 ...
  └── 중분류 (Category enum, ~50개): DIGITAL_MOBILE, DIGITAL_PC, ACC_SHOES ...
        └── 소분류 (subCategory String): Naver category3 값 그대로 저장
```

### DB 마이그레이션 주의사항
- `products`, `user_preferred_categories` 두 테이블 모두 category 컬럼 보유
- enum 값 변경 시 두 테이블 모두 마이그레이션 필요
- 마이그레이션 전 constraint 제거 필요:
```sql
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_check;
ALTER TABLE user_preferred_categories DROP CONSTRAINT IF EXISTS user_preferred_categories_category_check;
```

---

## 💾 Redis 캐시 구조

| 항목 | 값 |
|------|-----|
| 호스트 | `172.31.44.47` (EC2 내부 IP, Docker 컨테이너에서 접근) |
| 포트 | `6379` |
| 키 패턴 | `naver:product:{naverProductId}` |
| TTL | 24시간 |
| 직렬화 | JSON String (ObjectMapper 직접 사용, GenericJackson2JsonRedisSerializer 사용 불가) |

```bash
# 캐시 전체 삭제 (역직렬화 오류 발생 시)
redis6-cli keys "naver:product:*" | xargs redis6-cli del
```

---

## 📁 주요 파일 위치

| 파일 | 역할 |
|------|------|
| `domain/ai/client/AiServerClient.java` | AI 서버 HTTP 클라이언트 (5개 엔드포인트) |
| `domain/ai/service/AiAnalysisService.java` | AI 분석 병렬 호출 + DB 동기화 |
| `domain/ai/dto/response/ProductAnalysisResponse.java` | 분석 결과 통합 DTO (비율/신호 계산 포함) |
| `domain/ai/dto/response/TrustSignalDto.java` | 주요 판단 신호 DTO (판정 기준 포함) |
| `domain/ai/dto/response/SampleReview.java` | AI risk-report sample_reviews 역직렬화 |
| `domain/review/entity/Review.java` | @GeneratedValue(IDENTITY) 적용 |
| `domain/review/repository/ReviewRepository.java` | existsByProduct_IdAndReviewerIdAndWrittenAt 추가 |
| `domain/product/entity/Product.java` | id = naverProductId (수동 할당, @GeneratedValue 없음) |
| `global/config/DataInitializer.java` | 더미 데이터 초기화 (@Profile("!test"), fallback ID 적용) |
| `global/config/RedisConfig.java` | Redis String 직렬화 설정 |
| `global/config/RestTemplateConfig.java` | AI 전용 RestTemplate (30초 타임아웃) |
| `global/security/CustomAuthenticationEntryPoint.java` | 전역 401 포맷 처리 |
| `domain/product/cache/NaverProductCache.java` | 네이버 상품 Redis 캐시 (TTL 24h) |
| `domain/product/entity/MajorCategory.java` | 대분류 enum (14개) |
| `domain/product/entity/Category.java` | 중분류 enum (~50개, getMajor() 포함) |
| `domain/product/dto/CategoryMapper.java` | Naver category1+2 → 내부 Category 변환 |
| `domain/product/repository/ProductRepository.java` | JOIN FETCH 쿼리, findByNaverProductId 포함 |
| `domain/search/service/NaverSearchService.java` | 검색 (DB + 네이버 멀티페이지 병합) |
| `docs/frontend-integration-guide.md` | 프론트엔드 연동 수정 가이드 |

---

## ⚠️ 남은 이슈 / 다음 세션 작업 후보

### 1. 리뷰 인사이트 미구현
- **현상**: "리뷰 인사이트" 섹션 (키워드, 만족/아쉬운 포인트) 항상 빈 칸
- **원인**: AI 서버 MVP에서 제외된 기능, NLP/키워드 추출 필요
- **해결 방향**: AI 서버팀에 키워드 추출 API 추가 요청 필요

### 2. reason code 한국어 매핑 불일치
- 프론트 `_codeToDescription()` 매핑 테이블이 실제 AI 코드와 불일치
- `frontend-integration-guide.md`에 수정 내용 정리 완료, 프론트 반영 필요

### 3. safe 리뷰 content null
- AI `risk-report`의 `sample_reviews`는 `warn`/`danger`만 포함
- `safe` 리뷰는 content/author/date가 null로 내려감
- 의도된 동작인지 AI 서버팀 확인 필요

### 4. 대부분 상품 AI 데이터 없음
- AI 서버에 `53530143052` 등 일부 상품만 데이터 존재
- 나머지 상품은 분석 트리거해도 빈 결과 반환
- AI 서버팀에 데이터 수집 범위 확대 요청 필요

### 5. Product.id 설계 이슈
- `Product.id` = naverProductId (수동 할당, `@GeneratedValue` 없음)
- 네이버 API가 productId를 반환하지 않으면 fallback ID(900_000_000_000~) 사용
- fallback ID 상품은 naverProductId가 null → AI 분석 불가
