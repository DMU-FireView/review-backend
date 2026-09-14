# 완료 웹훅 규격 (Data 서버 → Spring)

> 대상: Data 서버 담당자
> Spring 구현: `domain/webhook`, `global/security/ServiceTokenFilter`

분석 job이 끝나면 Data 서버가 Spring에 이 웹훅을 호출한다. Spring은 요청자에게 알림을 발송한다.

---

## 1. 엔드포인트

```
POST https://api.beens.kr/api/internal/webhooks/analysis-complete
Content-Type: application/json
X-Service-Token: <SERVICE_TOKEN>
```

## 2. 인증

`X-Service-Token` 헤더에 양쪽이 공유하는 토큰을 그대로 싣는다.

- 값은 Spring의 `SERVICE_TOKEN` 환경변수와 **완전히 같아야** 한다
- 헤더가 없거나 다르면 `401`
- `/api/internal/**` 경로 전체에 적용된다

토큰 생성: `openssl rand -base64 32` — 한쪽이 만들고 다른 쪽에 안전한 경로로 전달한다. 채팅·이슈·커밋에 붙이지 말 것.

## 3. 요청 본문

```json
{
  "jobId": "a3f9c2e1-...",
  "productId": "naver-7195971829",
  "productName": "무선 이어폰 XYZ",
  "requesterEmail": "user@example.com",
  "status": "COMPLETED",
  "failureReason": null
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `jobId` | string (≤100) | ✅ | Data 서버가 발급한 job 식별자. **멱등성 키**로 쓰인다 |
| `productId` | string (≤100) | ✅ | 분석 대상 상품의 외부 식별자. 알림 클릭 시 `/products/{productId}`로 이동 |
| `productName` | string (≤200) | | 알림 문구에 표시. 없으면 상품명 없이 발송 |
| `requesterEmail` | string (email) | ✅ | 분석을 요청한 사용자. 알림 수신자 |
| `status` | `COMPLETED` \| `FAILED` | ✅ | |
| `failureReason` | string (≤500) | | `FAILED`일 때 알림에 함께 표시 |

`requesterEmail`은 Spring이 분석 요청을 Data에 전달할 때 함께 보내는 값을 그대로 돌려주면 된다.

## 4. 응답

| 상태 | 의미 | Data 서버 동작 |
|------|------|--------------|
| `200` | 처리됨 (이미 처리된 jobId 포함) | 재전송 중단 |
| `400` | 본문 형식 오류 | 재전송해도 같음 — 로그 남기고 중단 |
| `401` | 토큰 없음/불일치 | 재전송해도 같음 — 설정 확인 |
| `5xx` | Spring 내부 오류 | **재전송** (아래 참고) |

성공 응답 예:

```json
{ "success": true, "message": "웹훅이 처리되었습니다." }
```

## 5. 재전송과 멱등성

웹훅은 유실될 수 있으므로 Data 서버는 `2xx`를 받을 때까지 재전송한다. 권장:

- 최대 5회, 간격 1분 → 5분 → 15분 → 1시간 → 6시간
- `4xx`는 재전송하지 않는다

Spring은 같은 `jobId`를 **한 번만** 처리한다. 같은 jobId가 다시 오면 알림을 만들지 않고 `200`을 돌려준다. 따라서 Data 서버가 중복 전송해도 사용자에게 알림이 두 번 가지 않는다.

단, Spring이 처리 도중 `5xx`로 실패한 경우에는 해당 jobId의 처리 표시를 되돌리므로, 재전송하면 정상 처리된다.

처리 기록은 24시간 후 만료된다. 그 이후 같은 jobId를 보내면 새 웹훅으로 취급된다.

## 6. 수신자를 찾지 못하는 경우

`requesterEmail`에 해당하는 사용자가 없으면 Spring은 알림 없이 `200`을 반환하고 경고 로그만 남긴다. 재전송해도 결과가 같기 때문이다.

## 7. curl 예시

```bash
curl -X POST https://api.beens.kr/api/internal/webhooks/analysis-complete \
  -H "Content-Type: application/json" \
  -H "X-Service-Token: $SERVICE_TOKEN" \
  -d '{
    "jobId": "test-job-001",
    "productId": "naver-7195971829",
    "productName": "테스트 상품",
    "requesterEmail": "user@example.com",
    "status": "COMPLETED"
  }'
```

## 8. 향후 변경 가능성

- 현재는 고정 토큰 비교다. 토큰 유출 시 재전송 공격이 가능하므로, 필요해지면 본문+타임스탬프 HMAC 서명 방식으로 올릴 수 있다. 그 경우 이 문서를 먼저 갱신한다.
- `requesterEmail`을 본문에 싣는 대신 Spring이 분석 요청 시점에 jobId↔사용자 매핑을 보관하는 방식으로 바꿀 수 있다. 분석 요청 프록시가 구현되면 검토한다.
