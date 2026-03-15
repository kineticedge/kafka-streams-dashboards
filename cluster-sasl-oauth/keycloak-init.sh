#!/bin/sh
set -eu

KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8080}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-master}"
KEYCLOAK_ADMIN_REALM="${KEYCLOAK_ADMIN_REALM:-master}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-keycloak}"


BASE_ADMIN_URL="${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}"

get_admin_token() {
  curl -fsS \
    -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_ADMIN_REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=password" \
    --data-urlencode "client_id=admin-cli" \
    --data-urlencode "username=${KEYCLOAK_ADMIN_USER}" \
    --data-urlencode "password=${KEYCLOAK_ADMIN_PASSWORD}" \
    | jq -r '.access_token'
}

api_get() {
  path="$1"
  curl -fsS \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_ADMIN_URL}${path}"
}

api_post_json() {
  path="$1"
  payload="$2"
  curl -fsS \
    -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${payload}" \
    "${BASE_ADMIN_URL}${path}" >/dev/null
}

api_put_json() {
  path="$1"
  payload="$2"
  curl -fsS \
    -X PUT \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${payload}" \
    "${BASE_ADMIN_URL}${path}" >/dev/null
}

api_put_empty() {
  path="$1"
  curl -fsS \
    -X PUT \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_ADMIN_URL}${path}" >/dev/null
}

scope_exists() {
  scope_name="$1"
  printf '%s\n' "${SCOPES_JSON}" | jq -e --arg name "${scope_name}" '.[] | select(.name == $name)' >/dev/null
}

client_exists() {
  client_id="$1"
  printf '%s\n' "${CLIENTS_JSON}" | jq -e --arg clientId "${client_id}" '.[] | select(.clientId == $clientId)' >/dev/null
}

scope_id() {
  scope_name="$1"
  printf '%s\n' "${SCOPES_JSON}" | jq -r --arg name "${scope_name}" 'map(select(.name == $name)) | .[0].id // empty'
}

default_scope_attached() {
  scope_name="$1"
  printf '%s\n' "${DEFAULT_SCOPES_JSON}" | jq -e --arg name "${scope_name}" '.[] | select(.name == $name)' >/dev/null
}

refresh_scopes() {
  SCOPES_JSON="$(api_get "/client-scopes")"
}

refresh_clients() {
  CLIENTS_JSON="$(api_get "/clients")"
}

refresh_default_scopes() {
  DEFAULT_SCOPES_JSON="$(api_get "/default-default-client-scopes")"
}

create_client_scope_if_missing() {
  scope_name="$1"

  if ! scope_exists "${scope_name}"; then
    echo "Creating client scope: ${scope_name}" >&2
    api_post_json "/client-scopes" "$(jq -cn --arg name "${scope_name}" '{
      name: $name,
      protocol: "openid-connect",
      attributes: {
        "include.in.token.scope": "true",
        "display.on.consent.screen": "false"
      }
    }')"
    refresh_scopes
  fi

  scope_id "${scope_name}"
}

ensure_default_scope() {
  scope_name="$1"
  scope_id_value="$2"

  if ! default_scope_attached "${scope_name}"; then
    echo "Attaching default scope: ${scope_name}" >&2
    api_put_empty "/default-default-client-scopes/${scope_id_value}"
    refresh_default_scopes
  fi
}

ensure_audience_mapper() {
  scope_id_value="$1"
  mappers_json="$(api_get "/client-scopes/${scope_id_value}/protocol-mappers/models")"

  if ! printf '%s\n' "${mappers_json}" | jq -e '.[] | select(.name == "kafka")' >/dev/null; then
    echo "Creating kafka audience mapper" >&2
    api_post_json "/client-scopes/${scope_id_value}/protocol-mappers/models" "$(jq -cn '{
      name: "kafka",
      protocol: "openid-connect",
      protocolMapper: "oidc-audience-mapper",
      config: {
        "id.token.claim": "false",
        "access.token.claim": "true",
        "included.custom.audience": "kafka"
      }
    }')"
  fi
}

create_client_if_missing() {
  client_id="$1"
  client_secret="$2"

  if ! client_exists "${client_id}"; then
    echo "Creating client: ${client_id}" >&2
    api_post_json "/clients" "$(jq -cn \
      --arg clientId "${client_id}" \
      --arg secret "${client_secret}" \
      '{
        clientId: $clientId,
        enabled: true,
        clientAuthenticatorType: "client-secret",
        secret: $secret,
        serviceAccountsEnabled: true,
        defaultClientScopes: ["kafka", "openid"]
      }')"
    refresh_clients
  fi
}


echo "Waiting for Keycloak at ${KEYCLOAK_URL}..."
until TOKEN="$(get_admin_token 2>/dev/null)" && [ -n "${TOKEN}" ] && [ "${TOKEN}" != "null" ]; do
  sleep 0.2
done

echo "Keycloak is ready. Loading current state..."

SCOPES_JSON="$(api_get "/client-scopes")"
CLIENTS_JSON="$(api_get "/clients")"
DEFAULT_SCOPES_JSON="$(api_get "/default-default-client-scopes")"


if scope_exists "kafka" && client_exists "app-ui"; then
  echo "Keycloak already initialized. Exiting."
  exit 0
fi

echo "Applying initialization..."

api_put_json "" '{"accessTokenLifespan":3000}'

OPENID_SCOPE_ID="$(create_client_scope_if_missing "openid")"
KAFKA_SCOPE_ID="$(create_client_scope_if_missing "kafka")"

ensure_default_scope "openid" "${OPENID_SCOPE_ID}"
ensure_default_scope "kafka" "${KAFKA_SCOPE_ID}"
ensure_audience_mapper "${KAFKA_SCOPE_ID}"

create_client_if_missing "streams" "streams-oauth-password"
create_client_if_missing "publisher" "publisher-oauth-password"
create_client_if_missing "analytics-tumbling" "analytics-tumbling-oauth-password"
create_client_if_missing "analytics-hopping" "analytics-hopping-oauth-password"
create_client_if_missing "analytics-sliding" "analytics-sliding-oauth-password"
create_client_if_missing "analytics-session" "analytics-session-oauth-password"
create_client_if_missing "analytics-none" "analytics-none-oauth-password"

create_client_if_missing "app-ui" "app-ui-oauth-password"
create_client_if_missing "cli" "cli-oauth-password"

echo "Keycloak initialization complete."
