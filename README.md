# MBN Knews Backend

MBN 연예 RSS를 수집해 MySQL에 저장하고 기사 조회 API를 제공하는 Spring Boot 백엔드입니다. 이 디렉터리가 IntelliJ와 Git의 단일 프로젝트 루트입니다.

## 요구사항

- Java 21 이상
- Docker

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

애플리케이션은 `http://localhost:8080`에서 실행됩니다. 시작 후 10초 뒤 첫 RSS 수집을 실행하고 이후 5분마다 반복합니다.

## Swagger UI

애플리케이션 실행 후 다음 주소에서 API를 직접 조회하고 실행할 수 있습니다.

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI 원본 명세:

```text
http://localhost:8080/v3/api-docs
```

## API

```text
GET  /actuator/health
GET  /api/v1/news?page=0&size=20
GET  /api/v1/news?keyword=BTS&page=0&size=20
GET  /api/v1/news/{articleId}
POST /api/v1/admin/rss/collect
POST /api/v1/admin/naver/backfill?keyword=김채원&maxPages=10
```

수동 RSS 수집:

```bash
curl -X POST http://localhost:8080/api/v1/admin/rss/collect
```

## 환경변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/kculture?...` | MySQL JDBC URL |
| `DB_USERNAME` | `kculture` | DB 사용자 |
| `DB_PASSWORD` | `local-password` | DB 비밀번호 |
| `RSS_ENABLED` | `true` | 예약 수집 활성화 |
| `RSS_INTERVAL_MS` | `300000` | 수집 간격 |
| `RSS_INITIAL_DELAY_MS` | `10000` | 시작 후 첫 수집 대기시간 |
| `NAVER_CLIENT_ID` | 없음 | NAVER API HUB Client ID |
| `NAVER_CLIENT_SECRET` | 없음 | NAVER API HUB Client Secret |
| `ADMIN_API_KEY` | 없음 | 관리자 API의 `X-Admin-Key` 값. 빈 값이면 로컬에서는 비활성화 |

네이버 보충 수집을 사용하려면 IntelliJ 실행 구성이나 터미널 환경변수에 키를 설정합니다. 키는 YAML이나 Git에 저장하지 않습니다.

```bash
export NAVER_CLIENT_ID="발급받은 Client ID"
export NAVER_CLIENT_SECRET="발급받은 Client Secret"
./gradlew bootRun
```

스키마는 Hibernate 자동 생성이 아니라 Flyway의 `src/main/resources/db/migration`에서 관리합니다.

## AWS Lightsail 배포

AWS 운영 배포용 파일은 다음과 같습니다.

- `Dockerfile`: Java 21 멀티 스테이지 빌드
- `docker-compose.prod.yml`: Spring Boot + MySQL 운영 구성
- `.env.production.example`: 운영 비밀값 템플릿
- `docs/aws-lightsail-deploy.md`: Lightsail 생성부터 업데이트·백업까지의 절차

상세 절차는 [AWS Lightsail 배포 가이드](docs/aws-lightsail-deploy.md)를 따릅니다.
