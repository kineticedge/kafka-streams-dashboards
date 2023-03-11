package io.kineticedge.ksd.streams.reporter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kineticedge.ksd.tools.config.CommonConfigs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaMetricsReporter implements MetricsReporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Duration INTERVAL = Duration.ofSeconds(5);

    final Map<String, Object> defaults = Map.ofEntries(
            Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
            Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
            Map.entry(ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO"),
            Map.entry(ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG, "")
    );

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final Map<MetricName, Pair<KafkaMetric, ObjectNode>> map = new ConcurrentHashMap<>();

    private KafkaProducer<String, String> producer;

    private String topic;

    private String applicationId;
    private String groupId;
    private String clientId;

    //
    // TODO eventually allow for separate broker for metrics.
    //
    @Override
    public void configure(final Map<String, ?> configs) {

        final Map<String, Object> map = new HashMap<>(configs);
        map.putAll(defaults);

        // ensure monitoring interceptors are not part of the metrics reporter.
        map.remove(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        map.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
        map.remove("confluent.monitoring.interceptor.bootstrap.servers");

        producer = new KafkaProducer<>(map);

        applicationId = (String) configs.get(StreamsConfig.APPLICATION_ID_CONFIG);
        groupId = (String) configs.get(ConsumerConfig.GROUP_ID_CONFIG);
        clientId = (String) configs.get(CommonClientConfigs.CLIENT_ID_CONFIG);

        topic = (String) configs.get(CommonConfigs.METRICS_REPORTER_CONFIG);

        if (clientId == null) {
            clientId = UUID.randomUUID().toString();
        }



        clientId += "-reporter";

        log.debug("applicationId={}, groupId={}, clientId={}, topic={}", applicationId, groupId, clientId, topic);
    }


    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {

//            log.debug("sending metrics");

            if (topic == null) {
                log.warn("metric topic not defined.");
                return;
            }

            map.forEach((k, v) -> {
                final KafkaMetric metric = v.getKey();
                final ObjectNode node = v.getValue();

                final String name = metric.metricName().name();

                final Object object = metric.metricValue();

                if (object instanceof Double) {
                    node.put("value", (Double) object);
                } else if (object instanceof Number) {
                    node.put("value", ((Number) object).doubleValue());
                } else {
                    //TODO -- something else?
                    node.putNull("value");
                }
                Double value = (Double) metric.metricValue();

                // only send process rate/total metrics
//                if ("process-rate".equals(name) || "process-total".equals(name)) {
//                    double value = v.getKey().metricValue(); // metricValue() causing deadlocks, TBD
                // double value = (double) metric.metricValue();
//                double value = 0;
//                    DoubleNode valueNode = (DoubleNode) node.get("value");
//                    if (valueNode != null) {
//                        node.put("previous", valueNode.doubleValue());
//                        node.put("delta", value - valueNode.doubleValue() );
//                    }

                node.put("timestamp", System.currentTimeMillis());

                // TODO determine a better key to use
                producer.send(
                        new ProducerRecord<>(topic, null, null, metric.metricName().name(), serialize(node)),
                        (metadata, e) -> {
                            if (e != null) {
                                log.warn("unable to publish to metrics topic e={}", e.getMessage());
                            }
                        }
                );
//                }
            });
        }
    };


    @Override
    public void init(final List<KafkaMetric> metrics) {
        metrics.forEach(metric -> {
            map.put(metric.metricName(), Pair.of(metric, jsonNode(metric)));
        });

        executor.scheduleAtFixedRate(runnable, INTERVAL.toMillis(), INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void metricChange(final KafkaMetric metric) {
        map.put(metric.metricName(), Pair.of(metric, jsonNode(metric)));
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        map.remove(metric.metricName());
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
    }

    private static String serialize(final JsonNode jsonNode) {

        if (jsonNode == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectNode jsonNode(final KafkaMetric metric) {

        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("group-id", applicationId != null ? applicationId : groupId);
        // objectNode.put("client-id", clientId); -- should be part of the tags
        objectNode.put("name", metric.metricName().name());
        objectNode.put("group", metric.metricName().group());

        metric.metricName().tags().forEach((k, v) -> {
            objectNode.put(k, v);
            if ("task-id".equals(k) && v.indexOf('_') > 0) {
                objectNode.put("subtopology", v.split("_")[0]);
                objectNode.put("partition", v.split("_")[1]);
            }
        });

        if (objectNode.get("thread-id") == null) {
            objectNode.set("thread-id", objectNode.get("client-id"));
        } else if (objectNode.get("client-id") == null) {
            objectNode.set("client-id", objectNode.get("thread-id"));
        }

        return objectNode;
    }
}
