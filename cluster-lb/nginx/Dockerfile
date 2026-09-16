FROM nginx:1.27.4

RUN \
    apt-get update && \
    apt-get install -y curl iproute2 iputils-ping

#RUN \
#    apt-get install -y blockade


COPY ./broker_latency.sh /root/
