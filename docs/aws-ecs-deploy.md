# AWS ECS Fargate + RDS 배포 가이드

이 문서는 MBN Knews 백엔드를 서울 리전의 AWS 관리형 인프라에 배포하는 절차다.

## 아키텍처

```text
Internet
   |
Application Load Balancer (public subnet, 2 AZ)
   |
ECS Fargate (private app subnet, 2 AZ)
   |-- NAT Gateway --> MBN RSS / NAVER API HUB / ECR
   |
RDS MySQL (private DB subnet, 2 AZ)

ECR             : Docker 이미지
Secrets Manager : DB 비밀번호, NAVER 키, 관리자 키
CloudWatch Logs : Spring Boot 로그
Terraform       : 전체 인프라 정의
```

기본값은 비용을 줄이기 위해 NAT Gateway 1개, ECS Task 1개, Single-AZ RDS를 사용한다. `high_availability = true`로 변경하면 NAT Gateway 2개와 Multi-AZ RDS를 사용한다.

AWS Free Plan 계정과 호환되도록 RDS 자동 백업 보관기간은 기본 1일이다. 유료 계정으로 전환한 후 `backup_retention_days = 7` 등으로 늘릴 수 있다.

## 1. 사전 준비

로컬 PC에 다음 도구가 필요하다.

- AWS CLI v2
- Terraform 1.7 이상
- Docker Desktop

AWS root 계정 대신 배포용 IAM 사용자 또는 IAM Identity Center 권한을 사용한다. 로컬 AWS CLI 인증을 준비한 후 확인한다.

```bash
aws sts get-caller-identity
```

첫 적용 전에 AWS Budgets에서 월 예산과 알림을 생성한다. NAT Gateway, ALB, Fargate, RDS, Secrets Manager에서 각각 비용이 발생한다.

## 2. Terraform 비밀값 준비

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars
```

다음 값을 실제 값으로 교체한다.

```hcl
naver_client_id     = "실제 NAVER API KEY ID"
naver_client_secret = "실제 NAVER API KEY"
admin_api_key       = "긴 무작위 관리자 키"
openai_api_key_secret_arn = "OpenAI API 키를 저장한 Secrets Manager ARN"
```

OpenAI API 키는 `terraform.tfvars`와 Terraform state에 남기지 않는다. AWS 콘솔의
Secrets Manager에서 `mbn-knews/openai-api-key` 같은 이름의 **일반 텍스트 비밀값**을 별도로 만들고,
그 ARN만 `openai_api_key_secret_arn`에 입력한다.

관리자 키는 다음과 같이 생성할 수 있다.

```bash
openssl rand -hex 32
```

`terraform.tfvars`와 `terraform.tfstate`에는 비밀값이 포함되므로 Git에 커밋하지 않는다. 팀 운영에서는 `backend.tf.example`을 기준으로 별도 S3 state bucket을 만들어 remote backend로 전환한다.

## 3. ECR 먼저 생성

ECS가 시작되기 전 Docker 이미지가 ECR에 있어야 하므로 ECR을 먼저 생성한다.

```bash
terraform init
terraform fmt -check
terraform validate
terraform apply \
  -target=aws_ecr_repository.app \
  -target=aws_ecr_lifecycle_policy.app
```

## 4. 첫 Docker 이미지 push

프로젝트 루트에서 실행한다. Apple Silicon Mac에서도 Fargate X86_64용 이미지를 생성한다.

```bash
cd ../..
./scripts/aws-push-image.sh
```

## 5. 전체 인프라 생성

```bash
cd infra/terraform
terraform plan -out=tfplan
terraform apply tfplan
```

RDS와 NAT Gateway 생성에 시간이 걸릴 수 있다. 완료 후 URL을 확인한다.

```bash
terraform output -raw api_base_url
```

다음 경로를 확인한다.

```text
<api_base_url>/actuator/health
<api_base_url>/swagger-ui.html
<api_base_url>/api/v1/news
```

`/actuator/health`가 `UP`이면 ECS, ALB, RDS 연결이 정상이다. Flyway가 RDS에 테이블을 자동 생성한다.

## 6. Swagger 관리자 API

`/api/v1/admin/**`는 `X-Admin-Key` 헤더를 요구한다.

1. Swagger 상단의 `Authorize`를 누른다.
2. `terraform.tfvars`의 `admin_api_key` 값을 입력한다.
3. RSS 수집이나 NAVER backfill을 호출한다.

HTTPS 적용 전에는 공개 네트워크에서 관리자 API를 호출하지 않는다.

## 7. HTTPS와 도메인

1. Route 53 또는 도메인 DNS에서 API 도메인을 준비한다.
2. 서울 리전 ACM에서 해당 도메인 인증서를 발급하고 DNS 검증을 완료한다.
3. `terraform.tfvars`에 다음을 설정한다.

```hcl
domain_name     = "api.example.com"
certificate_arn = "arn:aws:acm:ap-northeast-2:...:certificate/..."
```

4. `terraform apply`를 실행한다.
5. DNS의 `api.example.com`을 ALB로 Alias 또는 CNAME 연결한다.

설정 후 HTTP 80은 HTTPS 443으로 리다이렉트된다.

## 8. 코드 업데이트

코드를 수정하고 테스트한 후 프로젝트 루트에서 실행한다.

```bash
./gradlew test
./scripts/aws-push-image.sh
```

스크립트가 ECR에 `latest` 이미지를 push하고 ECS Service에 force new deployment를 요청한다.

## 9. 로그와 상태

```bash
aws logs tail /ecs/mbn-knews --region ap-northeast-2 --follow

aws ecs describe-services \
  --region ap-northeast-2 \
  --cluster mbn-knews \
  --services mbn-knews
```

## 10. 비용 중지

해커톤 종료 후 비용을 멈추려면 Terraform 상태 파일을 보관한 상태에서 다음을 실행한다.

```bash
cd infra/terraform
terraform plan -destroy
terraform destroy
```

`terraform destroy`는 RDS, VPC, ALB 등 전체 운영 자원을 삭제하므로 필요한 DB 백업을 먼저 만든다.
