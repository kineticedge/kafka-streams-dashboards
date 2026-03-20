#!/bin/bash

set -e
cd "$(dirname -- "$0")"

USERS=(
    "client-publisher"
    "client-stream"
    "client-analytics"
)

BOOTSTRAP_SERVER=localhost:19092

COMMAND_CONFIG=./client.properties

for username in "${USERS[@]}"; do

    echo $username

    kafka-acls \
      --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --add \
      --force \
      --allow-principal User:CN=${username} \
      --operation All \
      --topic '*' \
      --group '*' \
      --cluster

done

