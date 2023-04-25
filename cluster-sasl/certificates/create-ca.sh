#!/bin/bash

BASE=$(dirname "$0")
cd ${BASE}
. ./env.sh

ca_password=${CA_PASSWORD}

DAYS=3650

#
# the subject name for your certificate authority certificate
#
subject=${CA_SUBJECT}

key=${SECRETS}/ca.key
req=${SECRETS}/ca.csr
crt=${SECRETS}/ca.crt
cnf=${SECRETS}/ca.cnf


cat <<EOF >${cnf}
[ext]
basicConstraints       = critical, CA:true
keyUsage               = critical, keyCertSign, cRLSign
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always, issuer
EOF

#crlDistributionPoints = @crl_info
#authorityInfoAccess = @ocsp_info
#
#[crl_info]
#URI.0 = http://crl.grilledcheese.us/whoremovedmycheese.crl
#
#[ocsp_info]
#caIssuers;URI.0 = http://ocsp.grilledcheese.us/cheddarcheeseroot.crt
#OCSP;URI.0 = http://ocsp.grilledcheese.us/

heading "create root certificate"

if [ -f ${key} ] &&  [ -f ${crt} ]; then
 footing "CA key and certificate already exist, not recreating."
 exit 0
elif [ -f ${key} ]; then
 error_msg "CA key already exists, but certificate does not; aborting."
 exit 1
elif [ -f ${crt} ]; then
 error_msg "CA certificate already exists, but key does not; aborting."
 exit 1
fi



subheading "create key and csr"
openssl req \
  -newkey rsa:4096 \
  -sha256 \
  -passout pass:${ca_password} \
  -keyout ${key} \
  -out ${req} \
  -subj "${subject}"
#	-reqexts ext \
#	-config <(cat ./openssl.cnf <(printf "\n[ext]\nbasicConstraints=CA:TRUE"))
[ $? -eq 1 ] && error_msg "unable to create CA key and csr" && exit 1

subheading "verify key"
openssl rsa -check -in ${key} -passin pass:${ca_password}
[ $? -eq 1 ] && error_msg "unable to verify CA key" && exit 1

subheading "verify csr (request)"
openssl req -text -noout -verify -in ${req}
[ $? -eq 1 ] && error_msg "unable to verify CA csr" && exit 1

subheading "sign csr (request) [ create certificate ]"
openssl x509 \
	-req \
	-in ${req} \
	-sha256 \
	-days ${DAYS} \
	-passin pass:${ca_password} \
	-signkey ${key} \
	-out ${crt} \
	-extensions ext \
	-extfile ${cnf}
[ $? -eq 1 ] && error_msg "unable to self-sign CA csr" && exit 1

subheading "verify crt (certificate)"
openssl x509 -in ${crt} -text -noout
[ $? -eq 1 ] && error_msg "unable to verify CA crt" && exit 1

footing "root certificate created"

true
