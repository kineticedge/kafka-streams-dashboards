#!/bin/bash

set -e

KEYCLOAK_URL="http://localhost:8080"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM="kafka"
CLIENT_ID="kafka-client"
CLIENT_SECRET="kafka-client-secret"

MAX_RETRIES=30
RETRY_COUNT=0

echo "Waiting for Keycloak to be ready..."
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  if curl -s "$KEYCLOAK_URL/auth/admin/realms" > /dev/null 2>&1; then
    echo "✓ Keycloak is responding"
    break
  fi
  
  RETRY_COUNT=$((RETRY_COUNT + 1))
  echo "  Attempt $RETRY_COUNT/$MAX_RETRIES - Keycloak not ready yet, waiting..."
  sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
  echo "✗ Keycloak failed to start after ${MAX_RETRIES} retries"
  exit 1
fi

echo ""
echo "Getting admin token..."
TOKEN_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/auth/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASSWORD" \
  -d "grant_type=password")

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [ -z "$TOKEN" ]; then
  echo "✗ Failed to get admin token"
  echo "Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "✓ Got admin token"
echo ""
echo "Creating realm '$REALM'..."

# Create realm
REALM_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/auth/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "'$REALM'",
    "enabled": true,
    "displayName": "Kafka OAuth Realm",
    "accessTokenLifespan": 3600
  }')

echo "✓ Realm created/verified"

echo ""
echo "Creating client '$CLIENT_ID'..."

# Create client with proper scopes
CLIENT_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/auth/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'$CLIENT_ID'",
    "name": "Kafka Client",
    "enabled": true,
    "public": false,
    "secret": "'$CLIENT_SECRET'",
    "standardFlowEnabled": false,
    "implicitFlowEnabled": false,
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": true,
    "clientAuthenticatorType": "client-secret-basic"
  }')

echo "✓ Client created/verified"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✓ Keycloak setup complete!"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "Available endpoints:"
echo "  • Token endpoint:     $KEYCLOAK_URL/auth/realms/$REALM/protocol/openid-connect/token"
echo "  • JWKS endpoint:      $KEYCLOAK_URL/auth/realms/$REALM/protocol/openid-connect/certs"
echo "  • Admin UI:           $KEYCLOAK_URL/auth/admin"
echo ""
echo "Client credentials:"
echo "  • Client ID:          $CLIENT_ID"
echo "  • Client Secret:      $CLIENT_SECRET"
echo ""
echo "Test token retrieval:"
echo "  curl -X POST $KEYCLOAK_URL/auth/realms/$REALM/protocol/openid-connect/token \\"
echo "    -d 'grant_type=client_credentials' \\"
echo "    -d 'client_id=$CLIENT_ID' \\"
echo "    -d 'client_secret=$CLIENT_SECRET'"
echo ""
