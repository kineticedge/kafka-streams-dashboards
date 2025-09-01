package io.kineticedge.ksd.analytics;

import io.kineticedge.ksd.common.domain.ProductAnalytic;
import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.tools.serde.JsonSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StatePurger {

  private static final Logger log = LoggerFactory.getLogger(StatePurger.class);

  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

  private final KafkaStreams streams;

  private final Options.WindowType windowType;
  private final String topic;

  private final String storeName;

  private KafkaProducer<String, PurchaseOrder> producer;

  private final Runnable runnable = new Runnable() {
    @Override
    public void run() {
      purge();
    }
  };


  public StatePurger(final KafkaStreams streams, final Options options) {
    this.streams = streams;
    this.windowType = options.getWindowType();
    this.topic = options.getTopic();
    this.storeName = windowType.name() + "-aggregate-purchase-order";
    this.producer = new KafkaProducer<String, PurchaseOrder>(properties(options));

    executor.scheduleAtFixedRate(runnable, 1000L, 1000L, TimeUnit.MILLISECONDS);
  }

  private void purge() {
    try {
      ReadOnlyKeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.timestampedKeyValueStore()));
      store.all().forEachRemaining(i -> {
        log.info("TOMBSTONING {}", i.key);
        producer.send(new ProducerRecord<>(topic, null, i.key, null));
      });

      producer.flush();
    } catch (final Exception e) {
      log.error("e={}", e.getMessage(), e);
    }
  }


  private Map<String, Object> properties(final Options options) {
    return Map.ofEntries(
            Map.entry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
            Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
            Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName())
    );
  }
}
