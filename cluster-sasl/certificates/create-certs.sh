#!/bin/bash

BASE=$(dirname "$0")

cd ${BASE}

. ./env.sh

for i in "${MACHINES[@]}"; do
  ${BASE}/create-cert.sh ${i}
  [ $? -eq 1 ] && echo "unable to create host certificate for ${i}" && exit 1
done

true
