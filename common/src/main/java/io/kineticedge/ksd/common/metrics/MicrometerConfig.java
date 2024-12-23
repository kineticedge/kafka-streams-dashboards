package io.kineticedge.ksd.common.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

public class MicrometerConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MicrometerConfig.class);

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

    private final String applicationId;

    public MicrometerConfig(final String applicationId, final KafkaStreams kafkaStreams) {

        this.applicationId = applicationId;

        Metrics.globalRegistry.config().meterFilter(augmentKafkaStreamMetrics());

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

    public void scrape(OutputStream os) throws IOException  {
        prometheusMeterRegistry.scrape(os);
    }


    // kafka_stream_state

    // kafka.stream.record.cache.hit.ratio.avg
    // kafka.stream.record.cache.hit.ratio.min
    // kafka.stream.record.cache.hit.ratio.max

    private MeterFilter augmentKafkaStreamMetrics() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(final Meter.Id id) {
                if (id.getName().startsWith("kafka.stream.")) {
                    return ATTRIBUTES.stream()
                        .filter(attribute -> id.getTag(attribute) != null)
                        .findFirst()
                        .map(attribute -> {
                            final String storeType = attribute.substring(0, attribute.length() - 3).replace('.', '-');
                            final String value = id.getTag(attribute);
                            return id
                                .withTag(Tag.of("store.type", storeType))
                                .withTag(Tag.of("state.id", value));

                        }).orElse(id).withTag(Tag.of("application.id", applicationId));
                }
                return id;
            }
        };
    }
}
