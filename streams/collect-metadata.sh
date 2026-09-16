#!/bin/sh

echo ""
echo "Building GraalVM agent container for metadata collection - linux/arm64"
echo ""

docker buildx build --platform=linux/arm64 -f docker.agent/Dockerfile . -t stream-agent:latest

echo ""
echo "Container built. To collect metadata, run:"
echo ""
echo "  docker run --rm -v \$(pwd)/metadata:/app/metadata stream-agent:latest [your app args]"
echo ""
echo "Metadata will be saved to ./metadata/"
echo ""
