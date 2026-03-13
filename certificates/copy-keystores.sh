#!/bin/bash

cd "$(dirname -- "$0")" || exit

. ./env.sh

#
# SCRAM based cluster does not have a keycloak
#
cp ${SECRETS}/kafka.key 			              ../cluster-sasl/secrets
cp ${SECRETS}/kafka.server.truststore.jks 	../cluster-sasl/secrets
cp ${SECRETS}/sasl-*.jks 			              ../cluster-sasl/secrets

#
# OAUTH based cluster has an additional service, keycloak
#
cp ${SECRETS}/kafka.key 			              ../cluster-sasl-oauth/secrets
cp ${SECRETS}/kafka.server.truststore.jks 	../cluster-sasl-oauth/secrets
cp ${SECRETS}/sasl-*.jks 			              ../cluster-sasl-oauth/secrets
cp ${SECRETS}/keycloak.keystore.jks 		    ../cluster-sasl-oauth/secrets

#
# copy the truststore to the application, in this scenario the trusstore
# for the kafka nodes is also the truststore for keycloak; in a "real-world"
# deployment, they could have different truststores.
#
cp ${SECRETS}/kafka.server.truststore.jks 	../applications-sasl/security
