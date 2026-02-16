variable "project_name" {
  description = "Nome do projeto"
  type        = string
  default     = "crypto-anomaly-detection"
}

variable "environment" {
  description = "Ambiente (dev, prod)"
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "Região AWS"
  type        = string
  default     = "us-east-1"
}

variable "localstack_endpoint" {
  description = "Endpoint do LocalStack"
  type        = string
  default     = "http://127.0.0.1:4566"
}

variable "crypto_ids" {
  description = "Lista de criptomoedas monitoradas"
  type        = list(string)
  default     = ["bitcoin", "ethereum", "cardano", "binancecoin", "solana"]
}

variable "s3_lifecycle_days" {
  description = "Dias para expiração de dados raw no S3"
  type        = number
  default     = 30
}

variable "tags" {
  description = "Tags padrão para recursos"
  type        = map(string)
  default = {
    Project     = "crypto-anomaly-detection"
    ManagedBy   = "terraform"
    Environment = "dev"
  }
}
