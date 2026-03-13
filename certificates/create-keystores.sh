#!/bin/bash

BASE=$(dirname "$0")
cd ${BASE}
. ./env.sh

heading "creating keystores"

for i in ${MACHINES[@]}; do
  ${BASE}/create-keystore.sh ${i}
  [ $? -eq 1 ] && echo "unable to create keystore for ${i}" && exit 1
done

subheading "creating truststore for brokers"

# create truststore for brokers (technically not needed since brokers inner protocol is plaintext)
rm -f ${SECRETS}/kafka.server.truststore.jks
keytool -keystore ${SECRETS}/kafka.server.truststore.jks -alias ca-root -import -file ${SECRETS}/ca.crt -storepass ${BROKER_PASSWORD}  -noprompt

subheading "creating truststore for clients"

CLIENT_TRUSTSTORE=kafka.client.truststore.jks

# Creating truststore for clients (create with different password)
rm -f ${SECRETS}/${CLIENT_TRUSTSTORE}

footing "keystores created created"

true
