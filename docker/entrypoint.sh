#!/bin/bash

set -e

#
# The IP Address will have a scaled instance
#
IP_ADDRESS=$(ifconfig eth0 | grep 'inet ' | awk '{print $2}')

#
# grab the last numerical field of the hostname and treat that as the unique ID of this scaled instance.
#
export INSTANCE_ID=$(dig -x $IP_ADDRESS +short | sed -E "s/(.*)-([0-9]+)\.(.*)/\2/")

# use INSTANCE_ID as Kafka Streams group.instance.id for static membership
export GROUP_INSTANCE_ID=${INSTANCE_ID}

# attach INSTANCE_ID to the prefix
export CLIENT_ID=${CLIENT_ID_PREFIX:-}-${INSTANCE_ID}

# by pre-loading container with large dependencies (e.g. rocksdb)
# the exploding of the tar is very lightweight.
(cd /app; tar xfv /app.tar)

PROJECT="$(ls -A /app)"
APPLICATION=$(echo "$PROJECT" | sed -E -e 's/(.*)-(.*)/\1/')
VERSION=$(echo "$PROJECT" | sed -E -e 's/(.*)-(.*)/\2/')

# move over any preloaded dependencies, move ensures this is only
# done once and also is faster as files exist in same unix volume.
shopt -s nullglob
for i in /dependencies/*; do
  echo "moving $i to /app/${PROJECT}/lib"
  mv "$i" "/app/${PROJECT}/lib"
done
shopt -u nullglob

COMMAND="/app/${PROJECT}/bin/${APPLICATION}"

#
# use exec so signals are properly handled
#
exec "${COMMAND}" "$@"
