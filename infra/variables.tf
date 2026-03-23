variable "prefix" {
  description = "Prefix for all resources"
  default     = "matchmyduo"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "mysql_password" {
  description = "MySQL root password"
  sensitive   = true
}

variable "redis_password" {
  description = "Redis password"
  sensitive   = true
}

variable "github_access_token_1" {
  description = "GitHub PAT for GHCR login"
  sensitive   = true
}

variable "github_access_token_1_owner" {
  description = "GitHub username for GHCR"
}