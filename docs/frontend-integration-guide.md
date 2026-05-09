# 프론트엔드 연동 수정 가이드

> 백엔드 변경사항에 따른 Flutter 프론트엔드 수정 목록입니다.
> 기준 브랜치: `main` (PR #83, #84, #85 머지 완료 기준)

---

## 수정 파일 목록

| 파일 | 수정 유형 |
|------|---------|
| `lib/features/product_detail/data/dtos/product_analysis_dto.dart` | 필드 추가, 매핑 수정 |
| `lib/features/product_detail/data/dtos/product_detail_dto.dart` | 비율 필드 연동, trustSignals 파싱 |
| `lib/features/product_detail/presentation/view_models/product_detail_view_model.dart` | 분석 완료 후 상태 업데이트 |

---

## 1. `product_analysis_dto.dart`

### 1-1. `ProductAnalysisDto` — 새 필드 3개 추가

백엔드 `POST /api/analysis/product` 응답에 비율 통계와 주요 판단 신호 필드가 추가되었습니다.

**추가할 필드:**
```dart
final double realReviewRatio;   // 실사용 리뷰 비율 (0.0 ~ 100.0)
final double adSuspicionRatio;  // 광고성 의심 비율 (0.0 ~ 100.0)
final double repetitiveRatio;   // 반복 표현 비율  (0.0 ~ 100.0)
final List<TrustSignalDto> trustSignals; // 주요 판단 신호 4개
```

**`fromJson` 수정:**
```dart
factory ProductAnalysisDto.fromJson(Map<String, dynamic> json) {
  final rawReviews = json['reviews'] as List? ?? [];
  final rawSignals = json['trustSignals'] as List? ?? [];
  return ProductAnalysisDto(
    productId: json['productId']?.toString() ?? '',
    averageRti: (json['averageRti'] as num?)?.toDouble() ?? 0.0,
    level: json['level'] as String? ?? 'safe',
    reviewCount: (json['reviewCount'] as num?)?.toInt() ?? 0,
    safeCount: (json['safeCount'] as num?)?.toInt() ?? 0,
    warnCount: (json['warnCount'] as num?)?.toInt() ?? 0,
    dangerCount: (json['dangerCount'] as num?)?.toInt() ?? 0,
    reviews: rawReviews
        .map((e) => ReviewAnalysisItemDto.fromJson(e as Map<String, dynamic>))
        .toList(),
    // ↓ 새로 추가
    realReviewRatio: (json['realReviewRatio'] as num?)?.toDouble() ?? 0.0,
    adSuspicionRatio: (json['adSuspicionRatio'] as num?)?.toDouble() ?? 0.0,
    repetitiveRatio: (json['repetitiveRatio'] as num?)?.toDouble() ?? 0.0,
    trustSignals: rawSignals
        .map((e) => TrustSignalDto.fromJson(e as Map<String, dynamic>))
        .toList(),
  );
}
```

---

### 1-2. `TrustSignalDto` 클래스 추가

`product_analysis_dto.dart` 하단(또는 별도 파일)에 추가:

```dart
class TrustSignalDto {
  const TrustSignalDto({
    required this.label,
    required this.value,
    required this.isPositive,
  });

  final String label;      // 예: "구매인증 리뷰 비율"
  final String value;      // 예: "높음" | "보통" | "낮음" | "자연스러움" | "부자연스러움"
  final bool isPositive;   // true → check_circle(초록/파랑), false → cancel(주황)

  factory TrustSignalDto.fromJson(Map<String, dynamic> json) {
    return TrustSignalDto(
      label: json['label'] as String? ?? '',
      value: json['value'] as String? ?? '',
      isPositive: json['isPositive'] as bool? ?? false,
    );
  }

  // 도메인 엔티티로 변환
  TrustSignal toEntity() => TrustSignal(
    label: label,
    value: value,
    isPositive: isPositive,
  );
}
```

---

### 1-3. `ReviewAnalysisItemDto` — `content`, `author`, `date` 필드 추가

현재 파싱 누락된 3개 필드입니다. 리뷰 본문/작성자/날짜 표시에 필요합니다.

**추가할 필드:**
```dart
final String? content;  // 리뷰 본문 (warn/danger 리뷰만 제공, safe는 null)
final String? author;   // 작성자 마스킹 ID (예: "reviewer_0099")
final String? date;     // 작성일 (예: "2026.05.25")
```

**`fromJson` 수정:**
```dart
factory ReviewAnalysisItemDto.fromJson(Map<String, dynamic> json) {
  final rawReasons = json['reasons'] as List? ?? [];
  return ReviewAnalysisItemDto(
    reviewId: json['reviewId']?.toString() ?? '',
    rti: (json['rti'] as num?)?.toInt() ?? 0,
    level: json['level'] as String? ?? 'safe',
    textScore: (json['textScore'] as num?)?.toInt() ?? 0,
    behaviorScore: (json['behaviorScore'] as num?)?.toInt() ?? 0,
    networkScore: (json['networkScore'] as num?)?.toInt() ?? 0,
    reasons: rawReasons
        .map((e) => ReviewReasonDto.fromJson(e as Map<String, dynamic>))
        .toList(),
    // ↓ 새로 추가
    content: json['content'] as String?,
    author: json['author'] as String?,
    date: json['date'] as String?,
  );
}
```

---

### 1-4. `_codeToDescription` — reason code 매핑 테이블 수정

현재 매핑 테이블이 실제 AI 서버 코드와 거의 맞지 않습니다. `EXCESSIVE_EXCLAMATION` 외 대부분 영문 코드 그대로 표시됩니다.

**수정:**
```dart
static String _codeToDescription(String code) => switch (code) {
  'REPETITIVE_KEYWORD'         => '반복 표현 패턴이 감지되었습니다.',
  'PURCHASE_NOT_VERIFIED'      => '구매 이력이 확인되지 않았습니다.',
  'MULTIPLE_REVIEWS_SAME_DAY'  => '동일 작성자의 같은 날짜 다수 리뷰 작성이 감지되었습니다.',
  'NO_IMAGE_ATTACHED'          => '이미지 첨부가 없는 리뷰입니다.',
  'SIMILAR_REVIEW_CLUSTER'     => '유사 리뷰 네트워크 군집이 탐지되었습니다.',
  'SIMILAR_REVIEW_PATTERN'     => '일부 유사 리뷰 패턴이 탐지되었습니다.',
  'EXCESSIVE_EXCLAMATION'      => '과도한 느낌표 사용 패턴이 감지되었습니다.',
  'SHORT_REVIEW'               => '내용이 지나치게 짧은 리뷰입니다.',
  'LOW_QUALITY_SCORE'          => '리뷰 품질 점수가 낮습니다.',
  'PURCHASE_UNKNOWN'           => '구매 인증 여부가 불명확합니다.',
  'FREE_TRIAL_REVIEW'          => '체험단 리뷰로 의심됩니다.',
  'REPURCHASE_SIGNAL'          => '재구매 신호가 감지되었습니다.',
  _ => code,
};

static String _codeToIconType(String code) => switch (code) {
  'REPETITIVE_KEYWORD' || 'SIMILAR_REVIEW_CLUSTER' || 'SIMILAR_REVIEW_PATTERN' => 'repeat',
  'PURCHASE_NOT_VERIFIED' || 'PURCHASE_UNKNOWN' || 'FREE_TRIAL_REVIEW'         => 'history',
  'MULTIPLE_REVIEWS_SAME_DAY' || 'REPURCHASE_SIGNAL'                            => 'similarity',
  _                                                                              => 'context',
};
```

---

## 2. `product_detail_dto.dart`

### `_deriveRtiSummary()` — 비율 필드 하드코딩 제거

현재 `0.0`으로 하드코딩된 3개 필드를 분석 결과로 채워야 합니다.

`_deriveRtiSummary()`에 분석 결과를 받아서 사용하도록 변경:

```dart
// 기존: RtiSummary _deriveRtiSummary()
// 변경: 분석 결과를 파라미터로 받도록 수정

RtiSummary _deriveRtiSummary({
  double realReviewRatio = 0.0,
  double adSuspicionRatio = 0.0,
  double repetitionRatio = 0.0,
}) {
  final label = _gradeLabel(rtiGrade);
  return RtiSummary(
    rtiScore: avgRti.round(),
    rtiLabel: label,
    rtiSubLabel: 'AI 분석 결과',
    realReviewRatio: realReviewRatio / 100,       // 0~100 → 0.0~1.0
    realReviewLabel: realReviewRatio > 0 ? '${realReviewRatio.toStringAsFixed(1)}%' : '집계 중',
    adSuspicionRatio: adSuspicionRatio / 100,
    adSuspicionLabel: adSuspicionRatio > 0 ? '${adSuspicionRatio.toStringAsFixed(1)}%' : '집계 중',
    repetitionRatio: repetitionRatio / 100,
    repetitionLabel: repetitionRatio > 0 ? '${repetitionRatio.toStringAsFixed(1)}%' : '집계 중',
    summaryMessage: '$reviewCount개 리뷰 기반 RTI 분석 결과입니다.',
    analyzedReviewCount: reviewCount,
  );
}
```

### `trustSignals` 하드코딩 제거

```dart
// 기존
trustSignals: const [],

// 변경: ProductAnalysisDto를 받아서 변환
trustSignals: analysisDto?.trustSignals.map((s) => s.toEntity()).toList() ?? const [],
```

---

## 3. `product_detail_view_model.dart`

### 분석 완료 후 `rtiSummary`와 `trustSignals` 상태 업데이트 추가

현재 분석 완료 시 `safeCount`, `warnCount`, `dangerCount`만 업데이트하고 있습니다.
`rtiSummary`와 `trustSignals`도 함께 업데이트해야 합니다.

```dart
// 기존
state = current.copyWith(
  reviews: enrichedReviews,
  isAnalyzing: false,
  safeCount: analysis.safeCount,
  warnCount: analysis.warnCount,
  dangerCount: analysis.dangerCount,
);

// 변경
state = current.copyWith(
  reviews: enrichedReviews,
  isAnalyzing: false,
  safeCount: analysis.safeCount,
  warnCount: analysis.warnCount,
  dangerCount: analysis.dangerCount,
  // ↓ 새로 추가
  rtiSummary: current.productDetail.rtiSummary.copyWith(
    realReviewRatio: analysis.realReviewRatio / 100,
    realReviewLabel: '${analysis.realReviewRatio.toStringAsFixed(1)}%',
    adSuspicionRatio: analysis.adSuspicionRatio / 100,
    adSuspicionLabel: '${analysis.adSuspicionRatio.toStringAsFixed(1)}%',
    repetitionRatio: analysis.repetitiveRatio / 100,
    repetitionLabel: '${analysis.repetitiveRatio.toStringAsFixed(1)}%',
  ),
  trustSignals: analysis.trustSignals.map((s) => s.toEntity()).toList(),
);
```

---

## 백엔드 응답 예시 (참고)

`POST /api/analysis/product` 응답:

```json
{
  "success": true,
  "data": {
    "productId": "53530143052",
    "averageRti": 78.33,
    "level": "warn",
    "reviewCount": 3,
    "safeCount": 1,
    "warnCount": 2,
    "dangerCount": 0,
    "realReviewRatio": 33.3,
    "adSuspicionRatio": 66.7,
    "repetitiveRatio": 33.3,
    "trustSignals": [
      { "label": "구매인증 리뷰 비율", "value": "낮음",       "isPositive": false },
      { "label": "텍스트 다양성",     "value": "보통",       "isPositive": false },
      { "label": "반복 표현 비율",    "value": "보통",       "isPositive": false },
      { "label": "작성 시점 패턴",    "value": "자연스러움",  "isPositive": true  }
    ],
    "reviews": [
      {
        "reviewId": "1001",
        "content": "이 제품 정말 최고예요...",
        "author": "reviewer_0099",
        "date": "2026.05.25",
        "rti": 58,
        "level": "warn",
        "textScore": 70,
        "behaviorScore": 50,
        "networkScore": 50,
        "reasons": [
          { "code": "REPETITIVE_KEYWORD", "message": "반복 표현 탐지: '대박' 2회" }
        ]
      }
    ],
    "trend": [ ... ]
  }
}
```

> **참고:** `reviews[].content`, `author`, `date`는 `warn`/`danger` 리뷰만 제공됩니다. `safe` 리뷰는 `null`입니다.
