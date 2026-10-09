# 로그인 세션 (리프레시 토큰) — 30분 유휴 만료

이슈 #213. 일반 로그인·OAuth 공통.

- **액세스 토큰(JWT)**: 짧게 산다. `JWT_EXPIRATION_MS` (기본 24시간 → 프론트 연동 후 15분)
- **리프레시 토큰**: 무작위 불투명 값. 서버는 SHA-256 해시만 저장(Redis), TTL 30분.
  refresh 할 때마다 **회전**(새 값 발급, TTL 재설정, 옛 값 소비) → 30분 동안 활동(refresh)이 없으면 로그아웃.
- 웹은 HttpOnly 쿠키, 앱은 본문으로 주고받는다. **한 응답에서 두 경로로 동시에 내보내지 않는다.**

## 1. 엔드포인트

모두 `/api/auth/**` 아래라 인증 없이 호출한다(`permitAll`).

| 요청 | 결과 |
|------|------|
| `POST /api/auth/login`, `/signup` | 기존 `LoginResponse` 그대로 + `Set-Cookie: review_rt` (앱이면 쿠키 대신 본문 `refreshToken`, 1-1절) |
| `POST /api/auth/refresh` | 새 `LoginResponse`(같은 모양) + 받은 경로로 회전된 값 / 실패 시 401 `REFRESH_TOKEN_INVALID` + 쿠키 삭제 / 저장소 장애 시 503 |
| `POST /api/auth/logout` | 리프레시 토큰(그 로그인의 패밀리) 폐기 + 쿠키 삭제. 토큰이 없어도 200 / 저장소 장애 시 503 |
| OAuth 성공 302 | 기존 쿼리 `accessToken` 그대로 + `Set-Cookie: review_rt` (저장소 장애 시 새 쿠키 대신 기존 쿠키 삭제) |
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

### 1-1. 리프레시 토큰이 나가는 경로 (쿠키 vs 본문)

| 요청 | 응답 |
|------|------|
| 로그인·회원가입, 앱 헤더 없음 | `Set-Cookie` 만 |
| 로그인·회원가입, `X-Client-Platform: app` + `Origin`·`Sec-Fetch-Site`·`Sec-Fetch-Mode` 모두 없음 | 본문 `data.refreshToken` 만 (Set-Cookie 없음) |
| 로그인·회원가입, 앱 헤더가 있지만 위 브라우저 헤더 중 하나라도 있음 | 웹으로 본다 → `Set-Cookie` 만 |
| refresh, 쿠키 `review_rt` 로 옴 | `Set-Cookie` 만. **`X-Client-Platform` 은 보지 않는다** |
| refresh, 쿠키 없이 본문 `refreshToken` 으로 옴 | 본문 `data.refreshToken` 만 (Set-Cookie 없음) |

이유(#213 리뷰 P2-1): 처음에는 "쿠키는 항상, 앱 헤더면 본문에도" 였다. 그러면 re-view.kr 에 XSS 가 하나라도 생겼을 때
`fetch('/api/auth/refresh', {method:'POST', headers:{'X-Client-Platform':'app'}})` 한 줄로 HttpOnly 쿠키 값이
본문으로 나와, HttpOnly 로 막으려던 원문 탈취가 그대로 된다(같은 출처 fetch 는 쿠키를 자동으로 싣고 CORS 도 막지 않는다).
그래서 앱 헤더를 신뢰 경계로 쓰지 않는다.

- refresh 는 **토큰이 들어온 경로**로 응답 경로를 정한다. 쿠키로 인증한 요청의 응답 본문에는 어떤 헤더여도 토큰이 없다.
  스크립트가 쿠키를 빼고(`credentials:'omit'`) 보내면 토큰이 없어 401 이다.
- 로그인·회원가입은 쿠키가 없으니 헤더로 고를 수밖에 없다. 대신 브라우저가 붙이고 페이지 스크립트가 지우거나 바꿀 수 없는
  헤더(`Origin` — 브라우저는 POST 에 항상 붙인다, `Sec-Fetch-*`)가 있으면 앱 모드를 거부하고 웹 응답(쿠키만)으로 처리한다.
  400 으로 막지 않은 건, 웹 응답으로 내려도 토큰이 노출되지 않고 기존 웹 동작도 그대로이기 때문이다.
  XSS 공격자는 비밀번호를 알아야 로그인할 수 있으니 이 경로로 얻는 것도 없다.
- 네이티브 앱(Flutter `http`/`dio`, 모바일)은 `Origin`·`Sec-Fetch-*` 를 보내지 않는다. 앱에서 이 헤더를 직접 붙이면 본문으로 받지 못한다.

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
- **refresh 503 `AUTH_SESSION_UNAVAILABLE`**: 서버 쪽 일시 장애다. 로그아웃시키지 말고(쿠키도 그대로다) 원래 요청을 실패로
  처리하거나 잠시 뒤 다시 시도한다. 401 과 구분해야 한다.
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

- 로그인·회원가입 요청에 `X-Client-Platform: app` 헤더를 붙이면 본문 `data.refreshToken` 으로 받는다(쿠키는 내려오지 않는다).
  `Origin`·`Sec-Fetch-*` 헤더는 보내지 않는다(1-1절).
- 저장은 보안 저장소(iOS Keychain / Android Keystore, 예: `flutter_secure_storage`). `SharedPreferences` 금지.
- refresh: `POST /api/auth/refresh` + 본문 `{"refreshToken":"..."}`(헤더는 있어도 없어도 된다). 응답의 새 `refreshToken` 으로 **반드시 교체** 저장.
- 로그인 응답에 `refreshToken` 이 없을 수 있다(리프레시 저장소 장애, 3-1절). 그때는 **저장해 둔 이전 `refreshToken` 을 지우고** 액세스 토큰만 저장하며, 만료되면 다시 로그인시킨다.
- 로그아웃: `POST /api/auth/logout` + 본문 `{"refreshToken":"..."}`.
- 401 처리 규칙은 웹과 같다(동시 요청 단일화, 1회 재시도, 실패 시 로그아웃).
- 앱 OAuth 는 현재 웹 콜백 흐름을 쓰므로 쿠키로만 받는다. 앱 전용 OAuth 흐름이 생기면 별도로 다룬다.

## 3-1. 리프레시 저장소(Redis) 장애 시 동작

#213 리뷰 P2-2. 리프레시 저장소가 생기기 전에는 로그인이 DB·JWT 만으로 끝났다. 저장소 쓰기를 필수로 두면 Redis 장애
하나로 정상 자격 증명 로그인·OAuth 까지 500 이 돼 기존보다 가용성이 나빠진다. 그래서 경로별로 다르게 처리한다.

| 경로 | 저장소 장애 시 | 이유 |
|------|---------------|------|
| 로그인·회원가입·OAuth (처음 발급) | **성공**. 액세스 토큰만 주고 리프레시 토큰(쿠키·본문)은 생략. 웹은 **기존 `review_rt` 쿠키를 삭제**(`Max-Age=0`). WARN 로그(userId·예외 종류만) | 새 세션을 만드는 것이라 리프레시가 없어도 잃는 게 없다. 그 세션은 액세스 토큰 만료와 함께 끝난다 |
| refresh | **503 `AUTH_SESSION_UNAVAILABLE`**, 쿠키 유지 | 401 이면 프론트가 로그아웃시키고 쿠키도 지워져, 저장소가 돌아와도 살아 있던 세션을 못 쓴다 |
| logout | **503**, 쿠키 유지 | 폐기를 확인할 수 없는데 200 을 주면(fail-open) 폐기했다고 믿은 토큰이 30분 동안 살아 있다. 다시 시도할 수 있게 쿠키를 남긴다 |
| 비밀번호 재설정·탈퇴의 전체 폐기 | **503**, 트랜잭션 롤백 | 다른 기기 세션을 끊지 못한 채 비밀번호만 바뀌는 상태를 만들지 않는다 |

- 처음 발급이 장애로 생략될 때 기존 쿠키를 지우는 이유: 이 브라우저에 다른 계정(A)의 쿠키가 남아 있는 채로 B 로 로그인하면,
  복구 뒤 refresh 가 A 의 토큰을 돌려줘 사용자가 모르게 계정이 바뀐다(#213 재리뷰). 앱은 로그인 응답에 `refreshToken` 이
  없으면 저장해 둔 이전 값을 지워야 한다.
- 저장소 예외는 Spring 의 `DataAccessException`(Redis 연결 실패·타임아웃 등)으로 판단한다(`RefreshTokenService.withStore`).
- 로그에는 토큰 원문·해시·예외 메시지를 남기지 않고 예외 클래스 이름만 남긴다(Lua 인자가 메시지에 섞일 수 있다).
- refresh 도중 소비는 됐는데 새 값 저장에서 장애가 나면 503 이고 옛 값은 "소비됨" 상태로 남는다. 15초 유예 안의 재시도는
  성공하지만, 그 뒤 재시도는 재사용으로 판단돼 401(패밀리 폐기)이 된다. 장애가 길면 결국 다시 로그인해야 한다.
- 액세스 토큰 TTL 을 15분으로 줄인 뒤(4절 3단계)에는 장애 중 로그인한 사용자가 15분 뒤 다시 로그인해야 한다.
  이 이상이 필요해지면 짧은 Redis timeout, circuit breaker, Redis 고가용성·모니터링을 따로 설계한다.

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

- **CSRF**: 쿠키만으로 인증하는 곳은 `POST /api/auth/refresh`·`/logout` 뿐이다. 방어는 두 겹이다.
  - `SameSite=Lax` 는 **다른 사이트**에서 시작된 POST 에 쿠키를 싣지 않는다.
  - 같은 사이트의 다른 서브도메인(data.re-view.kr, ai.re-view.kr 등)은 Lax 로 막히지 않는다. 이건 Spring Security CORS 가
    막는다 — 허용 목록에 없는 `Origin` 이 붙은 요청은 단순 POST(form-urlencoded)라도 컨트롤러 전에 403 이다(리뷰에서 확인).
    그래서 `CORS_ALLOWED_ORIGINS` 에 서브도메인을 넣으면 그 서브도메인은 refresh·logout 을 위조할 수 있게 된다는 점에 주의한다.
  - refresh 결과(새 액세스 토큰)는 CORS 를 통과한 출처만 읽을 수 있고, 쿠키로 온 refresh 의 본문에는 리프레시 토큰이 없다(1-1절).
- **CORS**: `allowCredentials=true` 와 `*` 패턴 조합은 기동 시 거부한다(`SecurityConfig.requireExplicitOrigins`).
- **로그**: 토큰 원문·해시를 로그에 남기지 않는다. 재사용 감지 시 userId·familyId 만 WARN.
- **쿠키**: Domain 속성 없음 → re-view.kr 호스트 전용. 리프레시 토큰은 URL 쿼리로 주지 않는다.

## 8. 알려진 한계·위험

- 액세스 토큰은 무효화하지 않는다. 로그아웃·비밀번호 재설정 뒤에도 이미 발급된 액세스 토큰은 만료(15분 목표)까지 유효하다.
- `User` 에 정지(suspended) 상태가 없어 "정지 사용자 refresh 거부" 는 사용자 삭제 여부로만 판단한다. 정지 상태가 생기면
  `RefreshTokenService.rotate` 에서 함께 막는다.
- 인메모리 저장소(로컬·테스트)는 패밀리 인덱스를 정리하지 않는다. 장기 실행용이 아니다.
- **유예 창 분기(#213 리뷰)**: 15초 유예 안에서는 같은 옛 값으로 새 값을 여러 개 받을 수 있다. 탈취자가 유예 안에 한 번
  분기를 얻어 계속 회전만 하면 정상 사용자와 **동시에 살아남을 수 있다** — "옛 값은 15초만 쓸 수 있다" 는 뜻이지,
  "탈취 세션이 15초 안에 끝난다" 는 뜻이 아니다. 재사용 탐지는 유예 밖 재사용에서만 보장된다.
  - 이번 변경으로 쿠키 값이 응답 본문으로 나오지 않게 되어(1-1절) 웹 XSS 로 원문을 얻는 경로는 막혔다.
  - 남은 대책 후보: 유예 축소(예: 5초), 소비된 값당 유예 재발급 1회 제한, 패밀리 최대 수명(절대 만료), 클라이언트 결합.
    여러 탭 동시 refresh 와 맞물려 있어 프론트 단일화 연동(3절)을 본 뒤 정한다.
- **인덱스 멤버 누적**: Redis 의 패밀리·사용자 Set 은 저장할 때마다 TTL 만 늘어나 계속 활동하는 사용자는 지난 해시·끝난
  패밀리 ID 가 쌓였다. 이제 저장(SAVE 스크립트) 때 가리키는 키가 사라진 멤버를 지워, 크기가 TTL(30분) 안의 회전·로그인
  수로 묶인다. 한 사용자의 동시 세션(패밀리) 수 상한은 아직 없다 — 30분 안에 로그인을 아주 많이 반복하면 그만큼 커진다.
- **OAuth 콜백 호스트**: prod 기본 redirect-uri 는 `https://re-view.kr/login/oauth2/code/{provider}` 라 302 의 쿠키가
  re-view.kr 호스트 쿠키가 되고 같은 호스트의 refresh 에 실린다. `OAUTH2_CALLBACK_BASE_URL` 을 api.re-view.kr 로 바꾸면
  쿠키가 api 호스트에 심겨 웹(re-view.kr 프록시) refresh 에 실리지 않는다 → OAuth 사용자만 refresh 가 항상 401.
  콜백 기준 주소는 웹 출처(re-view.kr)로 유지하고, www 와 apex 를 섞지 않는다(Domain 없는 호스트 전용 쿠키).
