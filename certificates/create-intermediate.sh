#!/bin/bash

BASE=$(dirname "$0")

cd ${BASE}

. ./env.sh

CA_PW=${CA_PASSWORD}
IN_PW=${INTERMEDIATE_PASSWORD}

DAYS=3650

#
# The subject name of your intermediate certificate. 
#
# An intermediate certificate isn't needed, but is more typical on how a company will supply certificates.
# By having an intermediate certificate, you will see how certificates are chained.
#
subject=${INTERMEDIATE_SUBJECT}

key=${SECRETS}/intermediate.key
req=${SECRETS}/intermediate.csr
crt=${SECRETS}/intermediate.crt
cnf=${SECRETS}/intermediate.cnf

ca_key=${SECRETS}/ca.key
ca_crt=${SECRETS}/ca.crt

heading "create intermediate certificate"

if [ -f ${key} ] &&  [ -f ${crt} ]; then
 footing "intermediate key and certificate already exist, not recreating."
 exit 0
elif [ -f ${key} ]; then
 error_msg "intermediate key already exists, but certificate does not; aborting."
 exit 1
elif [ -f ${crt} ]; then
 error_msg "intermediate certificate already exists, but key does not; aborting."
 exit 1
fi

cat <<EOF >${cnf}
[ext]
basicConstraints       = critical, CA:true, pathlen:0
keyUsage               = critical, digitalSignature, keyCertSign, cRLSign
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always, issuer
EOF


subheading "create key and csr"
openssl req -newkey rsa:2048 -sha256 -passout pass:${IN_PW} -keyout ${key} -out ${req} -subj "${subject}"
[ $? -eq 1 ] && echo "unable to create IN key and csr" && exit 1

subheading "verify key"
openssl rsa -check -in ${key} -passin pass:${IN_PW} 
[ $? -eq 1 ] && echo "unable to verify IN key" && exit 1

subheading "verify csr (request)"
openssl req -text -noout -verify -in ${req}
[ $? -eq 1 ] && echo "unable to verify IN csr" && exit 1

subheading "sign csr (request) [ create certificate ]"
openssl x509 \
	-req \
	-CA ${ca_crt} \
	-CAkey ${ca_key} \
	-passin pass:${CA_PW} \
	-in ${req} \
	-sha256 \
	-days ${DAYS} \
	-out ${crt} \
	-CAcreateserial \
	-extensions ext \
	-extfile ${cnf}
[ $? -eq 1 ] && echo "unable to sign IN csr" && exit 1

subheading "verify crt (certificate)"
openssl x509 -in ${crt} -text -noout
[ $? -eq 1 ] && echo "unable to verify IN crt" && exit 1

footing "intermediate certificate created"

true
