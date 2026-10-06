# 프론트엔드 v2 전환 안내

Data 서버(review-data) 연동이 끝나 상품·리뷰를 **실제 쇼핑몰 데이터**로 받을 수 있게 됐습니다.
이 문서는 프론트가 바꿔야 할 것만 모았습니다. 전체 API 규격은 [api-spec.md](api-spec.md) 참고.

---

## 0. 먼저 — 챗봇은 이미 바뀌었습니다 ⚠️

운영 서버의 챗봇은 **이미 Data 서버를 보도록 전환**됐습니다. 지금 `productId` 를 기존처럼
Long 으로 보내면 챗봇이 상품을 못 찾고 "모른다"고 답합니다.

```jsonc
// 전
{ "productId": "900000000000", "question": "이 상품 어때?" }

// 후
{ "productId": "kurly-1000146248", "question": "이 상품 어때?" }
```

값은 v2 상품 응답의 `product.externalId` 를 그대로 넣으면 됩니다.
**이것만 먼저 고쳐도 챗봇이 정상 동작합니다.**

---

## 1. 무엇이 바뀌나

| | 지금 | 전환 후 |
|---|---|---|
| 상품 상세 | `GET /api/products/{Long}` | `GET /api/v2/products/{platform}/{productId}` |
| 리뷰 | 상품 응답에 포함 (Spring DB) | 같은 응답의 `reviews` (Data 서버) |
| 식별자 | `900000000000` | `kurly` + `1000146248` |
| 보이는 데이터 | 더미 33건 | 실제 쇼핑몰 상품 |

`lib/core/config/app_config.dart` 의 `searchPath` · `productPath` 가 `/api/products` 로
박혀 있습니다. 경로만 바꾸는 걸로는 끝나지 않습니다 — 아래 2~5번을 함께 처리해야 합니다.

`platform` 은 Data 서버 수집기 이름(소문자)입니다.

```
naver  kurly  elevenst  ably  auction  gmarket  musinsa  ohouse  oliveyoung
```

**상품 ID 하나만으로는 상세를 열 수 없습니다.** 목록·검색 결과에서 상세로 넘어갈 때
`platform` 을 함께 들고 다녀야 합니다.

---

## 2. 상품 상세 — `collectionStatus` 로 화면을 가릅니다

```
GET /api/v2/products/{platform}/{productId}?cursor={다음페이지커서}
```

**네 가지 상태가 전부 HTTP 200 입니다.** 이 값을 안 보면 빈 화면을 "고장"으로 오해합니다.

| `collectionStatus` | `product` | 화면 |
|---|---|---|
| `FRESH` | 있음 | 그대로 표시 |
| `STALE` | 있음 | 그대로 표시 (+ "갱신 중" 정도) |
| `QUEUED` | **null** | **로딩 화면.** `job.id` 로 완료를 기다린다 |
| `UNAVAILABLE` | **null** | 오류 안내. 상품이 없는 게 아니라 **못 가져온** 것 |

### 처음 보는 상품은 반드시 `QUEUED` 를 한 번 거칩니다

Data 서버가 그때 수집을 시작합니다. 로딩 화면 없이 바로 상세를 열면 **빈 화면**이 됩니다.

```
1. GET /api/v2/products/kurly/1000146248
   → { "collectionStatus": "QUEUED", "product": null, "job": { "id": 9, "status": "pending" } }

2. 로딩 화면 표시 + 2~3초 간격으로 폴링
   GET /api/v2/products/collection-jobs/9
   → { "status": "pending" }  …  → { "status": "succeeded" }

3. status 가 succeeded 또는 partial 이면 1번을 다시 호출
   → { "collectionStatus": "FRESH", "product": { ... } }
```

- `partial` 은 **상품만 수집되고 리뷰가 실패**한 상태입니다. 상품은 보여줄 수 있습니다
- `failed` 면 재시도해도 같을 가능성이 높습니다. `lastError` 를 안내하세요
- 수집은 보통 **10~20초** 걸립니다 (실측)

### `STALE` 은 실패가 아닙니다

TTL(상품 24h / 리뷰 6h)이 지났을 뿐 쓸 수 있는 데이터가 들어 있습니다.
최신을 기다리면 화면이 크롤링 속도에 묶입니다. 그냥 보여주세요.

### 실제 응답

```json
{
  "collectionStatus": "FRESH",
  "springProductId": null,
  "product": {
    "platform": "kurly",
    "productId": "1000146248",
    "externalId": "kurly-1000146248",
    "name": "[토리든] 다이브인 저분자 히알루론산 마스크팩 10매/1매 2종 (택)",
    "url": "https://www.kurly.com/goods/1000146248",
    "brand": "토리든",
    "price": 17000,
    "thumbnailUrl": "https://product-image.kurly.com/product/image/...",
    "category": "뷰티 인디 > 인디 스킨케어 > 인디 마스크/팩",
    "reviewCount": 1318,
    "rating": null,
    "lastCollectedAt": "2026-10-05T08:09:38+00:00"
  },
  "reviews": {
    "items": [
      { "reviewId": "138405291", "content": "속건조 잡는 데 딱입니다", "rating": null,
        "author": "김**", "writtenAt": "2026-10-04T09:45:58+00:00",
        "option": null, "images": ["https://img-cf.kurly.com/..."], "helpfulCount": 0 }
    ],
    "nextCursor": "WyIyMDI2LTA5LTIwVDE4OjExOjU3Kz..."
  },
  "job": null,
  "analysis": null
}
```

> 위 응답은 2026-10-05 운영 서버에서 실제로 받은 값입니다(이미지 URL 만 줄였습니다).

`reviewId` 는 **숫자처럼 보여도 문자열**입니다. 쇼핑몰마다 형식이 달라 그대로 문자열로 둡니다.

리뷰는 **cursor 페이지네이션**입니다. `reviews.nextCursor` 를 다음 요청의 `?cursor=` 에
그대로 넣습니다. `null` 이면 마지막 페이지입니다. 페이지 번호는 없습니다.

---

## 3. null 로 올 수 있는 필드

### 신뢰도 분석은 **아직 아무 데서도 제공하지 않습니다**

`analysis` 는 **항상 null** 입니다. Data 서버는 원본 수집만 하고, AI 서버는 아직 안 붙었습니다.
자리만 잡아둔 것이라, 분석이 붙어도 응답 모양은 안 바뀝니다.

기존 `/api/products` 쪽도 분석 전 상품이 섞이면 아래가 null 로 옵니다.

```
avgRti  rtiGrade  rtiLevel  rtiColor
category  categoryDisplayName  majorCategory  majorCategoryDisplayName
```

**기본값으로 그리지 마세요.** 예전에 서버가 `avgRti` 를 50 으로 채우고 있었는데, 분석도
하지 않은 상품에 신뢰도 50점이 붙어 "보통인 상품"처럼 보였습니다. 그래서 없앴습니다.
값이 없으면 "분석 전" 으로 그려야 합니다.

### `rating` 이 null 입니다

컬리 기준으로 상품 평균 평점도, 리뷰 개별 평점도 전부 null 로 옵니다.
**수집기가 평점을 아직 안 채웁니다.** 별점 UI 를 쓸 거면 Data 서버 담당자에게 확인이 필요합니다.
다른 쇼핑몰은 다를 수 있습니다.

---

## 4. 찜 · 장바구니

찜·장바구니 API 는 Spring 의 `productId`(Long)를 받습니다. Data 서버 상품은
`(platform, productId)` 로 식별되므로 **한 번 변환**해야 합니다.

```
1. 찜 버튼 클릭
2. POST /api/v2/products/kurly/1000146248/tag      (인증 필요)
   → { "springProductId": 1234, "externalId": "kurly-1000146248", "name": "..." }
3. POST /api/wishlist/1234                          (기존 API 그대로)
```

- **상품 상세 응답의 `springProductId` 가 이미 있으면 2번을 건너뜁니다**
- 여러 번 불러도 안전합니다. 같은 상품이면 늘 같은 번호가 나옵니다
- 조회·삭제·중복확인은 기존 API 를 그대로 씁니다

| 응답 | 상황 |
|---|---|
| `409 PRODUCT_NOT_COLLECTED` | 아직 수집 전. **상품이 없다는 뜻이 아닙니다.** 수집 후 다시 |
| `503 DATA_SERVER_UNAVAILABLE` | Data 서버에 닿지 못함 |

**찜·장바구니 목록에서 분석 전 상품은** `categoryDisplayName` · `avgRti` · `rtiGrade` ·
`rtiColor` 가 null 로 옵니다.

---

## 5. 신고 · 리뷰 피드백

v2 로 조회한 리뷰는 전용 경로를 씁니다. 요청 본문은 기존과 같습니다.

```
POST /api/reports/external/{platform}/{productId}/reviews/{externalReviewId}
POST /api/reviews/external/{platform}/{productId}/reviews/{externalReviewId}/feedback
```

`externalReviewId` 는 `reviews.items[].reviewId` 를 그대로 넣습니다.
**번호표가 없으면 자동으로 발급되므로 `tag` 를 따로 부를 필요 없습니다.**

### 조회할 때 — 리뷰 본문이 안 옵니다

Spring DB 에 그 리뷰 행이 없고, 본문을 클라이언트에게 받으면 신고 내용을 위조할 수
있어서 저장하지 않습니다. 신고·피드백 내역에서 Data 서버 리뷰는 이렇게 옵니다.

| 필드 | 값 |
|---|---|
| `reviewId` (Long) | **null** |
| `reviewContent` / `reviewContentSummary` | **null** |
| `externalReviewId` (String) | 채워짐 |
| `productExternalId` · `productName` | 채워짐 |

내역 화면에서는 상품명으로 가리키고, 리뷰 본문이 필요하면 상품 상세로 들어가야 합니다.
**기존 Spring 리뷰에 대한 신고·피드백은 전과 동일하게** 본문까지 내려옵니다.

---

## 6. 아직 백엔드에 없는 것

프론트 코드에 아래 호출이 있는데 **서버에 해당 엔드포인트가 없습니다.** 확인 부탁드립니다.

```
/api/plans
/api/users/me/plan
```

요금제 정보는 현재 이렇게 제공됩니다.

- `GET /api/users/me` 응답의 `planTier` · `planExpiresAt`
- `GET /api/chat/quota` — 남은 챗봇 횟수, 프로 기능 노출 여부(`proAvailable`)

---

## 7. 전환 체크리스트

- [ ] **챗봇 `productId` 를 `"{platform}-{productId}"` 로** ← 지금 깨져 있음, 제일 급함
- [ ] 상품 식별자를 `(platform, productId)` 두 값으로 들고 다니기
- [ ] `app_config.dart` 의 `productPath` · `searchPath` 교체
- [ ] `collectionStatus` 네 가지 분기 + `QUEUED` 로딩 화면 + job 폴링
- [ ] 리뷰 cursor 페이지네이션
- [ ] 분석 관련 필드 null 처리 ("분석 전" 표시)
- [ ] 찜·장바구니 `tag` 변환 끼우기
- [ ] 신고·피드백 external 경로로 교체
- [ ] 신고 내역에서 본문 null 처리
- [ ] **목록·검색 결과에 `externalId` 가 있으면 v2 상세로 열기**
- [ ] **`avgRti ?? 0.0` 제거 — null 을 "분석 전"으로** (지금은 분석 안 한 상품이 0점으로 보임)

---

## 8. 검색·홈 목록은 이제 실제 상품입니다 (2026-10-06)

> 이전 판에서 "검색은 느려서 전환 대상이 아니다"라고 적었는데 **틀렸습니다.**
> 실측해보니 Data 서버 검색이 몰당 0.2~0.7초였습니다.

`GET /api/products` 와 `GET /api/products?keyword=` 는 **경로·응답 모양 그대로**
Data 서버 상품을 내려줍니다. 프론트가 호출을 바꿀 필요는 없습니다.

다만 응답의 상품이 Data 서버 상품이면 **`externalId` 가 채워져** 옵니다.
이 상품은 이렇게 다뤄 주세요.

| | 해야 할 것 |
|---|---|
| 상세 열기 | `/product/:platform/:productId` (v2) 로. `dataPlatform`·`dataProductId` 를 쓴다 |
| 챗봇 | `productId` 에 `externalId` 를 그대로 |
| 신뢰도 | `avgRti` 가 **null** 이다. "분석 전" 으로 표시 |
| 카테고리 | `category` 는 null, `subCategory` 에 쇼핑몰 원문 |

### ⚠️ 지금 프론트에서 null 이 0 점으로 바뀝니다

`search_result_product_dto.dart` 와 `product_detail_dto.dart` 가 이렇게 읽습니다.

```dart
avgRti: _readDouble(json, ['avgRti', ...]) ?? 0.0,
avgRti: (json['avgRti'] as num?)?.toDouble() ?? 0.0,
```

서버는 분석 전이라 null 을 보내는데, 화면에서 **0 점(위험)** 으로 그려집니다.
분석도 안 한 상품이 위험 상품처럼 보입니다. null 을 그대로 두고 "분석 전"으로
분기해 주세요.

### 상세를 legacy 경로로 열면

`externalId` 가 있는 상품을 예전처럼 `/product/:id` 로 열어도 **상품 정보는** 나옵니다.
하지만 리뷰(`/api/products/{id}/reviews`)는 **빈 목록**입니다 — 리뷰는 Data 서버에 있고
v2 경로로만 내려갑니다. 그래서 v2 로 여는 게 맞습니다.

### 검색 대상 몰

컬리·올리브영·무신사·11번가 4곳입니다. 나머지는 Data 서버 쪽 사정으로 아직 안 됩니다
(오늘의집·네이버는 서버에서 브라우저가 안 뜸, 에이블리는 키 설정, G마켓은 차단,
옥션은 30초 이상). 고쳐지면 백엔드 설정만 바꿔 늘립니다.

---

## 문의

- API 규격 전체: [api-spec.md](api-spec.md)
- Swagger: https://api.re-view.kr/swagger-ui.html (태그 "상품 (Data 서버)")
