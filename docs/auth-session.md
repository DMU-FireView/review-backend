# 로그인 세션 (리프레시 토큰) — 30분 유휴 만료

이슈 #213. 일반 로그인·OAuth 공통.

- **액세스 토큰(JWT)**: 짧게 산다. `JWT_EXPIRATION_MS` (기본 24시간 → 프론트 연동 후 15분)
- **리프레시 토큰**: 무작위 불투명 값. 서버는 SHA-256 해시만 저장(Redis), TTL 30분.
  refresh 할 때마다 **회전**(새 값 발급, TTL 재설정, 옛 값 소비) → 30분 동안 활동(refresh)이 없으면 로그아웃.
- 웹은 HttpOnly 쿠키, 앱은 본문으로 주고받는다.

## 1. 엔드포인트

모두 `/api/auth/**` 아래라 인증 없이 호출한다(`permitAll`).

| 요청 | 결과 |
|------|------|
| `POST /api/auth/login`, `/signup` | 기존 `LoginResponse` 그대로 + `Set-Cookie: review_rt` |
| `POST /api/auth/refresh` | 새 `LoginResponse`(같은 모양) + 회전된 `Set-Cookie` / 실패 시 401 `REFRESH_TOKEN_INVALID` + 쿠키 삭제 |
| `POST /api/auth/logout` | 리프레시 토큰(그 로그인의 패밀리) 폐기 + 쿠키 삭제. 토큰이 없어도 200 |
| OAuth 성공 302 | 기존 쿼리 `accessToken` 그대로 + `Set-Cookie: review_rt` |
| `POST /api/auth/password/reset` 성공 | 그 사용자의 모든 리프레시 토큰 폐기 |
| `DELETE /api/users/me` (탈퇴) | 그 사용자의 모든 리프레시 토큰 폐기 |

### 예시

```http
POST /api/auth/login
Content-Type: application/json

{"email":"user@example.com","password":"pw1234!!"}
```

```http
HTTP/1.1 200
Set-Cookie: review_rt=Q2x...43자; Path=/api/auth; Max-Age=1800; Expires=...; Secure; HttpOnly; SameSite=Lax

{"success":true,"message":"요청이 성공적으로 처리되었습니다.",
 "data":{"accessToken":"eyJ...","tokenType":"Bearer","email":"user@example.com",
         "nickname":"홍길동","role":"USER","onboardingCompleted":false}}
```

`X-Client-Platform: app` 을 붙이면 `data.refreshToken` 이 추가된다. 웹에는 절대 싣지 않는다.

```http
POST /api/auth/refresh
Cookie: review_rt=Q2x...

HTTP/1.1 200
Set-Cookie: review_rt=Zk9...새 값; Path=/api/auth; Max-Age=1800; ...; Secure; HttpOnly; SameSite=Lax
{"success":true,"data":{"accessToken":"eyJ...새 값","tokenType":"Bearer", ...}}
```

```http
POST /api/auth/refresh            (쿠키 없음·만료·폐기·재사용)

HTTP/1.1 401
Set-Cookie: review_rt=; Path=/api/auth; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Lax
{"success":false,"message":"로그인 세션이 만료되었습니다. 다시 로그인해주세요.","errorCode":"REFRESH_TOKEN_INVALID"}
```

```http
POST /api/auth/logout

HTTP/1.1 200
Set-Cookie: review_rt=; Path=/api/auth; Max-Age=0; ...
{"success":true,"message":"로그아웃되었습니다."}
```

## 2. 동시 refresh(여러 탭)와 재사용 탐지

탭 여러 개가 같은 쿠키로 거의 동시에 refresh 하면, 먼저 도착한 요청이 옛 값을 소비하고 나머지는
"이미 소비된 값" 을 내밀게 된다.

- **소비된 지 15초(`REFRESH_TOKEN_REUSE_GRACE`) 안**: 실패시키지 않고 **같은 패밀리로 새 값을 하나 더 발급**한다.
  두 탭이 받은 값이 모두 유효하므로 브라우저 쿠키에 어느 쪽이 남아도 다음 refresh 가 된다.
  - "같은 결과를 그대로 돌려주는" 방식은 새 원문을 서버에 보관해야 해서, 해시만 저장한다는 원칙과 충돌해 택하지 않았다.
- **15초를 넘겨 다시 오면**: 탈취된 값의 재사용으로 보고 **그 패밀리(그 로그인) 전체를 폐기**하고 401.
  정상 사용자도 그 기기에서 다시 로그인해야 하지만, 탈취자의 세션도 함께 끊긴다. 다른 기기의 로그인은 유지된다.

프론트가 동시 401 을 하나의 refresh 로 묶으면(아래 3절) 유예는 거의 쓰이지 않는다. 유예는 탭 사이처럼
프론트가 묶을 수 없는 경우를 위한 것이다.

## 3. 프론트 연동 가이드

### 웹 (re-view.kr)

- **`withCredentials` 불필요.** 웹은 `Uri.base.origin`(re-view.kr) 으로 호출하고 Vercel 이 `/api/*`,
  `/login/oauth2/*` 를 api.re-view.kr 로 rewrite 한다. 브라우저 입장에서는 같은 출처 요청이라
  쿠키가 자동으로 저장·전송된다. 쿠키는 HttpOnly 라 JS 에서 읽을 수도, 읽을 필요도 없다.
- 액세스 토큰은 지금처럼 메모리/`localStorage` 에 두고 `Authorization` 헤더로 보낸다.
  리프레시 토큰은 프론트가 다루지 않는다.
- **401 처리** (`api_client.dart` 의 `onError`):
  1. 401 이고 요청이 `/api/auth/*` 가 아니면 `POST /api/auth/refresh` 를 **한 번** 부른다.
  2. 동시에 여러 요청이 401 을 받으면 refresh 는 **하나만** 띄우고 나머지는 그 결과(Future)를 기다린다.
  3. 성공 → 새 `accessToken` 저장 → 원래 요청을 새 헤더로 **1회만** 재시도.
  4. refresh 가 401 이면 → `expireSession()`(로그아웃 처리, 로그인 화면). 재시도하지 않는다.
- **앱 시작 시**: 저장된 액세스 토큰이 없거나 만료됐으면 refresh 를 한 번 시도해 세션을 복구할 수 있다.
- **로그아웃**: `POST /api/auth/logout` 호출 후 로컬 액세스 토큰 삭제. 호출이 실패해도 로컬 삭제는 한다.
- OAuth 콜백 처리는 바뀌지 않는다(쿼리 `accessToken` 그대로). 쿠키는 콜백 302 에서 이미 심긴다.

```dart
// 개념 예시 — 동시 401 을 refresh 하나로 묶는다
Future<String?>? _refreshing;

Future<String?> _refreshOnce(Dio dio) {
  return _refreshing ??= dio
      .post('/api/auth/refresh')
      .then((r) => r.data['data']['accessToken'] as String?)
      .catchError((_) => null)
      .whenComplete(() => _refreshing = null);
}
```

- **로컬 웹 개발 주의**: localhost 에서 실행하면 `_defaultApiBaseUrl()` 이 api.re-view.kr 을 직접 부른다.
  이건 다른 사이트 요청이라 `SameSite=Lax` 쿠키가 실리지 않아 refresh 가 항상 401 이다.
  로컬에서 refresh 를 확인하려면 로컬 백엔드(`http://localhost:8080`, `REFRESH_COOKIE_SECURE=false`)에
  `withCredentials: true` 로 붙인다(localhost 끼리는 같은 사이트).

### 앱 (api.re-view.kr 직접)

- 로그인·회원가입·refresh 요청에 `X-Client-Platform: app` 헤더를 붙이면 본문 `data.refreshToken` 으로 받는다.
- 저장은 보안 저장소(iOS Keychain / Android Keystore, 예: `flutter_secure_storage`). `SharedPreferences` 금지.
- refresh: `POST /api/auth/refresh` + 헤더 + 본문 `{"refreshToken":"..."}`. 응답의 새 `refreshToken` 으로 **반드시 교체** 저장.
- 로그아웃: `POST /api/auth/logout` + 본문 `{"refreshToken":"..."}`.
- 401 처리 규칙은 웹과 같다(동시 요청 단일화, 1회 재시도, 실패 시 로그아웃).
- 앱 OAuth 는 현재 웹 콜백 흐름을 쓰므로 쿠키로만 받는다. 앱 전용 OAuth 흐름이 생기면 별도로 다룬다.

## 4. 배포 순서

1. **백엔드 배포** (이 변경). 기능은 추가만 된다. `JWT_EXPIRATION_MS` 미설정 → 액세스 토큰은 기존대로 24시간.
   - 운영은 `app.auth.redis-token-store.enabled=true`(prod 프로필)라 Redis 저장소를 쓴다.
   - `CORS_ALLOWED_ORIGINS` 에 `*` 가 들어 있으면 기동이 실패하니 배포 전에 확인한다.
2. **프론트 refresh 연동 배포** (3절). 이 시점부터 사용자는 30분 유휴 만료를 겪는다
   (액세스 토큰이 24시간이라 실제 체감은 3단계 이후).
3. **운영 환경변수 `JWT_EXPIRATION_MS=900000`(15분) 설정 후 재기동.**
   순서를 바꿔 프론트 연동 전에 줄이면 활동 중인 사용자가 15분마다 로그아웃된다.

## 5. 설정

| 프로퍼티 | 환경변수 | 기본값 |
|---------|---------|--------|
| `jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (목표 `900000`) |
| `app.auth.refresh.ttl` | `REFRESH_TOKEN_TTL` | `PT30M` |
| `app.auth.refresh.reuse-grace` | `REFRESH_TOKEN_REUSE_GRACE` | `PT15S` |
| `app.auth.refresh.cookie.name` | `REFRESH_COOKIE_NAME` | `review_rt` |
| `app.auth.refresh.cookie.path` | `REFRESH_COOKIE_PATH` | `/api/auth` |
| `app.auth.refresh.cookie.secure` | `REFRESH_COOKIE_SECURE` | `true` (로컬 http 는 `false`) |
| `app.auth.refresh.cookie.same-site` | `REFRESH_COOKIE_SAME_SITE` | `Lax` |
| `app.auth.redis-token-store.enabled` | — | 기본 `false`(인메모리), prod `true` |

## 6. 저장 구조 (Redis)

```
rt:{sha256}         → "userId:familyId"             TTL 30분 (살아 있는 토큰)
rt:used:{sha256}    → "userId:familyId:consumedMs"  TTL 30분 (회전으로 소비됨 — 유예·재사용 판단)
rt:fam:{familyId}   → Set(sha256)                   저장 때마다 TTL 갱신 (로그아웃·재사용 시 패밀리 폐기)
rt:user:{userId}    → Set(familyId)                 저장 때마다 TTL 갱신 (비밀번호 재설정·탈퇴 시 전체 폐기)
```

패밀리 = 한 번의 로그인에서 회전으로 이어진 토큰들. 모든 변경은 Lua 스크립트로 원자적이다
(소비는 GET+DEL+SET 을 한 스크립트로). 스크립트가 키 이름을 안에서 만들어 Redis Cluster 에선 못 쓴다(운영은 단일 노드).

## 7. 보안 판단

- **CSRF**: 쿠키만으로 인증하는 곳은 `POST /api/auth/refresh`·`/logout` 뿐이다. `SameSite=Lax` 는 다른 사이트에서
  시작된 POST 에 쿠키를 싣지 않는다. 같은 사이트 안에서 위조돼도 refresh 결과(새 액세스 토큰)는 CORS 를 통과한
  출처만 읽을 수 있고, 쿠키는 브라우저가 새 값으로 갈아끼울 뿐이라 공격자가 얻는 게 없다. logout 위조는 강제 로그아웃뿐.
  그래서 Origin/Referer 검사는 더하지 않았다.
- **CORS**: `allowCredentials=true` 와 `*` 패턴 조합은 기동 시 거부한다(`SecurityConfig.requireExplicitOrigins`).
- **로그**: 토큰 원문·해시를 로그에 남기지 않는다. 재사용 감지 시 userId·familyId 만 WARN.
- **쿠키**: Domain 속성 없음 → re-view.kr 호스트 전용. 리프레시 토큰은 URL 쿼리로 주지 않는다.

## 8. 알려진 한계

- 액세스 토큰은 무효화하지 않는다. 로그아웃·비밀번호 재설정 뒤에도 이미 발급된 액세스 토큰은 만료(15분 목표)까지 유효하다.
- `User` 에 정지(suspended) 상태가 없어 "정지 사용자 refresh 거부" 는 사용자 삭제 여부로만 판단한다. 정지 상태가 생기면
  `RefreshTokenService.rotate` 에서 함께 막는다.
- 인메모리 저장소(로컬·테스트)는 패밀리 인덱스를 정리하지 않는다. 장기 실행용이 아니다.
