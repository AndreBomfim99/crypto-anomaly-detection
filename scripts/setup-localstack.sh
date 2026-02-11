#!/bin/bash

set -e

echo "=================================="
echo "LocalStack Setup Script"
echo "=================================="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configurações
LOCALSTACK_ENDPOINT="http://localhost:4566"
AWS_REGION="us-east-1"

# Buckets S3
BUCKET_RAW="crypto-data-raw"
BUCKET_PROCESSED="crypto-data-processed"
BUCKET_CURATED="crypto-data-curated"

# SNS Topic
SNS_TOPIC="crypto-anomaly-alerts"

echo ""
echo -e "${YELLOW}Waiting for LocalStack to be ready...${NC}"
sleep 10

# Verificar se LocalStack está rodando
until curl -s "${LOCALSTACK_ENDPOINT}/_localstack/health" > /dev/null; do
  echo -e "${YELLOW}Waiting for LocalStack...${NC}"
  sleep 2
done

echo -e "${GREEN}✓ LocalStack is ready!${NC}"
echo ""

# Criar buckets S3
echo -e "${YELLOW}Creating S3 buckets...${NC}"

for bucket in $BUCKET_RAW $BUCKET_PROCESSED $BUCKET_CURATED; do
  if aws --endpoint-url="${LOCALSTACK_ENDPOINT}" s3 mb "s3://${bucket}" --region "${AWS_REGION}" 2>/dev/null; then
    echo -e "${GREEN}✓ Created bucket: ${bucket}${NC}"
  else
    echo -e "${YELLOW}○ Bucket already exists: ${bucket}${NC}"
  fi
done

echo ""

# Listar buckets
echo -e "${YELLOW}S3 Buckets:${NC}"
aws --endpoint-url="${LOCALSTACK_ENDPOINT}" s3 ls

echo ""

# Criar SNS Topic
echo -e "${YELLOW}Creating SNS topic...${NC}"
SNS_TOPIC_ARN=$(aws --endpoint-url="${LOCALSTACK_ENDPOINT}" \
  sns create-topic \
  --name "${SNS_TOPIC}" \
  --region "${AWS_REGION}" \
  --output text \
  --query 'TopicArn' 2>/dev/null || echo "")

if [ -n "$SNS_TOPIC_ARN" ]; then
  echo -e "${GREEN}✓ Created SNS topic: ${SNS_TOPIC}${NC}"
  echo "  ARN: ${SNS_TOPIC_ARN}"
else
  echo -e "${YELLOW}○ SNS topic already exists or failed to create${NC}"
fi

echo ""

# Configurar lifecycle policy para bucket raw (opcional - limpar dados antigos)
echo -e "${YELLOW}Configuring S3 lifecycle policies...${NC}"
cat > /tmp/lifecycle-policy.json << 'POLICY'
{
  "Rules": [
    {
      "Id": "DeleteOldRawData",
      "Status": "Enabled",
      "Prefix": "",
      "Expiration": {
        "Days": 30
      }
    }
  ]
}
POLICY

aws --endpoint-url="${LOCALSTACK_ENDPOINT}" \
  s3api put-bucket-lifecycle-configuration \
  --bucket "${BUCKET_RAW}" \
  --lifecycle-configuration file:///tmp/lifecycle-policy.json \
  --region "${AWS_REGION}" 2>/dev/null && \
  echo -e "${GREEN}✓ Lifecycle policy configured for ${BUCKET_RAW}${NC}" || \
  echo -e "${YELLOW}○ Failed to set lifecycle policy${NC}"

echo ""
echo -e "${GREEN}=================================="
echo "LocalStack Setup Complete!"
echo "==================================${NC}"
echo ""
echo "Endpoints:"
echo "  S3:         ${LOCALSTACK_ENDPOINT}"
echo "  SNS:        ${LOCALSTACK_ENDPOINT}"
echo ""
echo "Buckets created:"
echo "  - ${BUCKET_RAW}"
echo "  - ${BUCKET_PROCESSED}"
echo "  - ${BUCKET_CURATED}"
echo ""
echo "SNS Topics:"
echo "  - ${SNS_TOPIC}"
echo ""
