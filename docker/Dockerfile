FROM openjdk:17-jdk-slim AS build

RUN \
    apt-get update && \
    apt-get install -y curl && \
    curl -k -L -o jmx_prometheus_javaagent.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.16.1/jmx_prometheus_javaagent-0.16.1.jar && \
    curl -k -L -o jolokia-agent.jar "https://repo1.maven.org/maven2/org/jolokia/jolokia-jvm/1.7.1/jolokia-jvm-1.7.1.jar"

FROM openjdk:17-jdk-slim

RUN \
    apt-get update && \
    apt-get install -y curl net-tools dnsutils iproute2 iputils-ping jq && \
    mkdir -p /app

ENV \
    ENVIRONMENT="" \
    JAVA_OPTS="-javaagent:/jmx_prometheus_javaagent.jar=7071:/jmx-exporter-config.yml -javaagent:/jolokia-agent.jar=port=7072,host=*"

COPY ./jmx-exporter-config.yml ./entrypoint.sh ./healthcheck-streams.sh ./healthcheck-clients.sh /

COPY --from=build /jmx_prometheus_javaagent.jar /jolokia-agent.jar /

# an optional step, but by putting the 50MB rocksdb jar into the image, greatly improves startup time as it doesn't have to be
# extracted from the tar file, also improve build times.
COPY ./build/runtime /dependencies

EXPOSE 7071/tcp 7072/tcp 8080/tcp

ENTRYPOINT  ["/entrypoint.sh"]
CMD         []
