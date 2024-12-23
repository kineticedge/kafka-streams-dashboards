#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ksd.streams.Main"

#JAVA_OPTS="-javaagent:./jolokia-agent.jar=port=7072,host=*"
#JAVA_OPTS="-javaagent:../cluster/jmx_prometheus/jmx_prometheus_javaagent-1.0.1.jar:../docker/jmx-exporter-config.yml"

java $JAVA_OPTS -cp "${CP}" ${JAVA_OPTS}  $MAIN "$@"
