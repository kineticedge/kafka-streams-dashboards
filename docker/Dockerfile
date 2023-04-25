FROM eclipse-temurin:17-jdk-jammy AS build
#FROM openjdk:17-jdk-slim AS build

RUN \
    apt-get update && \
    apt-get install -y curl && \
    curl -k -L -o jmx_prometheus_javaagent.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.18.0/jmx_prometheus_javaagent-0.18.0.jar && \
    curl -k -L -o jolokia-agent.jar "https://repo1.maven.org/maven2/org/jolokia/jolokia-jvm/1.7.2/jolokia-jvm-1.7.2.jar"

RUN \
    curl -k -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.0/opentelemetry-javaagent.jar

FROM eclipse-temurin:17-jdk-jammy
#FROM openjdk:17-jdk-slim

RUN \
    apt-get update && \
    apt-get install -y curl net-tools dnsutils iproute2 iputils-ping jq && \
    apt-get remove -y wget && \
    mkdir -p /app

# "send" and "process" enabled by default "receive" is not.
#
# https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#capturing-consumer-message-receive-telemetry-in-messaging-instrumentations
# https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/6443
#    OTEL_RESOURCE_ATTRIBUTES=""
#     -Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true"

ENV \
    ENVIRONMENT="" \
    OTEL_EXPORTER_OTLP_ENDPOINT="http://otel:4317" \
    OTEL_METRICS_EXPORTER="none" \
    OTEL_SERVICE_NAME="__UNDEFINED__" \
    OTEL_TRACES_SAMPLER="parentbased_traceidratio" \
    OTEL_TRACES_SAMPLER_ARG="0.05" \
    JAVA_OPTS="-javaagent:/jmx_prometheus_javaagent.jar=7071:/jmx-exporter-config.yml -javaagent:/jolokia-agent.jar=port=7072,host=* -javaagent:/opentelemetry-javaagent.jar"
    #JAVA_OPTS="-javaagent:/jmx_prometheus_javaagent.jar=7071:/jmx-exporter-config.yml -javaagent:/jolokia-agent.jar=port=7072,host=*"

#OTEL_INSTRUMENTATION_MESSAGING_EXPERIMENTAL_RECEIVE_TELEMETRY_ENABLED

COPY ./jmx-exporter-config.yml ./entrypoint.sh ./healthcheck-streams.sh ./healthcheck-clients.sh /

COPY --from=build /jmx_prometheus_javaagent.jar /jolokia-agent.jar /opentelemetry-javaagent.jar /

# an optional step, but by putting the 50MB rocksdb jar into the image, greatly improves startup time as it doesn't have to be
# extracted from the tar file, also improve build times.
COPY ./build/runtime /dependencies

EXPOSE 7071/tcp 7072/tcp 8080/tcp

ENTRYPOINT  ["/entrypoint.sh"]
CMD         []
