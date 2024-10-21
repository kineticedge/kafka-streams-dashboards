#!/bin/bash

set -e
cd "$(dirname -- "$0")"

USERS=(
    "publisher"
    "streams"
    "analytics-none"
    "analytics-tumbling"
    "analytics-hopping"
    "analytics-sliding"
    "analytics-session"
    "analytics-none"
)

BOOTSTRAP_SERVER=localhost:19092
COMMAND_CONFIG=./secrets/admin.conf

for username in "${USERS[@]}"; do

    kafka-configs --bootstrap-server "${BOOTSTRAP_SERVER}" --command-config "${COMMAND_CONFIG}" \
      --alter \
      --add-config "SCRAM-SHA-512=[password=${username}-password]" \
      --entity-type users \
      --entity-name "${username}"

    kafka-acls \
      --bootstrap-server "${BOOTSTRAP_SERVER}" \
      --command-config "${COMMAND_CONFIG}" \
      --add \
      --force \
      --allow-principal User:${username} \
      --operation All \
      --topic '*' \
      --group '*' \
      --cluster

done

