#!/bin/bash

BASE=$(dirname "$0")
cd ${BASE}
. ./env.sh


cp ${SECRETS}/kafka.key ${SECRETS}/kafka.server.truststore.jks ../secrets
cp ${SECRETS}/kafka.server.truststore.jks ../secrets
cp ${SECRETS}/sasl-*.jks ../secrets

