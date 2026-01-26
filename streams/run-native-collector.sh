#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ksd.streams.Main"

GROUP_ID="io.kineticedge.ksd"
ARTIFACT_ID="stream"


JAVA_HOME=$GRAALVM_HOME
JAVA_OPTS="-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/${GROUP_ID}/${ARTIFACT_ID}"

JAVA="${GRAALVM_HOME}/bin/java"

${JAVA} $JAVA_OPTS -cp "${CP}" $MAIN "$@"
