package io.kineticedge.ksd.streams;

import io.kineticedge.ksd.common.domain.Product;
import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.common.domain.Store;
import io.kineticedge.ksd.common.domain.User;
import io.kineticedge.ksd.common.metrics.StreamsMetrics;
import io.kineticedge.ksd.common.rocksdb.RocksDBConfigSetter;
import io.kineticedge.ksd.tools.config.KafkaEnvUtil;
import io.kineticedge.ksd.tools.config.PropertyUtils;
import io.kineticedge.ksd.tools.serde.JsonSerde;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TaskMetadata;
import org.apache.kafka.streams.ThreadMetadata;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl.PROCESSOR_NODE_LEVEL_GROUP;
import static org.apache.kafka.streams.processor.internals.metrics.StreamsMetricsImpl.addAvgAndMinAndMaxToSensor;

public class Streams {

    private static final Logger log = LoggerFactory.getLogger(Streams.class);

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
                //Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),

//                Map.entry(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, 10_000),
                Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                //Map.entry("topology.optimization", "all"),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
                //Map.entry("built.in.metrics.version", "0.10.0-2.4"),
//                Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2),
                Map.entry(StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG, JmxReporter.class.getName()),
                //Map.entry(StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG, JmxReporter.class.getName() + "," + KafkaMetricsReporter.class.getName()),
                //Map.entry(CommonConfigs.METRICS_REPORTER_CONFIG, options.getCustomMetricsTopic())

                Map.entry(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, RocksDBConfigSetter.class.getName()),

//            Map.entry(ProducerConfig.RETRIES_CONFIG, 5),

                Map.entry("internal.leave.group.on.close", true)

        );


        final Map<String, Object> map = new HashMap<>(defaults);

        //
        // If set the consumer is treated as a static member; this ID must be unique for every member in the group.
        //
        // * if you look at docker/entrypoint.sh it uses the numerical value within docker to ensure a uniqe instance.
        //
        // * if you are running stand-alone, you need to set this uniquely for every instance you plan on running.
        //
        if (options.getGroupInstanceId() != null) {
            map.put(CommonClientConfigs.GROUP_INSTANCE_ID_CONFIG, options.getGroupInstanceId());
        }

//        try {
//            Class.forName("io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");
//
//            log.info("adding confluent interceptors, since package is on the classpath.");
//
//            map.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
//                    "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");
//
//            map.put(StreamsConfig.PRODUCER_PREFIX + "confluent.monitoring.interceptor.bootstrap.servers",
//                    options.getBootstrapServers());
//
//            //
//
//            map.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
//                    "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor");
//
//
//            map.put(StreamsConfig.CONSUMER_PREFIX + "confluent.monitoring.interceptor.bootstrap.servers",
//                    options.getBootstrapServers());
//
//        } catch (Throwable t) {
//            log.info("confluent interceptors not added, as library is not on the classpath.", t);
//        }

        //OTEL_SERVICE_NAME
        //OTEL_TRACES_EXPORTER

//        -Dotel.service.name=my-kafka-service \
//        -Dotel.traces.exporter=jaeger \
//        -Dotel.metrics.exporter=none \

        //Class.forName("io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor");
        //Class.forName("io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingConsumerInterceptor");

//        map.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
//        map.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());

        map.putAll(PropertyUtils.loadProperties("./streams.properties"));

        map.putAll(new KafkaEnvUtil().to("KAFKA_"));

        final String instanceId = System.getenv("INSTANCE_ID");
        if (instanceId != null) {
            int id = Integer.parseInt(System.getenv("INSTANCE_ID"));
            map.putAll(new KafkaEnvUtil().to("KAFKA_" + id + "_"));
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
            log.error("unhandled streams exception, shutting down (a warning of 'Detected that shutdown was requested. All clients in this app will now begin to shutdown' will repeat every 100ms for the duration of session timeout).", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        final PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        prometheusMeterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public MeterFilterReply accept(io.micrometer.core.instrument.Meter.Id id) {
                if (id.getName().contains("number.open.files")) {
                    log.debug("removing problematic metric {}", id.getName());
                    return MeterFilterReply.DENY;
                }
                return MeterFilterReply.NEUTRAL;
            }

            @Override
            public Meter.Id map(Meter.Id id) {
                // Use Stream.concat to build the final tag list in one pipeline
                var tags = Stream.concat(
                        id.getTags().stream().filter(t -> !t.getKey().equals("kafka.version")),
                        Stream.of(Tag.of("application_id", options.getApplicationId())
                        )
                ).collect(Collectors.toList());
                return new Meter.Id(id.getName(), Tags.of(tags), id.getBaseUnit(), id.getDescription(), id.getType());
            }
        });


        Metrics.addRegistry(prometheusMeterRegistry);

        final KafkaStreamsMetrics kafkaStreamsMetrics = new KafkaStreamsMetrics(streams);

        kafkaStreamsMetrics.bindTo(prometheusMeterRegistry);

//        prometheusMeterRegistry.forEachMeter(meter -> {
//            System.out.println("meter: " + meter.getId());
//        });

        prometheusMeterRegistry.gauge(
                "kafka_stream_application",
                Tags.empty(),
                //Tags.of(Tag.of("application.id", options.getApplicationId())),
                streams,
                s -> switch (s.state()) {
                    case RUNNING -> 1.0;
                    case REBALANCING -> 0.5;
                    default -> 0.0;
                }
        );

        Metrics.globalRegistry.add(prometheusMeterRegistry);

        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING) {
                // Query the local threads for metadata
                Set<ThreadMetadata> localThreads = streams.metadataForLocalThreads();
                for (ThreadMetadata thread : localThreads) {
                    System.out.println("Thread: " + thread.threadName());
                    // Get Active Tasks
                    for (TaskMetadata task : thread.activeTasks()) {
                        TaskId taskId = task.taskId();
                        Set<TopicPartition> partitions = task.topicPartitions();
                        System.out.println("  Active Task ID: " + taskId);
                        System.out.println("  Assigned Partitions: " + partitions);
                    }
                    // Get Standby Tasks
                    for (TaskMetadata task : thread.standbyTasks()) {
                        System.out.println("  Standby Task ID: " + task.taskId());
                    }
                }
            }
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Runtime shutdown hook, state={}", streams.state());
            if (streams.state().isRunningOrRebalancing()) {

                // New to Kafka Streams 3.3, you can have the application leave the group on shutting down (when member.id / static membership is used).
                //
                // There are reasons to do this and not to do it; from a development standpoint this makes starting/stopping
                // the application a lot easier reducing the time needed to rejoin the group.
                boolean leaveGroup = true;

                log.info("closing KafkaStreams with leaveGroup={}", leaveGroup);

                KafkaStreams.CloseOptions closeOptions = new KafkaStreams.CloseOptions().timeout(SHUTDOWN).leaveGroup(leaveGroup);

                boolean isClean = streams.close(closeOptions);
                if (!isClean) {
                    System.out.println("KafkaStreams was not closed cleanly");
                }

            } else if (streams.state().isShuttingDown()) {
                log.info("Kafka Streams is already shutting down with state={}, will wait {} to ensure proper shutdown.", streams.state(), SHUTDOWN);
                boolean isClean = streams.close(SHUTDOWN);
                if (!isClean) {
                    System.out.println("KafkaStreams was not closed cleanly");
                }
                System.out.println("final KafkaStreams state=" + streams.state());
            }
        }));

        final StateObserver observer = new StateObserver(streams);

        final Server servletDeployment2 = new Server(observer, prometheusMeterRegistry, options.getPort());
        servletDeployment2.start();
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
                Materialized.<String, PurchaseOrder, KeyValueStore<Bytes, byte[]>>as("pickup-order-reduce-store")
                        .withCachingDisabled();

//        final Materialized<String, PurchaseOrder, WindowStore<Bytes, byte[]>> materializedW =
//                Materialized.<String, PurchaseOrder, WindowStore<Bytes, byte[]>>as("pickup-order-reduce-store");
        //.withCachingDisabled();

//        final Materialized<String, PurchaseOrder, SessionStore<Bytes, byte[]>> materializedSW =
//                Materialized.<String, PurchaseOrder, SessionStore<Bytes, byte[]>>as("pickup-order-reduce-store");
        //.withCachingDisabled();


        builder.<String, PurchaseOrder>stream(options.getPurchaseTopic(), Consumed.as("purchase-order-source"))
                .peek((k, v) -> log.info("[purchase-order=source] key={}, timestamp={}", k, LocalDateTime.now()))
                .processValues(() -> new FixedKeyProcessor<String, PurchaseOrder, PurchaseOrder>() { // transformValues deprecated, changed to this.

                    private Sensor sensor;

                    private FixedKeyProcessorContext<String, PurchaseOrder> context;

                    @Override
                    public void init(FixedKeyProcessorContext<String, PurchaseOrder> context) {
                        this.context = context;
                        sensor = createSensor(
                                Thread.currentThread().getName(),
                                context.taskId().toString(),
                                "purchase-order-lineitem-counter",
                                (StreamsMetricsImpl) context.metrics());
                    }

                    @Override
                    public void process(FixedKeyRecord<String, PurchaseOrder> record) {
                        sensor.record(record.value().getItems().size());
                        context.forward(record);
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
                .peek((k, v) -> log.info("[purchase-order-join-user] key={}, timestamp={}", k, LocalDateTime.now()))
                .join(stores, (k, v) -> v.getStoreId(), (purchaseOrder, store) -> {
                    purchaseOrder.setStore(store);
                    return purchaseOrder;
                }, Named.as("purchase-order-join-store"))
                .peek((k, v) -> log.info("[purchase-order-join-store] key={}, timestamp={}", k, LocalDateTime.now()))
                .flatMap((k, v) -> v.getItems().stream().map(item -> KeyValue.pair(item.getSku(), v)).collect(Collectors.toList()),
                        Named.as("purchase-order-products-flatmap"))
                .join(products, (purchaseOrder, product) -> {
                    purchaseOrder.getItems().stream().filter(item -> item.getSku().equals(product.sku())).forEach(item -> item.setPrice(product.price()));
                    //pause(RANDOM.nextInt(1000));
                    return purchaseOrder;
                }, Joined.as("purchase-order-join-product"))
                .peek((k, v) -> {
                            //causes so many headaches -- pause(RANDOM.nextInt(1000));
                            log.info("[purchase-order-join-product] key={}, timestamp={}", k, LocalDateTime.now());
                        }
                )
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
                .peek((k, v) -> log.info("[pickup-order-reduce-tostream] key={}, timestamp={}", k, LocalDateTime.now()))
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
