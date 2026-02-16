#!/bin/bash

set -e

echo "=================================="
echo "Crypto Anomaly Detection Pipeline"
echo "Running with Docker Compose"
echo "=================================="

cd "$(dirname "$0")/../docker"

# Parar containers existentes
echo ""
echo "Stopping existing containers..."
docker-compose down

# Iniciar LocalStack
echo ""
echo "Starting LocalStack..."
docker-compose up -d localstack

# Aguardar LocalStack ficar healthy
echo "Waiting for LocalStack to be healthy..."
sleep 15

# Criar buckets (se não existirem)
echo ""
echo "Setting up S3 buckets..."
cd ..
./scripts/setup-localstack.sh

# Voltar para docker
cd docker

# Executar ingestão
echo ""
echo "Running data ingestion..."
docker-compose up ingestion

# Executar processamento
echo ""
echo "Running data processing..."
docker-compose up processor

echo ""
echo "=================================="
echo "Pipeline Completed!"
echo "=================================="
echo ""
echo "Check logs:"
echo "  docker-compose logs ingestion"
echo "  docker-compose logs processor"
echo ""
echo "Check S3 data:"
echo "  aws --endpoint-url=http://localhost:4566 s3 ls s3://crypto-data-raw/ --recursive"
echo "  aws --endpoint-url=http://localhost:4566 s3 ls s3://crypto-data-processed/ --recursive"
