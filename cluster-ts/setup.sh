#!/bin/sh

cd "$(dirname -- "$0")" || exit

mkdir -p ./plugins

if [ ! -d "./plugins/core-1.1.1" ]; then
  curl -s -k -L  https://github.com/Aiven-Open/tiered-storage-for-apache-kafka/releases/download/v1.1.1/core-1.1.1.tgz | tar xfv - -C plugins
fi

if [ ! -d "./plugins/s3-1.1.1" ]; then
  curl -s -k -L  https://github.com/Aiven-Open/tiered-storage-for-apache-kafka/releases/download/v1.1.1/s3-1.1.1.tgz | tar xfv - -C plugins
fi
