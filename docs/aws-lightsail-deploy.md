# AWS Lightsail 배포 가이드

이 문서는 Ubuntu Lightsail 인스턴스 한 대에 Spring Boot와 MySQL을 Docker Compose로 실행하는 해커톤용 구성을 기준으로 한다.

## 1. Lightsail 인스턴스 생성

1. AWS Lightsail에서 `Linux/Unix` 플랫폼과 `Ubuntu 24.04 LTS` 블루프린트를 선택한다.
2. 서울 리전과 메모리 2GB 이상 플랜을 선택한다.
3. 정적 IP를 생성해 인스턴스에 연결한다.
4. 네트워킹 방화벽에서 `TCP 80` 포트를 열고, SSH `22` 포트는 가능하면 내 IP로 제한한다.
5. MySQL `3306` 포트는 외부에 열지 않는다.

## 2. Docker 준비

Lightsail 브라우저 SSH 또는 로컬 SSH로 접속한 후 실행한다.

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

SSH 연결을 끊고 다시 접속한 뒤 확인한다.

```bash
docker --version
docker compose version
```

## 3. 코드와 비밀값 설정

```bash
git clone <GitHub 저장소 URL> mbn-knews
cd mbn-knews
cp .env.production.example .env.production
chmod 600 .env.production
nano .env.production
```

다음 값은 반드시 예시값을 교체한다.

```text
MYSQL_PASSWORD=긴_무작위_비밀번호
MYSQL_ROOT_PASSWORD=다른_긴_무작위_비밀번호
NAVER_CLIENT_ID=실제_API_KEY_ID
NAVER_CLIENT_SECRET=실제_API_KEY
ADMIN_API_KEY=긴_무작위_관리자_키
```

무작위 값은 다음 명령으로 만들 수 있다.

```bash
openssl rand -hex 32
```

`.env.production`은 Git에 커밋하지 않는다.

## 4. 실행

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

초기 빌드와 MySQL 시작에는 몇 분이 걸릴 수 있다. 로그는 다음으로 확인한다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml logs -f app
```

서버 안에서 헬스체크를 확인한다.

```bash
curl http://localhost/actuator/health
```

로컬 PC 브라우저에서 다음 주소를 연다.

```text
http://<Lightsail 정적 IP>/swagger-ui.html
http://<Lightsail 정적 IP>/api/v1/news
```

## 5. Swagger 관리자 API

운영 환경에서 `/api/v1/admin/**`는 `X-Admin-Key` 헤더를 요구한다.

1. Swagger 상단의 `Authorize`를 누른다.
2. `.env.production`의 `ADMIN_API_KEY` 값을 입력한다.
3. RSS 수동 수집이나 네이버 backfill을 실행한다.

터미널에서는 다음과 같이 호출한다.

```bash
curl -X POST \
  -H 'X-Admin-Key: <ADMIN_API_KEY>' \
  http://<Lightsail 정적 IP>/api/v1/admin/rss/collect
```

## 6. 코드 업데이트

```bash
git pull --ff-only
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

## 7. MySQL 백업

서버의 홈 디렉터리 등 안전한 경로에 백업한다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T mysql \
  sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  > kculture-backup.sql
```

백업 파일은 Lightsail 스냅샷만 믿지 말고 로컬 PC나 별도 스토리지에도 복사한다.

### 로컬 DB의 기존 기사 옮기기

로컬 Mac에서 현재 DB를 내보낸다.

```bash
docker exec mbn-knews-mysql mysqldump \
  --default-character-set=utf8mb4 \
  -ukculture -plocal-password kculture \
  > kculture-local.sql
```

`scp`로 `kculture-local.sql`을 Lightsail의 프로젝트 디렉터리에 복사한 후 가져온다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml stop app
docker compose --env-file .env.production -f docker-compose.prod.yml exec -T mysql \
  sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < kculture-local.sql
docker compose --env-file .env.production -f docker-compose.prod.yml up -d app
```

전체 dump import는 서버 DB의 동일한 테이블을 로컬 데이터로 교체할 수 있으므로, 초기 배포 단계에서만 실행한다.

## 8. HTTPS

현재 구성은 먼저 정적 IP와 HTTP로 배포를 확인하는 단계다. 도메인을 연결한 뒤 Caddy, Nginx + Let's Encrypt, 또는 Lightsail 로드 밸런서로 HTTPS를 적용한다. HTTPS 적용 전에는 공개 네트워크에서 관리자 API 키를 입력하지 않는다.
