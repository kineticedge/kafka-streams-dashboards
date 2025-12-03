package io.kineticedge.ksd.publisher;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;

class Produce {

  private static final String TOPIC = "foo";
  private static final String BOOTSTRAP_SERVERS = "localhost:9092";

  public static void main(String[] args) throws InterruptedException {

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.LINGER_MS_CONFIG, "0");

    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          producer.flush();
        } catch (Exception ignored) {}
      }));


      long i = 0L;
      while (true) {
        String key = "key-" + (i % 10);
        String value = "msg-" + i + " @ " + Instant.now();

        // Send specifically to partition 0
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, 0, key, value);

        Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
          System.out.printf("partitions=%s, offset=%s, timestamp=%s\n".formatted(metadata.partition(), metadata.offset(), metadata.timestamp()));
        });

        i++;
        Thread.sleep(1000);
      }
    }
  }


}