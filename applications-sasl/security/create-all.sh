#!/bin/bash

BASE=$(dirname "$0")

cd ${BASE}


declare -a SCRAM=(
  "producer"
  "streams"
  "analytics-tumbling"
  "analytics-hopping"
  "analytics-session"
  "analytics-sliding"
  "analytics-none"
)

for i in "${SCRAM[@]}"; do
  sed -e "s/{{USERNAME}}/${i}/" -e "s/{{PASSWORD}}/${i}-password/" scram-template.properties > credentials-scram-${i}.properties
done

for i in "${SCRAM[@]}"; do
  sed -e "s/{{USERNAME}}/${i}/" -e "s/{{PASSWORD}}/${i}-oauth-password/" oauth-template.properties > credentials-oauth-${i}.properties
done

true
