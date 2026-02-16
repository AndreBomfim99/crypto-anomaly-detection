output "s3_buckets" {
  value = {
    raw       = aws_s3_bucket.raw.id
    processed = aws_s3_bucket.processed.id
    curated   = aws_s3_bucket.curated.id
  }
}

output "sns_topic_arn" {
  value = aws_sns_topic.anomaly_alerts.arn
}
