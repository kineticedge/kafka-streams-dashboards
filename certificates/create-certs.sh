#!/bin/bash

cd "$(dirname -- "$0")" || exit

. ./env.sh

for i in "${MACHINES[@]}"; do
  ./create-cert.sh ${i}
  [ $? -eq 1 ] && echo "unable to create host certificate for ${i}" && exit 1
done

true
