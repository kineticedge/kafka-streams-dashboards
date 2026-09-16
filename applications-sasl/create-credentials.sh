#!/bin/bash

if [ "$#" -ne 1 ]; then
  echo "usage: $0 scram|oauth"
  exit
fi

auth=$1
shift

set -e
cd "$(dirname -- "$0")"

declare -a USERS=(
    "publisher"
    "streams"
    "analytics-tumbling"
    "analytics-hopping"
    "analytics-session"
    "analytics-sliding"
    "analytics-none"
)

EXTRA=""
if [ "$auth" == "oauth" ]; then
  EXTRA="oauth-"
fi

for i in "${USERS[@]}"; do
  sed -e "s/{{USERNAME}}/${i}/" -e "s/{{PASSWORD}}/${i}-${EXTRA}password/" "./security/${auth}.template" > "./security/credentials-${auth}-${i}.properties"
done
