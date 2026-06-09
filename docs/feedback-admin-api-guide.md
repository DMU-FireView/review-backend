# 피드백 & 관리자 API 연동 가이드

> 기준 브랜치: `feat/full-feature-expansion` (PR #109)
> 업데이트: 2026-06-09

---

## 공통 사항

```
베이스 URL: https://api.beens.kr
인증 헤더: Authorization: Bearer {accessToken}
공통 응답: { "success": true, "message": "...", "data": { ... } }
```

---

## 1. 피드백 시스템 개요

피드백은 **3가지 종류**로 구성됩니다.

| 종류 | 설명 | 엔드포인트 prefix |
|------|------|-----------------|
| ReviewFeedback | 리뷰 REAL/FAKE 투표 | `/api/reviews` |
| AnalysisFeedback | RTI 분석 결과 이의 제기 | `/api/analysis-feedbacks` |
| 통합 조회 | 위 두 가지 + 신고 합산 | `/api/users/me/feedback` |

---

## 2. 리뷰 피드백 (REAL / FAKE 투표)

### 2-1. 피드백 제출

```
POST /api/reviews/{reviewId}/feedback
Authorization: Bearer {token}
```

**Request Body**
```json
{
  "feedbackType": "REAL"
}
```

| feedbackType | 의미 |
|-------------|------|
| `REAL` | 진짜 리뷰라고 판단 |
| `FAKE` | 가짜 리뷰라고 판단 |

**Response 200**
```json
{
  "success": true,
  "message": "피드백이 등록되었습니다."
}
```

**에러**
| 상태코드 | errorCode | 의미 |
|---------|-----------|------|
| 409 | `FEEDBACK_ALREADY_EXISTS` | 이미 피드백 제출한 리뷰 |
| 404 | `REVIEW_NOT_FOUND` | 존재하지 않는 리뷰 |

---

### 2-2. 내 리뷰 피드백 목록

```
GET /api/reviews/feedbacks/me?page=0&size=10
Authorization: Bearer {token}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "feedbackId": 1,
        "reviewId": 10,
        "productName": "SOUNDPRO ANC X7 Pro",
        "reviewContent": "정말 좋아요...",
        "feedbackType": "REAL",
        "createdAt": "2026-06-09T14:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### 2-3. 내 리뷰 피드백 단건 조회

```
GET /api/reviews/feedbacks/me/{feedbackId}
Authorization: Bearer {token}
```

---

## 3. 분석 피드백 (RTI 이의 제기)

### 3-1. 분석 피드백 제출

```
POST /api/analysis-feedbacks/reviews/{reviewId}
Authorization: Bearer {token}
```

**Request Body**
```json
{
  "feedbackType": "SCORE_MISMATCH",
  "userJudgment": "MORE_TRUSTWORTHY",
  "relatedSignals": ["텍스트 표현", "구매확인 정보"],
  "detail": "실제 구매자인데 위험으로 분류되었습니다.",
  "attachmentUrl": "https://...",
  "replyEmail": "user@example.com"
}
```

**feedbackType 값**
| 값 | 의미 |
|----|------|
| `SCORE_MISMATCH` | 점수가 맞지 않아요 |
| `EXPLANATION_INSUFFICIENT` | 설명이 부족해요 |
| `INFO_INCORRECT` | 정보가 달라요 |
| `IMPROVEMENT_SUGGESTION` | 개선 제안 |

**userJudgment 값**
| 값 | 의미 |
|----|------|
| `MORE_TRUSTWORTHY` | 신뢰도가 더 높아요 |
| `MORE_RISKY` | 위험도가 더 높아요 |
| `UNDECIDED` | 판단 보류 |

**relatedSignals 선택값 (복수 선택 가능)**
```
"텍스트 표현", "구매확인 정보", "작성자 활동",
"네트워크 패턴", "시간대 패턴", "상품 정보"
```

> `attachmentUrl`, `replyEmail`, `userJudgment`, `relatedSignals`, `detail` 은 선택(optional)

**Response 200**
```json
{
  "success": true,
  "message": "분석 피드백이 접수되었습니다.",
  "data": {
    "feedbackId": 1,
    "reviewId": 10,
    "reviewContent": "...",
    "productName": "SOUNDPRO ANC X7 Pro",
    "feedbackType": "SCORE_MISMATCH",
    "feedbackTypeDescription": "점수가 맞지 않아요",
    "userJudgment": "MORE_TRUSTWORTHY",
    "relatedSignals": ["텍스트 표현"],
    "detail": "...",
    "attachmentUrl": null,
    "replyEmail": "user@example.com",
    "status": "SUBMITTED",
    "statusDescription": "접수",
    "createdAt": "2026-06-09T14:00:00",
    "updatedAt": "2026-06-09T14:00:00"
  }
}
```

**status 진행 단계**
| status | 의미 |
|--------|------|
| `SUBMITTED` | 접수 (1단계) |
| `UNDER_REVIEW` | 검토 중 (2단계) |
| `RESOLVED` | 처리 완료 (4단계) |
| `REJECTED` | 반려 (4단계) |

---

### 3-2. 내 분석 피드백 목록

```
GET /api/analysis-feedbacks/me?page=0&size=10
Authorization: Bearer {token}
```

응답 구조는 3-1 Response와 동일한 객체의 페이지

---

### 3-3. 내 분석 피드백 단건 조회

```
GET /api/analysis-feedbacks/me/{feedbackId}
Authorization: Bearer {token}
```

---

## 4. 통합 피드백 현황 조회

### 4-1. 내 피드백 + 신고 통합 목록

```
GET /api/users/me/feedback
Authorization: Bearer {token}
```

신고(Report) + 분석 피드백(AnalysisFeedback)을 **최신순**으로 합산하여 반환합니다.

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": 7,
      "feedbackCategory": "REPORT",
      "typeLabel": "리뷰 신고",
      "productName": "SOUNDPRO ANC X7 Pro",
      "reviewContent": "정말 좋아요...",
      "status": "UNDER_REVIEW",
      "statusDescription": "검토중",
      "currentStep": 2,
      "totalSteps": 4,
      "createdAt": "2026-06-09T14:00:00"
    },
    {
      "id": 3,
      "feedbackCategory": "ANALYSIS_FEEDBACK",
      "typeLabel": "점수가 맞지 않아요",
      "productName": "수분 장벽 앰플 50ml",
      "reviewContent": "촉촉하고...",
      "status": "SUBMITTED",
      "statusDescription": "접수",
      "currentStep": 1,
      "totalSteps": 4,
      "createdAt": "2026-06-08T10:00:00"
    }
  ]
}
```

**feedbackCategory 값**
| 값 | 의미 |
|----|------|
| `REPORT` | 리뷰 신고 |
| `ANALYSIS_FEEDBACK` | 분석 결과 피드백 |

**처리 단계 표시 (currentStep / totalSteps)**
```
1단계: 접수
2단계: 검토 중
3단계: (내부 처리)
4단계: 결과 안내 (완료 또는 반려)
```

---

## 5. 신고 API

### 5-1. 신고 제출

```
POST /api/reports/reviews/{reviewId}
Authorization: Bearer {token}
```

**Request Body**
```json
{
  "reason": "FAKE_REVIEW",
  "detail": "동일 문구가 반복되고 구매 이력이 없습니다.",
  "attachmentUrl": "https://...",
  "includeAiEvidence": true
}
```

**reason 값**
| 값 | 의미 |
|----|------|
| `FAKE_REVIEW` | 가짜 리뷰 / 리뷰 날바 의심 |
| `AI_GENERATED` | AI 생성 문체 의심 |
| `IRRELEVANT_CONTENT` | 상품과 무관한 내용 |
| `INAPPROPRIATE` | 부적절한 표현 / 개인정보 |
| `AD_REVIEW` | 광고성 리뷰 |
| `REPETITIVE_CONTENT` | 반복 내용 |
| `OTHER` | 기타 |

> `detail` 최소 20자 이상 필수
> `includeAiEvidence`: AI 분석 근거 함께 제출 여부 (true/false)

**Response 200**
```json
{
  "success": true,
  "data": {
    "reportId": 1,
    "reviewId": 10,
    "reviewContent": "...",
    "productName": "SOUNDPRO ANC X7 Pro",
    "reason": "FAKE_REVIEW",
    "reasonDescription": "가짜 리뷰 / 리뷰 날바 의심",
    "detail": "...",
    "attachmentUrl": null,
    "includeAiEvidence": true,
    "status": "PENDING",
    "statusDescription": "접수",
    "adminComment": null,
    "createdAt": "2026-06-09T14:00:00",
    "updatedAt": "2026-06-09T14:00:00"
  }
}
```

**에러**
| 상태코드 | errorCode | 의미 |
|---------|-----------|------|
| 409 | `REPORT_ALREADY_EXISTS` | 이미 신고한 리뷰 |

---

### 5-2. 내 신고 목록

```
GET /api/reports/me?page=0&size=10
Authorization: Bearer {token}
```

---

### 5-3. 내 신고 단건 조회

```
GET /api/reports/me/{reportId}
Authorization: Bearer {token}
```

---

## 6. 관리자 API (role: ADMIN 전용)

> ⚠️ 일반 유저 토큰으로 호출 시 **403 Forbidden** 반환

---

### 6-1. 운영 대시보드 통계

```
GET /api/admin/dashboard
Authorization: Bearer {adminToken}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "totalReviews": 1117,
    "pendingReports": 5,
    "pendingAnalysisFeedbacks": 3,
    "totalUsers": 42,
    "suspiciousReviewCount": 234,
    "dangerReviewCount": 47
  }
}
```

---

### 6-2. 의심 리뷰 목록

```
GET /api/admin/reviews/suspicious?maxRti=50&page=0&size=20
Authorization: Bearer {adminToken}
```

| 파라미터 | 기본값 | 설명 |
|---------|-------|------|
| `maxRti` | 50 | 이 점수 미만인 리뷰만 조회 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reviewId": 10,
        "productId": 53530143052,
        "productName": "SOUNDPRO ANC X7 Pro",
        "reviewerNickname": "reviewer_0099",
        "content": "정말 좋아요...",
        "rating": 5,
        "rtiScore": 18.0,
        "trustGrade": "DANGER",
        "reasons": ["반복 문구", "계정 생성 직후"],
        "isVerifiedPurchase": false,
        "writtenAt": "2026-05-02T10:24:00"
      }
    ],
    "totalElements": 234,
    "totalPages": 12
  }
}
```

---

### 6-3. 전체 신고 목록

```
GET /api/admin/reports?status=PENDING&page=0&size=20
Authorization: Bearer {adminToken}
```

| status 필터 | 의미 |
|------------|------|
| (없음) | 전체 조회 |
| `PENDING` | 접수 대기 |
| `UNDER_REVIEW` | 검토 중 |
| `ACCEPTED` | 처리완료 - 신고 인정 |
| `REJECTED` | 처리완료 - 신고 기각 |

---

### 6-4. 신고 상태 변경

```
PATCH /api/admin/reports/{reportId}
Authorization: Bearer {adminToken}
```

**Request Body**
```json
{
  "status": "ACCEPTED",
  "adminComment": "반복 문구 패턴 확인됨. 리뷰 숨김 처리 완료."
}
```

> 상태 변경 시 **신고자에게 알림 자동 발송**

---

### 6-5. 분석 피드백 검수 목록

```
GET /api/admin/analysis-feedbacks?status=SUBMITTED&page=0&size=20
Authorization: Bearer {adminToken}
```

| status 필터 | 의미 |
|------------|------|
| (없음) | 전체 조회 |
| `SUBMITTED` | 접수 대기 |
| `UNDER_REVIEW` | 검토 중 |
| `RESOLVED` | 처리 완료 |
| `REJECTED` | 반려 |

---

### 6-6. 분석 피드백 검수 처리

```
PATCH /api/admin/analysis-feedbacks/{feedbackId}
Authorization: Bearer {adminToken}
```

**Request Body**
```json
{
  "status": "RESOLVED",
  "adminComment": "RTI 점수 재산정 완료."
}
```

---

### 6-7. 전체 유저 목록

```
GET /api/admin/users?page=0&size=20
Authorization: Bearer {adminToken}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 1,
        "email": "user@example.com",
        "nickname": "닉네임",
        "role": "USER",
        "provider": "LOCAL",
        "atiScore": 78.5,
        "createdAt": "2026-04-18T00:00:00"
      }
    ],
    "totalElements": 42,
    "totalPages": 3
  }
}
```

---

## 7. Enum 전체 정리

### FeedbackType (리뷰 피드백)
| 값 | 의미 |
|----|------|
| `REAL` | 진짜 리뷰 |
| `FAKE` | 가짜 리뷰 |

### AnalysisFeedbackType
| 값 | 의미 |
|----|------|
| `SCORE_MISMATCH` | 점수가 맞지 않아요 |
| `EXPLANATION_INSUFFICIENT` | 설명이 부족해요 |
| `INFO_INCORRECT` | 정보가 달라요 |
| `IMPROVEMENT_SUGGESTION` | 개선 제안 |

### AnalysisFeedbackStatus
| 값 | 의미 | 단계 |
|----|------|------|
| `SUBMITTED` | 접수 | 1 |
| `UNDER_REVIEW` | 검토 중 | 2 |
| `RESOLVED` | 처리 완료 | 4 |
| `REJECTED` | 반려 | 4 |

### ReportReason
| 값 | 의미 |
|----|------|
| `FAKE_REVIEW` | 가짜 리뷰 / 리뷰 날바 의심 |
| `AI_GENERATED` | AI 생성 문체 의심 |
| `IRRELEVANT_CONTENT` | 상품과 무관한 내용 |
| `INAPPROPRIATE` | 부적절한 표현 / 개인정보 |
| `AD_REVIEW` | 광고성 리뷰 |
| `REPETITIVE_CONTENT` | 반복 내용 |
| `OTHER` | 기타 |

### ReportStatus
| 값 | 의미 |
|----|------|
| `PENDING` | 접수 |
| `UNDER_REVIEW` | 검토중 |
| `ACCEPTED` | 처리완료 - 신고 인정 |
| `REJECTED` | 처리완료 - 신고 기각 |

### UserJudgment
| 값 | 의미 |
|----|------|
| `MORE_TRUSTWORTHY` | 신뢰도가 더 높아요 |
| `MORE_RISKY` | 위험도가 더 높아요 |
| `UNDECIDED` | 판단 보류 |

### UnifiedFeedback.feedbackCategory
| 값 | 의미 |
|----|------|
| `REPORT` | 리뷰 신고 |
| `ANALYSIS_FEEDBACK` | 분석 결과 피드백 |
