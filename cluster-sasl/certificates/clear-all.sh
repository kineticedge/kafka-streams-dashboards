#!/bin/bash

cd $(dirname $0)

rm -f ./secrets/*.jks
rm -f ./secrets/*.p12
rm -f ./secrets/*.req
rm -f ./secrets/*.crt
rm -f ./secrets/*.key
rm -f ./secrets/*.cnf
rm -f ./secrets/*.csr
rm -f ./secrets/*.srl
rm -f ./secrets/*.pem
