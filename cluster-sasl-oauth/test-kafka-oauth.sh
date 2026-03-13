#!/bin/bash
set -euo pipefail

BROKER_CONTAINER="${BROKER_CONTAINER:-sasl-broker-1}"
BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-sasl-broker-1:9093}"
CLIENT_ID="${CLIENT_ID:-app-ui}"
CLIENT_SECRET="${CLIENT_SECRET:-app-ui-secret}"
REALM="${REALM:-master}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8080}"

docker compose exec "${BROKER_CONTAINER}" /bin/sh -ec "
cat > /tmp/oauth-client.properties <<'EOF'
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=\"${CLIENT_ID}\" clientSecret=\"${CLIENT_SECRET}\";
sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler
sasl.oauthbearer.token.endpoint.url=${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token
sasl.oauthbearer.jwt.retriever.class=org.apache.kafka.common.security.oauthbearer.ClientCredentialsJwtRetriever
ssl.endpoint.identification.algorithm=
ssl.truststore.location=/etc/kafka/secrets/kafka.server.truststore.jks
ssl.truststore.password=broker_secret
EOF

export KAFKA_OPTS='-Dorg.apache.kafka.sasl.oauthbearer.allowed.urls=${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token,${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/certs'

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server ${BOOTSTRAP_SERVER} \
  --command-config /tmp/oauth-client.properties \
  --list

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server ${BOOTSTRAP_SERVER} \
  --command-config /tmp/oauth-client.properties \
  --create --topic foo


"
