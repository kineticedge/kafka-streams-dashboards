#!/bin/bash
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ksd.builder.Main"

IS_SASL=$(docker inspect -f '{{.State.Running}}' ksdsc-sasl-broker-1 2>/dev/null || echo "false")

if [ "$IS_SASL" == "true" ]; then
  echo ""
  echo "SASL based container is running, adding in SASL based connection properties."
  echo ""
  export KAFKA_BOOTSTRAP_SERVERS=localhost:19092
  export KAFKA_SECURITY_PROTOCOL=SASL_PLAINTEXT
  export KAFKA_SASL_MECHANISM=PLAIN
  export KAFKA_SASL_JAAS_CONFIG="org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka-admin\" password=\"kafka-admin-secret\";"
fi

java -cp "${CP}" $MAIN "$@"

