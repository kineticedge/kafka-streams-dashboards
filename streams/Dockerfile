FROM openjdk:17-jdk-slim AS build

RUN \
    apt-get update && \
    apt-get install -y curl && \
    curl -k -L -o jmx_prometheus_javaagent.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.16.1/jmx_prometheus_javaagent-0.16.1.jar && \
    curl -k -L -o jolokia-agent.jar "https://repo1.maven.org/maven2/org/jolokia/jolokia-jvm/1.7.1/jolokia-jvm-1.7.1.jar"

FROM openjdk:17-jdk-slim

RUN \
    apt-get update && \
    apt-get install -y curl net-tools dnsutils iproute2 iputils-ping jq

ENV \
    ENVIRONMENT="" \
    JAVA_OPTS="-javaagent:/jmx_prometheus_javaagent.jar=7071:/streams-config.yml -javaagent:/jolokia-agent.jar=port=7072,host=*"

COPY ./build/distributions/*.tar ./docker/streams-config.yml ./docker/all-config.yml ./docker/entrypoint.sh ./docker/healthcheck.sh /

COPY --from=build /jmx_prometheus_javaagent.jar /jolokia-agent.jar /

RUN \
	tar xfvp /streams-1.0.tar

EXPOSE 7071/tcp 7072/tcp

ENTRYPOINT  ["/entrypoint.sh"]
CMD         ["/streams-1.0/bin/streams"]
