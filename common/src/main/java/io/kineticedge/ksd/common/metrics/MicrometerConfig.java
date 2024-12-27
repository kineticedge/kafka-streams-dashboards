package io.kineticedge.ksd.common.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.kafka.streams.KafkaStreams;

import java.io.IOException;
import java.io.OutputStream;

public class MicrometerConfig {

  final PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

  public MicrometerConfig(final String applicationId, final KafkaStreams kafkaStreams) {

    final KafkaStreamsMetrics kafkaStreamsMetrics = new KafkaStreamsMetrics(kafkaStreams);
    kafkaStreamsMetrics.bindTo(Metrics.globalRegistry);

    Metrics.globalRegistry.gauge(
            "kafka_stream_application",
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
