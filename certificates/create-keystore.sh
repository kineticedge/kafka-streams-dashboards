#!/bin/bash

if [ $# -lt 1 ]; then
  echo "usage: $0 hostname"
  exit
fi

BASE=$(dirname "$0")
cd ${BASE}
. ./env.sh

HOSTNAME=$1
shift

#

p12=${SECRETS}/${HOSTNAME}.p12
keystore=${SECRETS}/${HOSTNAME}.keystore.jks

subheading "creating keystore for ${HOSTNAME}."

rm -f ${keystore}

keytool -importkeystore \
	-srckeystore ${p12} \
	-srcstorepass $BROKER_PASSWORD \
	-srcstoretype pkcs12 \
	-destkeystore ${keystore} \
	-deststorepass $BROKER_PASSWORD \
	-deststoretype pkcs12


kafka_key=${SECRETS}/kafka.key

rm -f ${kafka-key}
echo ${BROKER_PASSWORD} > ${kafka_key}

