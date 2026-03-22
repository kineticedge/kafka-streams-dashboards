
FROM ghcr.io/graalvm/native-image-community:25 AS builder

RUN mkdir -p /app/config/META-INF/native-image/io.kineticedge.ksd/stream


COPY ./src/main/resources/META-INF/native-image /app/config/META-INF/native-image


#COPY ./src/main/resources/META-INF/native-image/io.kineticedge.ksd/stream/reachability-metadata.json /app/config/reachability-metadata.json

COPY ./build/distributions/streams-1.0.tar ./streams-1.0.tar


RUN tar xfv ./streams-1.0.tar

##
## TODO
##
## support x86 too - net/jpountz/util/linux/amd64/liblz4-java.so

RUN mkdir -p /app/native-libs && \
    cd /app/native-libs && \
    jar xf /app/streams-1.0/lib/rocksdbjni-10.1.3.jar librocksdbjni-linux-aarch64.so && \
    jar xf /app/streams-1.0/lib/lz4-java-1.10.1.jar net/jpountz/util/linux/aarch64/liblz4-java.so && \
    mv net/jpountz/util/linux/aarch64/liblz4-java.so . && \
    rm -rf net && \
    jar xf /app/streams-1.0/lib/zstd-jni-1.5.6-10.jar linux/aarch64/libzstd-jni-1.5.6-10.so && \
    mv linux/aarch64/libzstd-jni-1.5.6-10.so . && \
    rm -rf linux && \
    jar xf /app/streams-1.0/lib/snappy-java-1.1.10.7.jar org/xerial/snappy/native/Linux/aarch64/libsnappyjava.so && \
    mv org/xerial/snappy/native/Linux/aarch64/libsnappyjava.so . && \
    rm -rf org

RUN CLASSPATH=$(echo streams-1.0/lib/*.jar | tr ' ' ':') &&  \
  native-image \
  -cp "/app/config:${CLASSPATH}" io.kineticedge.ksd.streams.Main -o ./build/stream \
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
  -march=compatibility \
  --initialize-at-build-time=org.slf4j.LoggerFactory,org.slf4j.helpers,ch.qos.logback,ch.qos.logback.classic.Logger,org.xml.sax.helpers \
  --verbose


FROM debian:12-slim AS runner

RUN apt-get update && apt-get install -y --no-install-recommends \
    zlib1g && \
    rm -rf /var/lib/apt/lists/* \

USER 1001

COPY --from=builder /app/stream /app/
COPY --from=builder /app/native-libs /app/native-libs

ENV LD_LIBRARY_PATH=/app/native-libs

WORKDIR /app
ENTRYPOINT ["/app/stream"]
EXPOSE 8899
