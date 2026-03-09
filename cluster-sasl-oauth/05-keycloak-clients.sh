
SCRIPT=$(cat <<EOF 

cd /opt/keycloak/bin

# login

./kcadm.sh config credentials --server http://keycloak:8080 --realm master --user admin --password keycloak

#./kcadm.sh update realms/master -s accessTokenLifespan=600

#./kcadm.sh get clients -r master -q clientId=app-ui --fields 'id'

#APP_UI_CLIENT_ID=$(./kcadm.sh get clients -r master -q clientId=app-ui --fields 'id' 2>&1 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

APP_UI_CLIENT_ID=f148925c-6b35-490a-b4c1-672f79a2e64f

echo $APP_UI_CLIENT_ID

./kcadm.sh create clients/f148925c-6b35-490a-b4c1-672f79a2e64f/protocol-mappers/models \
    -s name="Token Type Mapper" \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-hardcoded-claim-mapper \
    -s 'config."claim.name"=typ' \
    -s 'config."claim.value"=JWT' \
    -s 'config."access.token.claim"=true' \
    -s 'config."id.token.claim"=false'


EOF
)

docker exec -it ksd-keycloak /bin/sh -c "$SCRIPT"
