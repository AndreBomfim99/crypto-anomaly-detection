#!/bin/bash

set -e

echo "=================================="
echo "Building Docker Containers"
echo "=================================="

cd "$(dirname "$0")/.."

echo ""
echo "Building ingestion container..."
docker build -f docker/Dockerfile.ingestion -t crypto-ingestion:latest .

echo ""
echo "Building processor container..."
docker build -f docker/Dockerfile.processor -t crypto-processor:latest .

echo ""
echo "=================================="
echo "Build Complete!"
echo "=================================="
echo ""
echo "Images created:"
docker images | grep crypto
