#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ksd.streams.Main"

java -cp "${CP}" $MAIN "$@"
