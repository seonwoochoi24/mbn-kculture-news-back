variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Resource name prefix"
  type        = string
  default     = "mbn-knews"
}

variable "image_tag" {
  description = "ECR image tag used by the ECS task"
  type        = string
  default     = "latest"
}

variable "desired_count" {
  description = "Number of ECS tasks"
  type        = number
  default     = 1
}

variable "certificate_arn" {
  description = "Optional ACM certificate ARN for HTTPS"
  type        = string
  default     = ""
}

variable "domain_name" {
  description = "Optional custom domain covered by the ACM certificate"
  type        = string
  default     = ""
}

check "https_configuration" {
  assert {
    condition = (
      var.certificate_arn == "" && var.domain_name == ""
      ) || (
      var.certificate_arn != "" && var.domain_name != ""
    )
    error_message = "certificate_arn and domain_name must be set together."
  }
}

variable "high_availability" {
  description = "Use a NAT gateway per AZ and Multi-AZ RDS"
  type        = bool
  default     = false
}

variable "db_name" {
  description = "Initial MySQL database"
  type        = string
  default     = "kculture"
}

variable "db_username" {
  description = "RDS master username"
  type        = string
  default     = "kculture"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "backup_retention_days" {
  description = "RDS automated backup retention in days; Free Plan accounts may be limited to 1"
  type        = number
  default     = 1

  validation {
    condition     = var.backup_retention_days >= 0 && var.backup_retention_days <= 35
    error_message = "backup_retention_days must be between 0 and 35."
  }
}

variable "naver_client_id" {
  description = "NAVER API HUB API key ID"
  type        = string
  sensitive   = true
}

variable "naver_client_secret" {
  description = "NAVER API HUB API key"
  type        = string
  sensitive   = true
}

variable "admin_api_key" {
  description = "X-Admin-Key value for admin endpoints"
  type        = string
  sensitive   = true
}

variable "openai_api_key_secret_arn" {
  description = "ARN of a manually created Secrets Manager secret containing the OpenAI API key"
  type        = string

  validation {
    condition     = startswith(var.openai_api_key_secret_arn, "arn:aws:secretsmanager:")
    error_message = "openai_api_key_secret_arn must be an AWS Secrets Manager ARN."
  }
}
