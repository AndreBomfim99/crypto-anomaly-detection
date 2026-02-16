resource "aws_sns_topic" "anomaly_alerts" {
  name = "crypto-anomaly-alerts"
  
  tags = var.tags
}
