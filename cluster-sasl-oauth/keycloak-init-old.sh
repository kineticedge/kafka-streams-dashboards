#!/bin/sh

set -eu

KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8080}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-master}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-keycloak}"

cd /opt/keycloak/bin

echo "Waiting for Keycloak at ${KEYCLOAK_URL}..."

until ./kcadm.sh config credentials \
  --server "${KEYCLOAK_URL}" \
  --realm master \
  --user "${KEYCLOAK_ADMIN_USER}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD}" >/dev/null 2>&1
do
  sleep 0.5
done

scope_exists() {
  scope_name="$1"
  ./kcadm.sh get client-scopes -r "${KEYCLOAK_REALM}" --fields name \
    | grep -q "\"name\" : \"${scope_name}\""
}

client_exists() {
  client_id="$1"
  ./kcadm.sh get clients -r "${KEYCLOAK_REALM}" -q clientId="${client_id}" \
    | grep -q "\"clientId\" : \"${client_id}\""
}

if scope_exists kafka && client_exists app-ui; then
  echo "Keycloak already initialized. Exiting."
  exit 0
fi


echo "Keycloak is ready. Applying initialization..."

./kcadm.sh update "realms/${KEYCLOAK_REALM}" -s accessTokenLifespan=3000

get_client_scope_id() {
  scope_name="$1"
  ./kcadm.sh get client-scopes -r "${KEYCLOAK_REALM}" --fields id,name \
    | grep -B 1 "\"name\" : \"${scope_name}\"" \
    | grep '"id"' \
    | head -n 1 \
    | cut -d '"' -f 4
}

create_client_scope_if_missing() {
  scope_name="$1"
  scope_id="$(get_client_scope_id "${scope_name}" || true)"

  if [ -z "${scope_id}" ]; then
    ./kcadm.sh create client-scopes -r "${KEYCLOAK_REALM}" \
      -s name="${scope_name}" \
      -s protocol=openid-connect \
      -s 'attributes."include.in.token.scope"=true' \
      -s 'attributes."display.on.consent.screen"=false' >/dev/null
    scope_id="$(get_client_scope_id "${scope_name}")"
  fi

  echo "${scope_id}"
}

ensure_default_scope() {
  scope_id="$1"
  if ! ./kcadm.sh get "default-default-client-scopes/${scope_id}" -r "${KEYCLOAK_REALM}" >/dev/null 2>&1; then
    ./kcadm.sh update "default-default-client-scopes/${scope_id}" -r "${KEYCLOAK_REALM}"
  fi
}

ensure_audience_mapper() {
  scope_id="$1"
  if ! ./kcadm.sh get "client-scopes/${scope_id}/protocol-mappers/models" -r "${KEYCLOAK_REALM}" \
    | grep -q '"name" : "kafka"'; then
    ./kcadm.sh create "client-scopes/${scope_id}/protocol-mappers/models" -r "${KEYCLOAK_REALM}" \
      -s name=kafka \
      -s protocol=openid-connect \
      -s protocolMapper=oidc-audience-mapper \
      -s 'config."id.token.claim"=false' \
      -s 'config."access.token.claim"=true' \
      -s 'config."included.custom.audience"=kafka' >/dev/null
  fi
}

client_exists() {
  client_id="$1"
  ./kcadm.sh get clients -r "${KEYCLOAK_REALM}" -q clientId="${client_id}" \
    | grep -q "\"clientId\" : \"${client_id}\""
}

create_client_if_missing() {
  client_id="$1"
  client_secret="$2"

  if ! client_exists "${client_id}"; then
    ./kcadm.sh create clients -r "${KEYCLOAK_REALM}" \
      -s clientId="${client_id}" \
      -s enabled=true \
      -s clientAuthenticatorType=client-secret \
      -s secret="${client_secret}" \
      -s serviceAccountsEnabled=true \
      -s defaultClientScopes='["kafka","openid"]' >/dev/null
  fi
}

OPENID_SCOPE_ID="$(create_client_scope_if_missing openid)"
KAFKA_SCOPE_ID="$(create_client_scope_if_missing kafka)"

ensure_default_scope "${OPENID_SCOPE_ID}"
ensure_default_scope "${KAFKA_SCOPE_ID}"
ensure_audience_mapper "${KAFKA_SCOPE_ID}"

create_client_if_missing streams streams-oauth-password
create_client_if_missing publisher publisher-oauth-password
create_client_if_missing analytics-tumbling analytics-tumbling-oauth-password
create_client_if_missing analytics-hopping analytics-hopping-oauth-password
create_client_if_missing analytics-sliding analytics-sliding-oauth-password
create_client_if_missing analytics-session analytics-session-oauth-password
create_client_if_missing analytics-none analytics-none-oauth-password

create_client_if_missing app-ui app-ui-secret
create_client_if_missing cli cli-secret

#create_client_if_missing app1-client app1-client-secret
#create_client_if_missing app2-client app2-client-secret
#create_client_if_missing app-jumphost app-jumphost-secret

#echo "Final client list:"
#./kcadm.sh get clients -r "${KEYCLOAK_REALM}" --fields clientId

echo "Keycloak initialization complete."
