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

# TODO verify intermediate.crt, intermediate.key, ca.crt exists

CA_PW=${CA_PASSWORD}
IN_PW=${INTERMEDIATE_PASSWORD}
B_PW=${BROKER_PASSWORD}

password=${B_PW}

i=${HOSTNAME}

subject="/CN=${i}"

DAYS=365

key=${SECRETS}/${i}.key
req=${SECRETS}/${i}.req
crt=${SECRETS}/${i}.crt
cnf=${SECRETS}/${i}.cnf
#req_cnf=${SECRETS}/${i}.req_cnf

intermediate_key=${SECRETS}/intermediate.key
intermediate_crt=${SECRETS}/intermediate.crt

ca_crt=${SECRETS}/ca.crt

#cat < ./openssl.cnf > ${req_cnf}
#cat <<EOF >> ${req_cnf}
#[SAN]
#subjectAltName=DNS:${i}, DNS:${i}.${INTERNAL_DOMAIN}, DNS:${i}.${EXTERNAL_DOMAIN}
#EOF

heading "create certificate for ${i}"


subheading "create key and csr"
openssl req \
	-newkey rsa:2048 \
	-sha256 \
	-passout pass:${password} \
	-keyout ${key} \
	-out ${req} \
	-subj "${subject}"
[ $? -eq 1 ] && echo "unable to create key and csr" && exit 1

subheading "verify key"
openssl rsa -check -in ${key} -passin pass:${password}
[ $? -eq 1 ] && echo "unable to verify CA key" && exit 1

subheading "verify csr (request)"
openssl req -text -noout -verify -in ${req}
[ $? -eq 1 ] && echo "unable to verify CA csr" && exit 1


cat <<EOF > ${cnf}
[ext]
subjectAltName=DNS:${i}, DNS:${i}.${INTERNAL_DOMAIN}, DNS:${i}.${EXTERNAL_DOMAIN}
extendedKeyUsage=critical,serverAuth,clientAuth
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
EOF


subheading "sign csr (request) [ create certificate ]"
openssl x509 \
	-req \
	-CA ${intermediate_crt} \
	-CAkey ${intermediate_key} \
	-passin pass:$IN_PW \
	-in ${req} \
	-out ${crt} \
	-days ${DAYS} \
	-CAcreateserial \
	-extfile ${cnf} \
	-extensions ext
[ $? -eq 1 ] && echo "unable to sign the csr for ${i}." && exit 1


cat ${intermediate_crt} ${ca_crt} > ${SECRETS}/chain.pem

subheading "verify crt (certificate)"
openssl verify -CAfile ${SECRETS}/chain.pem ${crt}
[ $? -eq 1 ] && echo "unable to verify certificate for ${i}." && exit 1

subheading "print crt (certificate)"
openssl x509 -in ${crt} -text -noout
[ $? -eq 1 ] && echo "unable to print certificate for ${i}." && exit 1 

subheading "create pkcs12"
# combine key and certificate into a pkcs12 file
openssl pkcs12 -export -in ${crt} -inkey ${key} -passin pass:${password} -chain -CAfile ${SECRETS}/chain.pem -name $i -out ${SECRETS}/$i.p12 -passout pass:${password}
[ $? -eq 1 ] && echo "unable to create pkcs12 file for ${i}." && exit 1

footing "${i} certificate created"

# make sure exit status of script is 0, as above check will result in non-0 exit code
true
