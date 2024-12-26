package io.kineticedge.ksd.common.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MicrometerConfig {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(MicrometerConfig.class);

  private static final String KAFKA_STREAM_STATE_PREFIX = "kafka.stream.state.";

  // only one of these will ever be defined, listed in order of typical ocurrance.
  private static final List<String> ATTRIBUTES = List.of(
          "rocksdb.state.id",
          "rocksdb.window.state.id",
          "rocksdb.session.state.id",
          "in.memory.state.id",
          "in.memory.window.state.id",
          "in.memory.lru.state.id",
          "in.memory.suppression.state.id"
  );

  final PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

  public MicrometerConfig(final String applicationId, final KafkaStreams kafkaStreams) {

    // pull the metric out of the name and place it in a tag for 'kafka_stream_state' except for the updater
    Metrics.globalRegistry.config().meterFilter(
            new MeterFilter() {
              @Override
              public Meter.Id map(final Meter.Id id) {
                if (!id.getName().startsWith(KAFKA_STREAM_STATE_PREFIX) || id.getName().startsWith(KAFKA_STREAM_STATE_PREFIX + "updater")) {
                  return id;
                }
                return ATTRIBUTES.stream()
                        .map(attribute -> new KeyValue<>(attribute, id.getTag(attribute)))
                        .filter(kv -> kv.value != null)
                        .findFirst()
                        .map(kv -> {
                          final String storeType = kv.key.substring(0, kv.key.length() - 3).replace('.', '-');
                          return id
                                  .withTag(Tag.of("store.type", storeType))
                                  .withTag(Tag.of("state.id", kv.value));
                        }).orElse(id)
                        ;
              }
            }
    );

    final KafkaStreamsMetrics kafkaStreamsMetrics = new KafkaStreamsMetrics(kafkaStreams);
    kafkaStreamsMetrics.bindTo(Metrics.globalRegistry);

    Metrics.globalRegistry.gauge(
            "kafka_streams_infor",
            Tags.of(Tag.of("application.id", applicationId)),
            kafkaStreams,
            streams -> switch (streams.state()) {
              case RUNNING -> 1.0;
              case REBALANCING -> 0.5;
              default -> 0.0;
            }
    );

    Metrics.globalRegistry.add(prometheusMeterRegistry);
  }

  public void scrape(OutputStream os) throws IOException {
    prometheusMeterRegistry.scrape(os);
  }

}
