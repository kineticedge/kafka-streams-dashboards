#!/bin/sh

echo ""
echo "building native 'stream' application image - for linux/arm64"
echo ""


docker buildx build --platform=linux/arm64 -f docker.native/Dockerfile . -t stream-native:latest






#docker buildx build --platform=linux/amd64 -f docker.native/Dockerfile . -t stream-native:latest
