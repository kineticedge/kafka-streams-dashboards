
SCRIPT=$(cat <<EOF 

cd /opt/keycloak/bin

# login

./kcadm.sh config credentials --server http://keycloak:8080 --realm master --user admin --password keycloak

# token is only active for 1 minute, we want to see/observe refreshing of tokens within demonstrations.

./kcadm.sh update realms/master -s accessTokenLifespan=600


CLIENT_SCOPE_OPENID_ID=\$(./kcadm.sh create -x "client-scopes" -r master \
    -s name=openid \
    -s protocol=openid-connect \
    -s type=default \
    -s 'attributes."include.in.token.scope"=true' \
    -s 'attributes."display.on.consent.screen"=false' \
    -s 'attributes."gui.order"=' \
    2>&1 | cut -d\' -f2)

./kcadm.sh update default-default-client-scopes/\${CLIENT_SCOPE_OPENID_ID}

# create client scope

# creates a keycloak client scope called 'kafka-access' that will attach the audience 'aud' to the claim with a value of 'kafka'.
CLIENT_SCOPE_ID=\$(./kcadm.sh create -x "client-scopes" -r master \
    -s name=kafka-access \
    -s protocol=openid-connect \
    -s 'attributes."include.in.token.scope"=true' \
    -s 'attributes."display.on.consent.screen"=false' \
    -s 'attributes."gui.order"=' \
  2>&1 | cut -d\' -f2)

./kcadm.sh update default-default-client-scopes/\${CLIENT_SCOPE_ID}

./kcadm.sh create client-scopes/\${CLIENT_SCOPE_ID}/protocol-mappers/models \
  -s name=kafka-audience \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-audience-mapper \
  -s 'config."included.custom.audience"=kafka' \
  -s 'config."multivalued"=true' \
  -s 'config."access.token.claim"=true'

./kcadm.sh create clients -r master \
	-s clientId=cli \
	-s enabled=true \
	-s clientAuthenticatorType=client-secret \
	-s secret=cli-secret \
	-s serviceAccountsEnabled=true \
  -s defaultClientScopes="[\"openid\", \"kafka-access\"]"

./kcadm.sh create clients -r master \
	-s clientId=app-ui \
	-s enabled=true \
	-s clientAuthenticatorType=client-secret \
	-s secret=app-ui-secret \
	-s serviceAccountsEnabled=true \
  -s defaultClientScopes="[\"openid\", \"kafka-access\"]"


./kcadm.sh get clients -r master --fields 'clientId'

EOF
)

docker exec -it ksd-keycloak /bin/sh -c "$SCRIPT"

