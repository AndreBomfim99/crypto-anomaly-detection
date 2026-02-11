#!/bin/bash

set -e

echo "=================================="
echo "Crypto Anomaly Detection Pipeline"
echo "=================================="
echo ""

# Verificar se LocalStack está rodando
if ! curl -s http://localhost:4566/_localstack/health > /dev/null; then
  echo "ERROR: LocalStack is not running!"
  echo "Start it with: cd docker && docker-compose up -d"
  exit 1
fi

echo "✓ LocalStack is running"
echo ""

# Executar ingestão
echo "Running data ingestion..."
cd /mnt/c/projects-git/03_crypto-anomaly-detection
sbt "runMain ingestion.DataIngestion"

echo ""
echo "✓ Pipeline completed!"
