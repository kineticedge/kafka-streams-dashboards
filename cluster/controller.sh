#!/bin/sh

# Docker workaround: Remove check for KAFKA_ZOOKEEPER_CONNECT parameter
sed -i '/KAFKA_ZOOKEEPER_CONNECT/d' /etc/confluent/docker/configure

# Docker workaround: If controller runs separately, then this needs to be removed also
sed -i '/dub ensure KAFKA_ADVERTISED_LISTENERS/d' /etc/confluent/docker/configure

# Docker workaround: Ignore cub zk-ready
sed -i '/cub zk-ready/d' /etc/confluent/docker/ensure

#base64-encoded UUID
CLUSTER_UUID=0000000000000000000000

#
# KafkaConfig.scala will complain if advertised.listeners is present, even if it is an empty string
#
echo "sed -i.bak '/advertised.listeners/d' /etc/kafka/kafka.properties" >> /etc/confluent/docker/ensure

# KRaft required step: Format the storage directory with a new cluster ID
echo "kafka-storage format --ignore-formatted --cluster-id=$CLUSTER_UUID -c /etc/kafka/kafka.properties" >> /etc/confluent/docker/ensure
