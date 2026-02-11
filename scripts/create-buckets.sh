#!/bin/bash

set -e

ENDPOINT="http://localhost:4566"
REGION="us-east-1"

echo "Creating S3 buckets in LocalStack..."

aws --endpoint-url=$ENDPOINT s3 mb s3://crypto-data-raw --region $REGION
aws --endpoint-url=$ENDPOINT s3 mb s3://crypto-data-processed --region $REGION
aws --endpoint-url=$ENDPOINT s3 mb s3://crypto-data-curated --region $REGION

echo ""
echo "Buckets created:"
aws --endpoint-url=$ENDPOINT s3 ls

echo ""
echo "✓ Setup complete!"
