#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ksd.streams.Main"

#JAVA_OPTS="-javaagent:./jolokia-agent.jar=port=7072,host=*"

java -cp "${CP}" ${JAVA_OPTS}  $MAIN "$@"
