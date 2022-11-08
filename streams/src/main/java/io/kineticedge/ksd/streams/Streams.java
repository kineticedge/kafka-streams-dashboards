package io.kineticedge.ksd.streams;

import io.kineticedge.ksd.tools.config.CommonConfigs;
import io.kineticedge.ksd.common.domain.Product;
import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.common.domain.Store;
import io.kineticedge.ksd.common.domain.User;
import io.kineticedge.ksd.common.metrics.StreamsMetrics;
import io.kineticedge.ksd.tools.serde.JsonSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import static org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl.PROCESSOR_NODE_LEVEL_GROUP;
import static org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl.addAvgAndMinAndMaxToSensor;

@Slf4j
public class Streams {

    private static final Duration SHUTDOWN = Duration.ofSeconds(30);

    private static final Random RANDOM = new Random();

    private Map<String, Object> properties(final Options options) {

        final Map<String, Object> defaults = Map.ofEntries(
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),

               // Map.entry(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 2);

                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
                Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
                Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, options.getApplicationId()),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, options.getAutoOffsetReset()),
                Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
                Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                //Map.entry("topology.optimization", "all"),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
                //          Map.entry("built.in.metrics.version", "0.10.0-2.4"),
                //Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2),
                //Map.entry(StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG, JmxReporter.class.getName() + "," + KafkaMetricsReporter.class.getName()),
                Map.entry(CommonConfigs.METRICS_REPORTER_CONFIG, options.getCustomMetricsTopic())

        );


        final Map<String, Object> map = new HashMap<>(defaults);


        try {
            Class.forName("io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");

            log.info("adding confluent interceptors, since package is on the classpath.");

            map.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");

            map.put(StreamsConfig.PRODUCER_PREFIX + "confluent.monitoring.interceptor.bootstrap.servers",
                    options.getBootstrapServers());

            //

            map.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor");


            map.put(StreamsConfig.CONSUMER_PREFIX + "confluent.monitoring.interceptor.bootstrap.servers",
                    options.getBootstrapServers());

        } catch (Throwable t) {
            log.info("confluent interceptors not added, as library is not on the classpath.", t);
        }

        try {
            final Properties properties = new Properties();
            final File file = new File("./streams.properties");
            if (file.exists() && file.isFile()) {
                log.info("applying streams.properties");
                properties.load(new FileInputStream(file));
                map.putAll(properties.entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)));
            }
        } catch (final IOException e) {
            log.info("no streams.properties override file found");
        }

        return map;
    }


    public void start(final Options options) {

        Properties p = toProperties(properties(options));

        log.info("starting streams : " + options.getClientId());

        final Topology topology = streamsBuilder(options).build(p);

        StreamsMetrics.register(topology.describe());

        log.info("Topology:\n" + topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, p);

        streams.setUncaughtExceptionHandler(e -> {
            log.error("unhandled streams exception, shutting down.", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Runtime shutdown hook, state={}", streams.state());
            if (streams.state().isRunningOrRebalancing()) {
                streams.close(SHUTDOWN);
            }
        }));
    }

    private StreamsBuilder streamsBuilder(final Options options) {
        final StreamsBuilder builder = new StreamsBuilder();

        GlobalKTable<String, Store> stores = builder.globalTable(options.getStoreTopic(),
                Consumed.as("gktable-stores"),
                Materialized.as("store-global-table")
        );

        KTable<String, User> users = builder.table(options.getUserTopic(),
                Consumed.as("ktable-users"),
                Materialized.as("user-table")
        );

        KTable<String, Product> products = builder.table(options.getProductTopic(),
                Consumed.as("ktable-products"),
                Materialized.as("product-table")
        );


        final Materialized<String, PurchaseOrder, KeyValueStore<Bytes, byte[]>> materialized =
                Materialized.<String, PurchaseOrder, KeyValueStore<Bytes, byte[]>>as("pickup-order-reduce-store");
        //.withCachingDisabled();

//        final Materialized<String, PurchaseOrder, WindowStore<Bytes, byte[]>> materializedW =
//                Materialized.<String, PurchaseOrder, WindowStore<Bytes, byte[]>>as("pickup-order-reduce-store");
        //.withCachingDisabled();

//        final Materialized<String, PurchaseOrder, SessionStore<Bytes, byte[]>> materializedSW =
//                Materialized.<String, PurchaseOrder, SessionStore<Bytes, byte[]>>as("pickup-order-reduce-store");
        //.withCachingDisabled();


        builder.<String, PurchaseOrder>stream(options.getPurchaseTopic(), Consumed.as("purchase-order-source"))
                .transformValues(() -> new ValueTransformerWithKey<String, PurchaseOrder, PurchaseOrder>() {

                    private Sensor sensor;

                    @Override
                    public void init(ProcessorContext context) {

                        sensor = createSensor(
                                Thread.currentThread().getName(),
                                context.taskId().toString(),
                                "purchase-order-lineitem-counter",
                                (StreamsMetricsImpl) context.metrics());
                    }

                    @Override
                    public PurchaseOrder transform(String readOnlyKey, PurchaseOrder value) {
                        sensor.record(value.getItems().size());
                        return value;
                    }

                    @Override
                    public void close() {
                    }

                    public Sensor createSensor(final String threadId, final String taskId, final String processorNodeId, final StreamsMetricsImpl streamsMetrics) {
                        final Sensor sensor = streamsMetrics.nodeLevelSensor(threadId, taskId, processorNodeId, processorNodeId + "-lineitems", Sensor.RecordingLevel.INFO);
                        addAvgAndMinAndMaxToSensor(
                                sensor,
                                PROCESSOR_NODE_LEVEL_GROUP,
                                streamsMetrics.nodeLevelTagMap(threadId, taskId, processorNodeId),
                                "lineitems",
                                "average number of line items in purchase orders",
                                "minimum number of line items in purchase orders",
                                "maximum number of line items in purchase orders"
                        );
                        return sensor;
                    }
                }, Named.as("purchase-order-lineitem-counter"))
                .selectKey((k, v) -> {
                    return v.getUserId();
                }, Named.as("purchase-order-keyByUserId"))
                .join(users, (purchaseOrder, user) -> {
                    purchaseOrder.setUser(user);
                    return purchaseOrder;
                }, Joined.as("purchase-order-join-user"))
                .join(stores, (k, v) -> v.getStoreId(), (purchaseOrder, store) -> {
                    purchaseOrder.setStore(store);
                    return purchaseOrder;
                }, Named.as("purchase-order-join-store"))
                .flatMap((k, v) -> v.getItems().stream().map(item -> KeyValue.pair(item.getSku(), v)).collect(Collectors.toList()),
                        Named.as("purchase-order-products-flatmap"))
                .join(products, (purchaseOrder, product) -> {
                    purchaseOrder.getItems().stream().filter(item -> item.getSku().equals(product.getSku())).forEach(item -> item.setPrice(product.getPrice()));
                    //pause(RANDOM.nextInt(1000));
                    return purchaseOrder;
                }, Joined.as("purchase-order-join-product"))
                .groupBy((k, v) -> v.getOrderId(), Grouped.as("pickup-order-groupBy-orderId"))
//                .windowedBy(TimeWindows.of(Duration.ofSeconds(options.getWindowSize()))
//                        .grace(Duration.ofSeconds(options.getGracePeriod())))
//                .windowedBy(SlidingWindows.withTimeDifferenceAndGrace(Duration.ofSeconds(options.getWindowSize()),
//                        Duration.ofSeconds(options.getGracePeriod())))
//               .windowedBy(SessionWindows.with(Duration.ofSeconds(options.getWindowSize())))
                .reduce((incoming, aggregate) -> {
                    if (aggregate == null) {
                        aggregate = incoming;
                    } else {
                        final PurchaseOrder purchaseOrder = aggregate;
                        incoming.getItems().stream().forEach(item -> {
                            if (item.getPrice() != null) {
                                purchaseOrder.getItems().stream().filter(i -> i.getSku().equals(item.getSku())).forEach(i -> i.setPrice(item.getPrice()));
                            }
                        });
                    }
                    return aggregate;
                }, Named.as("pickup-order-reduce"), materialized)
                .filter((k, v) -> {
                    return v.getItems().stream().allMatch(i -> i.getPrice() != null);
                }, Named.as("pickup-order-filtered"))
                .toStream(Named.as("pickup-order-reduce-tostream"))
                .to(options.getPickupTopic(), Produced.as("pickup-orders"));

        // e2e
        if (true) {
            builder.<String, PurchaseOrder>stream(options.getPickupTopic(), Consumed.as("pickup-orders-consumed-e2e"))
                    .peek((k, v) -> log.debug("key={}", k), Named.as("pickup-orders-consumed-e2e-peek"));
        }

        return builder;
    }


    private static void dumpRecord(final ConsumerRecord<String, String> record) {
        log.info("Record:\n\ttopic     : {}\n\tpartition : {}\n\toffset    : {}\n\tkey       : {}\n\tvalue     : {}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    private static void pause(final long duration) {
        try {
            Thread.sleep(duration);
        } catch (final InterruptedException e) {
        }
    }
}


//        topology.addProcessor("x", new ProcessorSupplier<>() {
//                    @Override
//                    public Processor get() {
//                        return new Processor() {
//
//                            private ProcessorContext context;
//
//                            @Override
//                            public void init(ProcessorContext context) {
//                                this.context = context;
//                            }
//
//                            @Override
//                            public void process(Object key, Object value) {
//                                context.schedule(Duration.ofMillis(1000), PunctuationType.WALL_CLOCK_TIME, ts -> {
//                                    System.out.println(context.metrics().metrics().keySet());
//                                });
//                            }
//
//                            @Override
//                            public void close() {
//
//                            }
//                        };
//                    }
//                }, "PARENT");
