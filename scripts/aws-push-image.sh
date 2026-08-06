#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
TERRAFORM_DIR="${PROJECT_DIR}/infra/terraform"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

ECR_URL="$(terraform -chdir="${TERRAFORM_DIR}" output -raw ecr_repository_url)"
ECR_REGISTRY="${ECR_URL%%/*}"

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

docker build --platform linux/amd64 -t "${ECR_URL}:${IMAGE_TAG}" "${PROJECT_DIR}"
docker push "${ECR_URL}:${IMAGE_TAG}"

if terraform -chdir="${TERRAFORM_DIR}" output -raw ecs_cluster_name >/dev/null 2>&1; then
  ECS_CLUSTER="$(terraform -chdir="${TERRAFORM_DIR}" output -raw ecs_cluster_name)"
  ECS_SERVICE="$(terraform -chdir="${TERRAFORM_DIR}" output -raw ecs_service_name)"
  aws ecs update-service \
    --region "${AWS_REGION}" \
    --cluster "${ECS_CLUSTER}" \
    --service "${ECS_SERVICE}" \
    --force-new-deployment >/dev/null
  echo "ECS deployment requested: ${ECS_CLUSTER}/${ECS_SERVICE}"
fi

echo "Pushed image: ${ECR_URL}:${IMAGE_TAG}"
