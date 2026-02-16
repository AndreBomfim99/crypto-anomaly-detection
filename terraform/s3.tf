resource "aws_s3_bucket" "raw" {
  bucket = "crypto-data-raw"
  
  tags = merge(
    var.tags,
    {
      Name = "crypto-data-raw"
    }
  )
}

resource "aws_s3_bucket" "processed" {
  bucket = "crypto-data-processed"
  
  tags = merge(
    var.tags,
    {
      Name = "crypto-data-processed"
    }
  )
}

resource "aws_s3_bucket" "curated" {
  bucket = "crypto-data-curated"
  
  tags = merge(
    var.tags,
    {
      Name = "crypto-data-curated"
    }
  )
}
