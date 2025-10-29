#!/bin/bash

# Build and run Supermarket Game Server with Docker

echo "🏪 Building Supermarket Game Server Docker Image..."

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Build the image
echo "📦 Building Docker image..."
docker-compose build

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "To start the server, run:"
    echo "  docker-compose up"
    echo ""
    echo "Or run in background:"
    echo "  docker-compose up -d"
    echo ""
    echo "To stop the server:"
    echo "  docker-compose down"
    echo ""
    echo "To view logs:"
    echo "  docker-compose logs -f"
else
    echo "❌ Build failed!"
    exit 1
fi