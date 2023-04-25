#!/bin/bash

#
# certificate needed, brokers certificate should be based on hostname of the cerificate
#
declare -a MACHINES=(
  "sasl-broker-1"
  "sasl-broker-2"
  "sasl-broker-3"
  )

#
# domains for Subject Alternate Names (SANs)
#
INTERNAL_DOMAIN=foo
EXTERNAL_DOMAIN=bar

CA_SUBJECT="/C=US/O=ORG/CN=Root CA"
# the password of the root authority certificate
CA_PASSWORD=ca_secret

INTERMEDIATE_SUBJECT="/C=US/O=ORG/CN=Kafka CA"
# the password if the intermediate certificate
INTERMEDIATE_PASSWORD=intermediate_secret

# the password of the broker certificate and jks keystore password which has to match
BROKER_PASSWORD=broker_secret

CLIENT_TRUSTSTORE_PASSWORD=truststore_secret


SECRETS=./secrets

#
# shared functions
#

function pause(){
 read -s -n 1 
}

function heading() {
  tput setaf 2; printf "\n\n$@"; tput sgr 0
  #pause
  tput setaf 2; printf "\n"; tput sgr 0
}

function footing() {
  tput setaf 4; printf "\n$@"; tput sgr 0
  #pause
  tput setaf 4; printf "\n\n"; tput sgr 0
}

function subheading() {
  tput setaf 3; printf "\n$@\n\n"; tput sgr 0
}

function error_msg() {
  tput setaf 1; printf "\n$@\n\n"; tput sgr 0
}

