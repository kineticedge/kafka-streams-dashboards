#!/bin/sh

# Docker workaround: Remove check for KAFKA_ZOOKEEPER_CONNECT parameter
sed -i '/KAFKA_ZOOKEEPER_CONNECT/d' /etc/confluent/docker/configure

# Docker workaround: Don't do cub zk-ready
sed -i '/cub zk-ready/d' /etc/confluent/docker/ensure

CLUSTER_UUID=0000000000000000000000

# KRaft required step: Format the storage directory with a new cluster ID
echo "kafka-storage format --ignore-formatted --cluster-id=$CLUSTER_UUID -c /etc/kafka/kafka.properties" >> /etc/confluent/docker/ensure
