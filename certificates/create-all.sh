#!/bin/bash

BASE=$(dirname "$0")
cd ${BASE}

./create-ca.sh
[ $? -eq 1 ] && echo "unable to create CA certificate" && exit 1

./create-intermediate.sh
[ $? -eq 1 ] && echo "unable to create intermediate certifcate" && exit 1

./create-certs.sh
[ $? -eq 1 ] && echo "unable to create host certificates" && exit 1

./create-keystores.sh
[ $? -eq 1 ] && echo "unable to create keystores" && exit 1

./copy-keystores.sh
[ $? -eq 1 ] && echo "unable to copy keystores to cluster secrets" && exit 1

true
