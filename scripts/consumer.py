#!/usr/bin/python3

import argparse
import sys
from confluent_kafka import Consumer, KafkaError, KafkaException, libversion


def consume_messages(topics):
    conf = {
        'debug': 'telemetry',
        'bootstrap.servers': 'localhost:19092',
        'group.id': 'python-consumer-a',
        'auto.offset.reset': 'earliest'
    }

    # Create Consumer instance
    consumer = Consumer(conf)

    def print_assignment(consumer, partitions):
        print('Assignment:', partitions)

    # Subscribe to topics
    consumer.subscribe(topics, on_assign=print_assignment)

    try:
        while True:
            msg = consumer.poll(timeout=1.0)  # Poll for messages

            if msg is None:
                continue

            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    # Reached end of partition event
                    print('%% %s [%d] reached end at offset %d\n' %
                          (msg.topic(), msg.partition(), msg.offset()))
                elif msg.error():
                    raise KafkaException(msg.error())
            else:
                # Proper message
                print('%% %s [%d] at offset %d with key %s:\n' %
                      (msg.topic(), msg.partition(), msg.offset(),
                       str(msg.key())))
                print(msg.value().decode('utf-8'))

    except KeyboardInterrupt:
        print('Aborted by user')

    finally:
        # Close down consumer to commit final offsets.
        consumer.close()


if __name__ == '__main__':

    version = libversion()
    version_str = version[0]
    print(f"librdkafka version: {version_str} ({version[1]})")

    required_version = "2.6.0"

    # Split the version string into major, minor, and patch numbers
    version_parts = [int(part) for part in version_str.split('.')]
    required_version_parts = [int(part) for part in required_version.split('.')]

    if version_parts < required_version_parts:
        raise RuntimeError(
            f"librdkafka version {version_str} is less than the required version {required_version}. Please upgrade your librdkafka installation.")
    else:
        print(f"librdkafka version {version_str} meets the required version {required_version}.")

    parser = argparse.ArgumentParser(description='Kafka Consumer')
    # parser.add_argument('broker', type=str, help='Kafka broker address')
    # parser.add_argument('group', type=str, help='Consumer group id')
    parser.add_argument('topics', type=str, nargs='+', help='List of Kafka topics to consume from')

    args = parser.parse_args()

    topic = args.topics

    consume_messages(topic)
