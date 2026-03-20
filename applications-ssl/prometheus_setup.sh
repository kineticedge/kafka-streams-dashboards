#!/bin/bash

if ! [ -x "$(command -v jq)" ]; then
    echo "jq is not installed." >&2
    exit 1
fi

if ! [ -x "$(command -v docker)" ]; then
    echo "docker is not installed." >&2
    exit 1
fi

# TODO check for docker compose

docker info > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "docker server is not running." >&2
  exit 1
fi

cd $(dirname $0)

# collect all of the stream hostnames and create a comma delimited string with host:port
#
for i in $(docker compose ps stream --format json | jq -r ".[] | .Name"); do
  INPUT+="\"${i}:7071\","
done
INPUT=$(echo $INPUT | sed 's/,$//g')


TEMPLATE=$(cat <<EOF
[
  {
    "targets": \$v,
    "labels": { 
      "job": "streams"
      "cluster_type": "streams",
      "cluster_id": "streams"
    }
  }
]
EOF
)


FILE=../monitoring/prometheus/application_streams.json

# take template and add each host:port as an element of that array
#
jq -n --argjson v "[ $INPUT ]" "$TEMPLATE" > $FILE

cat $FILE | jq

# force Prometheus to reload the sd_file immediately, --web.enable-lifecycle must be enabled in Prometheus.
#
curl -X POST http://localhost:9090/-/reload
