# AWS EC2 배포 가이드 (처음부터 끝까지)

> 대상: FireView(review-backend) — Spring Boot 3.4.5 / Java 17 / Gradle / Docker
> 결과물: `https://api.도메인` 으로 접속되는 운영 서버 + main 머지 시 자동 배포

---

## 0. 최종 구성도

```
[사용자]
   │ HTTPS(443)
   ▼
[EC2] nginx ──proxy──► Docker 컨테이너 fireview (8080)
   │                        │
   │                        ├──► RDS PostgreSQL (5432)
   │                        └──► Redis (EC2 호스트, 6379)
   ▲
   └── GitHub Actions (main 머지 → SSH 접속 → docker build & run)
```

핵심 포인트 3가지:

1. **애플리케이션은 Docker 컨테이너로 뜬다.** `Dockerfile`이 멀티스테이지(빌드+실행)라 EC2에 Java/Gradle을 따로 설치할 필요가 없다.
2. **모든 비밀값은 EC2의 `~/review-backend/.env.prod` 파일에 있다.** 코드에는 `${DB_PASSWORD}` 같은 자리표시자만 있다 (`application-prod.properties`).
3. **배포는 GitHub Actions가 SSH로 접속해서 수행한다** (`.github/workflows/deploy.yml`).

---

## 1. 사전 준비물

| 항목 | 설명 |
|------|------|
| AWS 계정 | 결제 수단 등록 필요 (프리티어여도 카드 등록은 해야 함) |
| 도메인 | HTTPS를 쓰려면 필수. 가비아/Route53 등 아무거나 |
| GitHub 저장소 권한 | Settings → Secrets 등록 권한 |
| OAuth2 키 | 구글/네이버 개발자 콘솔 client-id, client-secret |

리전은 **아시아 태평양(서울) `ap-northeast-2`** 로 통일한다. EC2와 RDS가 다른 리전에 있으면 서로 사설 IP로 통신할 수 없다.

---

## 2. 보안 그룹 먼저 만들기

인스턴스를 만들기 전에 보안 그룹(방화벽)을 먼저 만들어 두면 나중에 꼬이지 않는다.

EC2 콘솔 → 좌측 **네트워크 및 보안 → 보안 그룹 → 보안 그룹 생성**

### 2-1. `fireview-backend-sg` (EC2용)

인바운드 규칙:

| 유형 | 포트 | 소스 | 설명 |
|------|------|------|------|
| SSH | 22 | **내 IP** | 관리자 접속. `0.0.0.0/0`은 피할 것 |
| HTTP | 80 | 0.0.0.0/0 | Let's Encrypt 인증 + HTTPS 리다이렉트 |
| HTTPS | 443 | 0.0.0.0/0 | 실제 서비스 트래픽 |

> 8080은 **열지 않는다.** 외부에는 nginx(443)만 노출하고 8080은 서버 내부에서만 접근한다.
> 단, nginx/도메인 설정 전에 동작 확인만 하고 싶다면 임시로 8080을 내 IP에만 열고 확인 후 삭제한다.

아웃바운드는 기본값(전체 허용) 그대로 둔다. GitHub, 네이버 API, RDS 호출에 필요하다.

### 2-2. `fireview-rds-sg` (RDS용)

| 유형 | 포트 | 소스 |
|------|------|------|
| PostgreSQL | 5432 | **`fireview-backend-sg` 선택** |

소스에 IP가 아니라 위에서 만든 보안 그룹을 지정하는 게 핵심이다. EC2에서만 DB에 붙을 수 있게 된다.

---

## 3. 키 페어 생성

EC2 콘솔 → **네트워크 및 보안 → 키 페어 → 키 페어 생성**

- 이름: `fireview-key`
- 유형: **RSA**
- 형식: **.pem** (macOS/Linux 기준. Windows PuTTY를 쓸 때만 .ppk)

생성하면 `fireview-key.pem` 이 자동 다운로드된다. **이 파일은 재발급이 불가능하다.** 잃어버리면 인스턴스에 접속할 방법이 사라진다.

```bash
mkdir -p ~/.ssh
mv ~/Downloads/fireview-key.pem ~/.ssh/
chmod 400 ~/.ssh/fireview-key.pem
```

`chmod 400`을 안 하면 SSH가 "UNPROTECTED PRIVATE KEY FILE" 오류로 접속을 거부한다.

---

## 4. EC2 인스턴스 생성

EC2 콘솔 → **인스턴스 → 인스턴스 시작**

| 설정 | 값 | 이유 |
|------|-----|------|
| 이름 | `fireview-backend` | |
| AMI | **Amazon Linux 2023** (64비트 x86) | dnf 패키지가 정리돼 있고 Docker 설치가 쉬움 |
| 인스턴스 유형 | **t3.small** (2GB RAM) 권장 / 최소 t3.micro | ⚠️ 아래 설명 참고 |
| 키 페어 | `fireview-key` | |
| 네트워크 | 기본 VPC, 퍼블릭 IP 자동 할당 **활성화** | |
| 보안 그룹 | **기존 보안 그룹 선택 → `fireview-backend-sg`** | |
| 스토리지 | **30GiB gp3** | 프리티어 한도가 30GB. Docker 이미지가 금방 쌓인다 |

### ⚠️ 인스턴스 유형에 대한 경고

이 프로젝트의 `Dockerfile`은 **EC2 안에서 Gradle 빌드를 수행**한다:

```dockerfile
FROM gradle:8.5-jdk17 AS builder
RUN ./gradlew bootJar -x test --no-daemon
```

t2.micro / t3.micro(1GB RAM)에서는 Gradle 데몬이 메모리 부족으로 죽으면서 빌드가 실패하거나 몇 십 분씩 걸린다. 선택지는 둘 중 하나다:

- **t3.small(2GB) 사용** — 가장 단순. 월 약 $15
- **t3.micro + 스왑 2GB** — 프리티어 유지. 느리지만 빌드는 통과 (스왑 설정은 6-2에)

---

## 5. 탄력적 IP(Elastic IP) 할당

EC2를 재시작하면 퍼블릭 IP가 **바뀐다.** 도메인 DNS와 GitHub Secrets에 IP를 박아두는 구조라 고정 IP가 필수다.

EC2 콘솔 → **네트워크 및 보안 → 탄력적 IP → 탄력적 IP 주소 할당** → 생성된 IP 선택 → **작업 → 탄력적 IP 주소 연결** → 인스턴스 `fireview-backend` 선택 → 연결

> 탄력적 IP는 **인스턴스에 연결돼 있으면 무료**, 할당만 해두고 놀리면 시간당 과금된다. 인스턴스를 종료할 땐 탄력적 IP도 반드시 릴리스할 것.

---

## 6. 서버 초기 세팅

### 6-1. 접속

```bash
ssh -i ~/.ssh/fireview-key.pem ec2-user@탄력적IP
```

매번 치기 귀찮으면 `~/.ssh/config`에 등록:

```
Host fireview
    HostName 탄력적IP
    User ec2-user
    IdentityFile ~/.ssh/fireview-key.pem
```

이후엔 `ssh fireview` 로 접속된다.

### 6-2. 스왑 메모리 (t3.micro면 필수, t3.small도 권장)

```bash
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h   # Swap 2.0Gi 확인
```

`/etc/fstab` 등록까지 해야 재부팅 후에도 유지된다.

### 6-3. 패키지 설치

```bash
sudo dnf update -y
sudo dnf install -y docker git nginx redis6

# Docker 시작 + 부팅 시 자동 시작
sudo systemctl enable --now docker

# ec2-user가 sudo 없이 docker를 쓰도록
sudo usermod -aG docker ec2-user
```

**마지막 줄 실행 후 반드시 로그아웃 후 재접속**해야 그룹 권한이 적용된다.

```bash
exit
ssh fireview
docker ps   # 권한 오류 없이 빈 목록이 나오면 성공
```

### 6-4. Redis 설정

컨테이너 안에서 `localhost:6379`는 **컨테이너 자신**을 가리키므로 호스트 Redis에 닿지 않는다. Docker 브리지 게이트웨이 IP(`172.17.0.1`)로 붙게 설정한다.

```bash
sudo vi /etc/redis6/redis6.conf
```

두 줄을 찾아 수정:

```
bind 127.0.0.1 172.17.0.1
protected-mode no
```

> `172.17.0.1`은 Docker 기본 브리지 게이트웨이다. `ip addr show docker0` 로 실제 값을 확인할 수 있다.
> `bind`를 `0.0.0.0`으로 열지 말 것. 보안 그룹에 6379가 없어도 같은 VPC 내부에는 노출된다.

```bash
sudo systemctl enable --now redis6
redis6-cli ping   # PONG
```

---

## 7. RDS PostgreSQL 생성

RDS 콘솔 → **데이터베이스 생성**

| 설정 | 값 |
|------|-----|
| 생성 방식 | 표준 생성 |
| 엔진 | **PostgreSQL** (16.x) |
| 템플릿 | 프리 티어 (또는 개발/테스트) |
| DB 인스턴스 식별자 | `fireview-db` |
| 마스터 사용자 이름 | `fireview1` |
| 마스터 암호 | 직접 생성한 강한 암호 |
| 인스턴스 클래스 | db.t4g.micro |
| 스토리지 | 20GiB gp3, **스토리지 자동 조정 비활성화** (과금 폭탄 방지) |
| **퍼블릭 액세스** | **아니요** |
| VPC 보안 그룹 | **기존 항목 선택 → `fireview-rds-sg`** |
| 가용 영역 | EC2와 같은 리전 |
| 추가 구성 → 초기 데이터베이스 이름 | `fireview` |
| 자동 백업 | 7일 (기본값 유지) |

생성에 5~10분 걸린다. 완료되면 **엔드포인트**를 복사해 둔다:
`fireview-db.xxxxxxxx.ap-northeast-2.rds.amazonaws.com`

### 연결 확인

EC2에서:

```bash
sudo dnf install -y postgresql15
psql -h <RDS엔드포인트> -U fireview1 -d fireview
```

암호를 넣고 `fireview=>` 프롬프트가 뜨면 성공. 여기서 막히면 100% 보안 그룹 문제다 (rds-sg의 소스가 backend-sg인지 확인).

> 테이블은 직접 만들 필요 없다. `application-prod.properties`의 `spring.jpa.hibernate.ddl-auto=update`가 첫 기동 때 엔티티 기준으로 생성한다.

---

## 8. 코드 배포 준비

### 8-1. 저장소 클론

`deploy.yml`이 `cd ~/review-backend` 를 전제로 하므로 **경로가 정확히 이것이어야 한다.**

```bash
cd ~
git clone https://github.com/DMU-FireView/review-backend.git
cd review-backend
```

비공개 저장소라면 HTTPS 대신 배포 키(deploy key)를 쓰거나, GitHub Personal Access Token으로 클론한다.

### 8-2. `.env.prod` 작성

이 파일이 배포의 심장이다. `application-prod.properties`가 참조하는 모든 환경변수를 담는다.

먼저 JWT 시크릿을 생성한다:

```bash
openssl rand -base64 48
```

그리고 파일 작성:

```bash
vi ~/review-backend/.env.prod
```

```bash
# ── Database (RDS) ──────────────────────────────
DB_URL=jdbc:postgresql://fireview-db.xxxxxxxx.ap-northeast-2.rds.amazonaws.com:5432/fireview
DB_USERNAME=fireview1
DB_PASSWORD=여기에_RDS_마스터_암호

# ── Redis (EC2 호스트) ───────────────────────────
REDIS_HOST=172.17.0.1

# ── JWT ─────────────────────────────────────────
JWT_SECRET=위에서_openssl로_생성한_값

# ── OAuth2 ──────────────────────────────────────
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=xxxx
NAVER_CLIENT_ID=xxxx
NAVER_CLIENT_SECRET=xxxx

# ── CORS / 리다이렉트 ────────────────────────────
CORS_ALLOWED_ORIGINS=https://www.beens.kr,https://beens.kr
OAUTH2_REDIRECT_URI=https://www.beens.kr/oauth2/callback
FRONTEND_URL=https://www.beens.kr

# ── 네이버 쇼핑 검색 API ──────────────────────────
# (OAuth2용 키와 별개. 미설정 시 로컬 DB 검색으로 fallback)

# ── AI 서버 ─────────────────────────────────────
AI_SERVER_URL=http://20.249.211.171:8000

# ── 메일(SMTP) ───────────────────────────────────
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=발신계정@gmail.com
MAIL_PASSWORD=앱_비밀번호
MAIL_FROM=noreply@beens.kr
```

권한을 잠근다:

```bash
chmod 600 ~/review-backend/.env.prod
```

> **주의:** 현재 `.gitignore`에는 `.env`만 있고 `.env.prod`는 없다. 로컬에서 같은 이름의 파일을 만들면 커밋될 수 있으니 `.gitignore`에 `.env.prod`를 추가해 두는 걸 권장한다.

`.env.prod`에서 값 하나라도 빠지면 Spring이 `Could not resolve placeholder 'XXX'` 로 기동에 실패한다. 필수 변수 목록:

`DB_URL` `DB_USERNAME` `DB_PASSWORD` `JWT_SECRET` `GOOGLE_CLIENT_ID` `GOOGLE_CLIENT_SECRET` `NAVER_CLIENT_ID` `NAVER_CLIENT_SECRET` `CORS_ALLOWED_ORIGINS` `OAUTH2_REDIRECT_URI` `MAIL_HOST` `MAIL_USERNAME` `MAIL_PASSWORD`

(`REDIS_HOST` `AI_SERVER_URL` `MAIL_PORT` `MAIL_FROM` `FRONTEND_URL`은 기본값이 있어 선택)

---

## 9. 첫 배포 (수동)

자동화를 붙이기 전에 손으로 한 번 성공시켜 두는 게 중요하다. 문제가 생겼을 때 원인이 앱인지 Actions인지 구분할 수 있다.

```bash
cd ~/review-backend

# 1) 이미지 빌드 (첫 빌드는 의존성 다운로드 때문에 5~15분)
docker build -t fireview:latest .

# 2) 컨테이너 실행
docker run -d \
  --name fireview \
  --restart unless-stopped \
  --env-file .env.prod \
  -p 8080:8080 \
  fireview:latest

# 3) 로그 확인
docker logs -f fireview
```

`Started FireViewApplication in xx.xxx seconds` 가 보이면 성공이다. `Ctrl+C`로 로그 보기를 빠져나온다(컨테이너는 계속 돈다).

### 헬스체크

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

`DOWN`이 나오면 응답 본문의 `components`에서 어느 것이 죽었는지 보인다 (`db` = RDS 연결 실패, `redis` = Redis 연결 실패).

---

## 10. nginx + 도메인 + HTTPS

### 10-1. DNS 레코드 추가

도메인 관리 콘솔(또는 Route 53)에서:

| 타입 | 이름 | 값 |
|------|------|-----|
| A | `api` | 탄력적 IP |

전파 확인:

```bash
dig +short api.도메인.kr
```

탄력적 IP가 나올 때까지 기다린다(보통 몇 분, 최대 수십 분). **이게 안 되면 다음 단계의 인증서 발급이 실패한다.**

### 10-2. nginx 설정

```bash
sudo vi /etc/nginx/conf.d/fireview.conf
```

```nginx
server {
    listen 80;
    server_name api.도메인.kr;

    client_max_body_size 20M;

    location / {
        proxy_pass http://127.0.0.1:8080;

        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host  $host;

        # WebSocket
        proxy_http_version 1.1;
        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_read_timeout 120s;
    }
}
```

`X-Forwarded-*` 헤더가 핵심이다. `application-prod.properties`의 `server.forward-headers-strategy=framework`가 이 헤더를 읽어서 OAuth2 `redirect_uri`를 `http://localhost:8080/...` 이 아니라 `https://api.도메인.kr/...` 로 생성한다. 이 헤더를 빠뜨리면 소셜 로그인이 깨진다.

```bash
sudo nginx -t              # syntax is ok 확인
sudo systemctl enable --now nginx
```

이제 `http://api.도메인.kr/actuator/health` 가 외부에서 열려야 한다.

### 10-3. HTTPS 인증서 (Let's Encrypt)

```bash
sudo dnf install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.도메인.kr
```

- 이메일 입력 → 약관 동의 → HTTP를 HTTPS로 **리다이렉트할지 물으면 "2" (리다이렉트)** 선택

certbot이 위 nginx 설정 파일을 자동으로 고쳐서 443 서버 블록과 인증서 경로를 넣어준다.

자동 갱신 확인 (인증서는 90일마다 만료된다):

```bash
sudo systemctl enable --now certbot-renew.timer
sudo certbot renew --dry-run
```

> `dnf`에 certbot 패키지가 없다면: `sudo python3 -m pip install certbot certbot-nginx`

이제 `https://api.도메인.kr/actuator/health` 로 접속되면 서버 구축 완료다.

---

## 11. GitHub Actions 자동 배포 연결

`.github/workflows/deploy.yml`은 이미 저장소에 있다. Secrets만 채우면 main 머지 시 자동 배포된다.

### 11-1. 배포 전용 SSH 키 만들기 (권장)

개인 `.pem`을 GitHub에 넣는 대신 배포 전용 키를 새로 만든다. 유출 시 이 키만 폐기하면 된다.

로컬에서:

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/fireview-deploy -N ""
```

공개키를 EC2에 등록:

```bash
ssh fireview "echo '$(cat ~/.ssh/fireview-deploy.pub)' >> ~/.ssh/authorized_keys"
```

동작 확인:

```bash
ssh -i ~/.ssh/fireview-deploy ec2-user@탄력적IP "echo ok"
```

### 11-2. GitHub Secrets 등록

저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| 이름 | 값 |
|------|-----|
| `EC2_HOST` | 탄력적 IP |
| `EC2_USERNAME` | `ec2-user` |
| `EC2_SSH_KEY` | `cat ~/.ssh/fireview-deploy` 의 **전체 내용** |

`EC2_SSH_KEY`는 `-----BEGIN OPENSSH PRIVATE KEY-----` 부터 `-----END OPENSSH PRIVATE KEY-----` 까지 **마지막 줄바꿈 포함** 전부 붙여넣어야 한다. 한 줄이라도 빠지면 `ssh: no key found` 오류가 난다.

### 11-3. 테스트

Actions 탭 → **CD - Deploy to EC2** → **Run workflow** (수동 실행)

초록 체크가 뜨고 `✅ 배포 완료` 로그가 보이면 끝이다. 이후부터는 main에 머지될 때마다 자동으로:
코드 pull → 이미지 빌드 → 기존 컨테이너 교체 → 미사용 이미지 정리 순으로 실행된다.

> 배포는 컨테이너를 stop 후 재시작하므로 **30초~1분 정도 서비스가 끊긴다.** 무중단이 필요해지면 그때 blue-green을 고민하면 된다.

---

## 12. OAuth2 콘솔에 리다이렉트 URI 등록

서버가 떠도 이걸 안 하면 소셜 로그인만 실패한다.

**Google Cloud Console** → API 및 서비스 → 사용자 인증 정보 → OAuth 2.0 클라이언트 ID → 승인된 리디렉션 URI:

```
https://api.도메인.kr/login/oauth2/code/google
```

**네이버 개발자센터** → 내 애플리케이션 → API 설정 → Callback URL:

```
https://api.도메인.kr/login/oauth2/code/naver
```

경로는 `application.properties`의 `redirect-uri={baseUrl}/login/oauth2/code/naver` 설정에서 나온 것이라 **한 글자도 달라선 안 된다.**

---

## 13. 운영 명령어

```bash
# 상태 확인
docker ps
curl -s localhost:8080/actuator/health

# 로그
docker logs --tail 100 fireview
docker logs -f fireview
docker logs --tail 200 fireview 2>&1 | grep -E "ERROR|Exception"

# 재시작 (코드 변경 없이)
docker restart fireview

# 수동 재배포
cd ~/review-backend && git pull && docker build -t fireview:latest . \
  && docker stop fireview && docker rm fireview \
  && docker run -d --name fireview --restart unless-stopped \
     --env-file .env.prod -p 8080:8080 fireview:latest

# 디스크 정리 (No space left on device 발생 시)
docker system df
docker system prune -af

# Redis
redis6-cli ping
redis6-cli keys "naver:product:*"

# nginx
sudo nginx -t && sudo systemctl reload nginx
sudo tail -f /var/log/nginx/error.log
```

---

## 14. 자주 막히는 지점

| 증상 | 원인 / 해결 |
|------|------------|
| `docker build`가 멈추거나 killed | 메모리 부족. 스왑 추가(6-2) 또는 t3.small로 업그레이드 |
| `Could not resolve placeholder 'DB_URL'` | `.env.prod`에 변수 누락. 8-2의 필수 목록 확인 |
| DB 연결 타임아웃 | rds-sg 인바운드 소스가 backend-sg인지 확인. RDS와 EC2가 같은 VPC인지 확인 |
| Redis 연결 거부 | `REDIS_HOST=localhost`로 두면 안 됨 → `172.17.0.1`. redis6.conf의 `bind` 확인 |
| OAuth2 로그인 후 localhost로 튕김 | nginx의 `X-Forwarded-*` 헤더 누락 (10-2) |
| certbot 실패 | DNS A레코드 전파 전이거나 80포트가 보안 그룹에서 막힘 |
| Actions `ssh: no key found` | `EC2_SSH_KEY`에 BEGIN/END 줄과 마지막 줄바꿈까지 포함했는지 확인 |
| Actions는 성공인데 사이트가 500 | `docker logs fireview` 확인. 대부분 환경변수/DB 문제 |
| `No space left on device` | `docker system prune -af` |

---

## 15. 예상 비용 (서울 리전, 월 기준)

| 항목 | 프리티어 | 프리티어 이후 |
|------|---------|-------------|
| EC2 t3.micro | 750시간 무료(12개월) | 약 $9 |
| EC2 t3.small | — | 약 $18 |
| EBS 30GB gp3 | 무료 | 약 $2.7 |
| RDS db.t4g.micro | 750시간 무료(12개월) | 약 $13 |
| RDS 스토리지 20GB | 무료 | 약 $2.6 |
| 탄력적 IP | 연결 시 무료 | 연결 시 무료 |
| 데이터 전송 | 100GB/월 무료 | 초과분 GB당 $0.126 |

**과금 사고 방지 3종:**

1. Billing 콘솔 → **결제 알림(Budgets)** 을 $10 정도로 설정
2. RDS **스토리지 자동 조정 비활성화**
3. 인스턴스 종료 시 **탄력적 IP 릴리스, EBS 볼륨 삭제, RDS 스냅샷 정리**까지 확인

---

## 부록. 빌드를 GitHub Actions로 옮기기 (선택)

EC2 사양이 낮아 빌드가 힘들면, Actions에서 jar를 빌드해 EC2로 전송하는 방식으로 바꿀 수 있다. EC2는 실행만 하므로 t3.micro로도 충분해지고 배포 시간도 짧아진다.

이 경우 `Dockerfile`은 실행 스테이지만 남기고(`FROM eclipse-temurin:17-jre-alpine` + `COPY build/libs/*.jar`), `deploy.yml`은 `gradle bootJar` → `appleboy/scp-action`으로 jar 전송 → SSH로 `docker build && docker run` 순서가 된다.

지금 구성으로도 t3.small이면 문제없이 돌아가므로, 배포가 느려서 불편해질 때 검토하면 된다.
