package io.kineticedge.ksd.publisher;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

class Scratch {

  private static final String TOPIC = "foo";
  private static final String GROUP = "group.2";
  private static final String BOOTSTRAP_SERVERS = "localhost:9092";

  public static void main(String[] args) {

    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
      consumer.subscribe(List.of(TOPIC));

      try {
        while (true) {
          ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
          System.out.println("polled " + records.count());
          Set<TopicPartition> partitions = records.partitions();
          System.out.println(partitions);
        }
      } catch (WakeupException ignored) {
      } finally {
        try {
          consumer.commitSync();
        } catch (Exception e) {
        }
        System.out.println("Consumer closed.");
      }
    }
  }
}