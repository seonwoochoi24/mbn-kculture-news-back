output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.app.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.app.name
}

output "alb_dns_name" {
  description = "Public API hostname"
  value       = aws_lb.app.dns_name
}

output "api_base_url" {
  description = "Public API base URL"
  value       = var.certificate_arn == "" ? "http://${aws_lb.app.dns_name}" : "https://${var.domain_name}"
}

output "rds_endpoint" {
  description = "Private RDS endpoint"
  value       = aws_db_instance.app.endpoint
}

output "app_secret_arn" {
  description = "Secrets Manager ARN used by ECS"
  value       = aws_secretsmanager_secret.app.arn
}
