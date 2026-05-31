# AI 서버 연동 명세 (Spring Boot ↔ FastAPI)

> 기준 버전: AI 서버 명세서 v11.0  
> 최종 수정: 2026-05-31

---

## 1. 개요

Spring Boot 백엔드는 FastAPI AI 서버와 HTTP 통신으로 연동됩니다.  
백엔드는 **product_id만 전달(Trigger)** 하고, AI 서버가 자체적으로 크롤링 및 분석을 수행합니다.

```
프론트엔드
    │
    │  POST /api/analysis/product
    ▼
Spring Boot (api.beens.kr)
    │
    │  5개 API 순차 호출 (safeCall - 개별 실패 허용)
    ▼
FastAPI AI 서버 (http://20.249.211.171:8000)
    ├── product-list   (상품 RTI 요약)
    ├── product-detail (개별 리뷰 분석)
    ├── rti-trend      (30일 추이)
    ├── review-report  (리뷰 상세 리포트)
    └── risk-report    (상품 위험도 리포트)
```

---

## 2. 설정

### application-prod.properties
```properties
ai.server.base-url=${AI_SERVER_URL:http://localhost:8000}
```

### .env.prod (EC2)
```
AI_SERVER_URL=http://20.249.211.171:8000
```

---

## 3. 프론트엔드 → 백엔드 API

### POST /api/analysis/product

상품 분석을 요청합니다.

**Request Body**
```json
{
  "productId": "7195971829",
  "productUrl": "https://smartstore.naver.com/..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `productId` | String | ✅ | 네이버 상품 ID 또는 내부 DB ID |
| `productUrl` | String | ❌ | 상품 페이지 URL (AI 서버 크롤링 보조용) |

**Response**
```json
{
  "success": true,
  "data": {
    "productId": "7195971829",
    "averageRti": 82.5,
    "level": "safe",
    "reviewCount": 120,
    "safeCount": 72,
    "warnCount": 36,
    "dangerCount": 12,
    "reviews": [
      {
        "reviewId": "1001",
        "rti": 91,
        "level": "safe",
        "textScore": 88,
        "behaviorScore": 95,
        "networkScore": 90,
        "reasons": []
      }
    ],
    "trend": [
      {
        "date": "2026-05-01",
        "averageRti": 80.5,
        "reviewCount": 5,
        "safeCount": 3,
        "warnCount": 2,
        "dangerCount": 0
      }
    ]
  }
}
```

### GET /api/analysis/health

AI 서버 연결 상태를 확인합니다.

**Response**
```json
{
  "success": true,
  "data": {
    "status": "ok",
    "message": "AI 서버 연결 성공",
    "aiServerUrl": "http://20.249.211.171:8000",
    "responseTimeMs": 123,
    "checkedAt": "2026-05-31T10:00:00"
  }
}
```

---

## 4. 백엔드 → AI 서버 API (내부)

### 공통 요청 형식 (TriggerRequest)

모든 AI 서버 API에 동일한 요청 형식을 사용합니다.

```json
{
  "product_id": "7195971829",
  "url": "https://...",
  "page_url": "https://...",
  "product_url": "https://..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `product_id` | String | ✅ | 분석 대상 상품 ID |
| `url` | String | ❌ | 상품 URL (호환용) |
| `page_url` | String | ❌ | 상품 URL (호환용) |
| `product_url` | String | ❌ | 상품 URL (호환용) |

> url / page_url / product_url 세 필드는 동일한 값으로 채워 보냅니다 (AI 서버 호환성)

---

### API 1: 상품 RTI 요약

```
POST /api/internal/ai/products/product-list
```

**Response**
```json
{
  "products": [
    {
      "product_id": "7195971829",
      "average_rti": 82.5,
      "level": "safe",
      "review_count": 120,
      "safe_count": 72,
      "warn_count": 36,
      "danger_count": 12
    }
  ]
}
```

---

### API 2: 개별 리뷰 상세 분석

```
POST /api/internal/ai/reviews/product-detail
```

**Response**
```json
{
  "results": [
    {
      "review_id": "1001",
      "product_id": "7195971829",
      "user_id": "user_abc",
      "rating": 5,
      "review_date": "2026-05-01",
      "rti": 91,
      "level": "safe",
      "signals": {
        "text": 88,
        "behavior": 95,
        "network": 90
      },
      "reasons": [
        { "code": "NATURAL_TEXT", "message": "자연스러운 문체" }
      ]
    }
  ]
}
```

---

### API 3: RTI 30일 추이

```
POST /api/internal/ai/products/rti-trend
```

**Response**
```json
{
  "trend": [
    {
      "date": "2026-05-01",
      "average_rti": 80.5,
      "review_count": 5,
      "safe_count": 3,
      "warn_count": 2,
      "danger_count": 0
    }
  ]
}
```

---

### API 4: 리뷰 상세 분석 리포트

```
POST /api/internal/ai/reviews/report
```

**Response**
```json
{
  "review_id": "1001",
  "rti": 91,
  "signals": {
    "text": 88,
    "behavior": 95,
    "network": 90
  },
  "reasons": [
    {
      "title": "자연스러운 문체",
      "description": "리뷰 텍스트가 자연스럽고 진정성이 느껴집니다."
    }
  ]
}
```

---

### API 5: 상품 위험도 리포트

```
POST /api/internal/ai/products/risk-report
```

**Response**
```json
{
  "product_id": "7195971829",
  "product_name": "삼성 갤럭시 S25 Ultra",
  "summary_stat": {
    "total_reviews": 120,
    "average_rti": 82.5,
    "danger_count": 12,
    "warn_count": 36,
    "safe_count": 72
  },
  "trend": [...],
  "sample_reviews": [
    {
      "review_id": "1005",
      "author": "user_***",
      "date": "2026.05.01",
      "rating": 5,
      "content": "완전 강추!! ...",
      "level": "danger",
      "reasons": [
        { "code": "EXCESSIVE_EXCLAMATION", "message": "과도한 느낌표 사용" }
      ]
    }
  ]
}
```

---

## 5. 동작 방식

### 분석 흐름

```
1. 프론트 → POST /api/analysis/product { productId, productUrl }
2. AiAnalysisService.analyzeProduct() 실행
3. AI 서버 3개 API 순차 호출 (safeCall 적용)
   - product-list  → 상품 RTI 요약
   - product-detail → 개별 리뷰 분석
   - rti-trend     → 30일 추이
4. 분석 결과로 DB 자동 업데이트
   - 각 리뷰의 rtiScore 업데이트
   - 상품의 avgRti, reviewCount 업데이트
5. 통합 결과(ProductAnalysisResponse) 프론트에 반환
```

### safeCall 처리

각 AI API 호출은 독립적으로 실패 허용됩니다.  
하나가 실패해도 나머지 결과는 그대로 사용합니다.

```java
// 예시: product-list 실패 시 null 반환, product-detail은 정상 처리
listResponse   = safeCall(() -> aiServerClient.analyzeProductList(request));   // null 가능
detailResponse = safeCall(() -> aiServerClient.analyzeProductDetail(request)); // null 가능
trendResponse  = safeCall(() -> aiServerClient.analyzeRtiTrend(request));      // null 가능
```

### RTI 레벨 기준

| 점수 | 레벨 | 의미 |
|---|---|---|
| 80 이상 | `safe` (SAFE) | 신뢰도 높음 |
| 50 ~ 79 | `warn` (SUSPICIOUS) | 의심 |
| 49 이하 | `danger` (DANGER) | 위험 |

---

## 6. 관련 파일

```
domain/ai/
├── client/
│   └── AiServerClient.java         # FastAPI HTTP 클라이언트
├── controller/
│   ├── AiAnalysisController.java   # POST /api/analysis/product
│   └── AiHealthController.java     # GET /api/analysis/health
├── service/
│   └── AiAnalysisService.java      # 분석 로직 + DB 업데이트
├── dto/
│   ├── request/
│   │   ├── AiAnalyzeRequest.java   # AI 서버 공통 요청 DTO
│   │   └── ProductAnalyzeRequest.java # 프론트 요청 DTO
│   └── response/
│       ├── ProductAnalysisResponse.java  # 프론트 응답 DTO (통합)
│       ├── ReviewAnalysisDto.java        # 개별 리뷰 분석 (정제)
│       ├── TrendEntryDto.java            # 추이 데이터 (정제)
│       ├── AiProductListResponse.java    # product-list 응답
│       ├── AiProductDetailResponse.java  # product-detail 응답
│       ├── AiRtiTrendResponse.java       # rti-trend 응답
│       ├── AiReviewReportResponse.java   # review-report 응답
│       ├── AiProductRiskReportResponse.java # risk-report 응답
│       ├── AiProductSummary.java         # 상품 요약 통계
│       ├── AiAnalysisResult.java         # 개별 리뷰 분석 결과
│       ├── AiSignals.java                # 분석 세부 점수
│       ├── AiTrendEntry.java             # 날짜별 추이
│       ├── AiReason.java                 # 판정 사유
│       ├── ReasonDetail.java             # 상세 사유 카드
│       ├── SampleReview.java             # 대표 이상 리뷰
│       └── SummaryStat.java              # 요약 통계
```
