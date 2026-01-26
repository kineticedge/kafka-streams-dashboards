#!/bin/bash

. ./.classpath.sh

MAIN="io.kineticedge.ksd.streams.Main"
APP="stream"

GROUP_ID="io.kineticedge.ksd"
ARTIFACT_ID="stream"

METADATA_DIR="src/main/resources/META-INF/native-image/${GROUP_ID}/${ARTIFACT_ID}"
REACHABILITY_METADATA="${METADATA_DIR}/reachability-metadata.json"

$GRAALVM_HOME/bin/native-image \
  -cp ${CP} \
  ${MAIN} -o ./${APP} \
  --no-fallback \
  --enable-http \
  --enable-https \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --install-exit-handlers \
  --enable-monitoring=jmxserver,jmxclient,heapdump,jvmstat \
  -H:+ReportExceptionStackTraces \
  -H:+EnableAllSecurityServices \
  -H:EnableURLProtocols=http,https \
  -H:AdditionalSecurityProviders=sun.security.jgss.SunProvider \
  -H:ReachabilityMetadataResources=${REACHABILITY_METADATA} \
  -march=compatibility \
  --initialize-at-build-time=org.slf4j.LoggerFactory,org.slf4j.helpers,ch.qos.logback,ch.qos.logback.classic.Logger,org.xml.sax.helpers \
  --verbose