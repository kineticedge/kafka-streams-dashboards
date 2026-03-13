#!/bin/bash

TOKEN=$(curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials" \
     -d "scope=kafka" \
     -d "client_id=app2-client" \
     -d "client_secret=app2-client-secret" | jq -r .access_token)

echo $TOKEN

curl -I -X GET "http://localhost:8080/realms/master/protocol/openid-connect/userinfo" \
        -H "Authorization: Bearer $TOKEN"
