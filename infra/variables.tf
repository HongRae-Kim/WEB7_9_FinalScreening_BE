variable "prefix" {
  description = "Prefix for all resources"
  default     = "matchmyduo"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "mysql_password" {
  description = "MySQL root and application user password"
  type        = string
  sensitive   = true
}

variable "redis_password" {
  description = "Redis password"
  type        = string
  sensitive   = true
}

variable "github_access_token_1" {
  description = "GitHub Container Registry access token for EC2 image pulls"
  type        = string
  sensitive   = true
}

variable "github_access_token_1_owner" {
  description = "GitHub owner used for GHCR login on EC2"
  type        = string
}
