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
로컬 프론트엔드 개발 서버는 `http://localhost:5173`부터 `http://localhost:5175`까지 CORS가 허용됩니다.

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
GET  /api/v1/news/{articleId}?language=en
GET  /api/v1/news/{articleId}/cultural-terms?language=en
POST /api/v1/admin/rss/collect
POST /api/v1/admin/naver/backfill?keyword=김채원&maxPages=10
POST /api/v1/admin/articles/{articleId}/translations?language=en
POST /api/v1/admin/articles/content/backfill?batchSize=10
POST /api/v1/admin/articles/initial-backfill?targetCount=100&batchSize=10
GET  /api/v1/admin/articles/initial-backfill/{jobId}
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
| `OPENAI_API_KEY` | 없음 | 기사 번역과 문화 용어 분석에 사용하는 OpenAI API 키 |
| `OPENAI_MODEL` | `gpt-4o-mini` | 번역 및 문화 용어 분석 모델 |

네이버 보충 수집을 사용하려면 IntelliJ 실행 구성이나 터미널 환경변수에 키를 설정합니다. 키는 YAML이나 Git에 저장하지 않습니다.

```bash
export NAVER_CLIENT_ID="발급받은 Client ID"
export NAVER_CLIENT_SECRET="발급받은 Client Secret"
./gradlew bootRun
```

OpenAI 번역을 사용하려면 API 키를 서버 환경변수로 설정합니다. 실제 키를 YAML이나 Git에 저장하지 않습니다.

```bash
export OPENAI_API_KEY="발급받은 OpenAI API 키"
./gradlew bootRun
```

기사 상세 조회의 `language`에는 `ko`, `en`, `ja`, `zh`를 사용할 수 있습니다. `ko`는 원문을 반환하고,
다른 언어는 최초 요청에서만 OpenAI 번역을 생성한 뒤 `article_localization` 테이블의 캐시를 반환합니다.

문화 용어 API의 `language`에는 `en`, `ja`, `zh`를 사용할 수 있습니다. 기사 원문에 실제 등장하는
`애교`, `막내` 같은 용어만 추출해 해당 언어로 설명하며, 최초 분석 결과는
`article_cultural_analysis`와 `article_cultural_term` 테이블에 저장됩니다. 용어가 없는 결과도 저장하므로
동일한 기사와 언어를 다시 요청할 때 OpenAI를 재호출하지 않습니다.

RSS와 네이버 보충 수집으로 새 기사를 저장할 때 MBN 원문 페이지에서 본문 전체, 대표 이미지, 기자명,
원문 발행시각을 함께 가져옵니다. API는 `description` 대신 `content`를 반환하며 `imageUrl`,
`journalistName`, `publishedAt`도 함께 제공합니다. 원문 본문을 추출하지 못한 기사는 저장하지 않습니다.

초기 원문 수집 API는 네이버 검색을 MBN 기사 URL 발견 용도로만 사용합니다. 작업 상태를 DB에 저장하고
5초마다 5~10개의 검색 결과를 처리하며, 원문 확보가 완료된 기사 수가 최대 100개에 도달하면 종료합니다.

스키마는 Hibernate 자동 생성이 아니라 Flyway의 `src/main/resources/db/migration`에서 관리합니다.

## AWS 배포

운영 배포는 ECR, ECS Fargate, Application Load Balancer, RDS MySQL, Secrets Manager, CloudWatch를 사용하고 Terraform으로 관리합니다.

- `Dockerfile`: Java 21 멀티 스테이지 빌드
- `infra/terraform`: VPC, ALB, ECS, RDS, ECR, Secrets Manager 인프라
- `scripts/aws-push-image.sh`: Docker 이미지 ECR push와 ECS 재배포
- `docs/aws-ecs-deploy.md`: 초기 인프라 생성부터 업데이트·정리까지의 절차

상세 절차는 [AWS ECS Fargate + RDS 배포 가이드](docs/aws-ecs-deploy.md)를 따릅니다.
