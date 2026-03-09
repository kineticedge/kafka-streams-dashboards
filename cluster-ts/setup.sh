

mkdir -p ./plugins
curl -s -k -L  https://github.com/Aiven-Open/tiered-storage-for-apache-kafka/releases/download/v1.1.1/core-1.1.1.tgz | tar xfv - -C plugins
curl -s -k -L  https://github.com/Aiven-Open/tiered-storage-for-apache-kafka/releases/download/v1.1.1/s3-1.1.1.tgz | tar xfv - -C plugins

