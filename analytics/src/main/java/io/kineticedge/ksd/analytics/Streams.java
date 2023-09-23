package io.kineticedge.ksd.analytics;

import io.kineticedge.ksd.analytics.util.FixedKeyRecordFactory;
import io.kineticedge.ksd.common.domain.ProductAnalytic;
import io.kineticedge.ksd.common.domain.ProductAnalyticOut;
import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.common.metrics.StreamsMetrics;
import io.kineticedge.ksd.tools.serde.JsonSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.processor.api.InternalFixedKeyRecordFactory;
import org.apache.kafka.streams.state.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class Streams {

  private static final boolean INCLUDE_REREAD_TASK = true;

  private static final Duration SHUTDOWN = Duration.ofSeconds(30);

  //private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  private Map<String, Object> properties(final Options options) {
    final Map<String, Object> defaults = Map.ofEntries(
            Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),
            Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
            Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
            Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
            Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
            Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, options.getApplicationId()),
            Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
            Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, options.getAutoOffsetReset()),
            Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, options.getCommitInterval()),
            Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
            Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "TRACE"),
            Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName()),
            Map.entry(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, RocksDBConfig.class.getName()),

            Map.entry(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 2 * 1024 * 1024L),          // default is 10 MiB (10 * 1024 * 1024L)

            // very helpful for demo applications so instance immediately leaves group when closed.
            Map.entry("internal.leave.group.on.close", true)
    );

    final Map<String, Object> map = new HashMap<>(defaults);

    try {
      final Properties properties = new Properties();
      final File file = new File("./analytics.properties");
      if (file.exists() && file.isFile()) {
        log.info("applying analytics.properties");
        properties.load(new FileInputStream(file));
        map.putAll(properties.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)));
      }
    } catch (final IOException e) {
      log.info("no analytics.properties override file found");
    }


    return map;
  }

  private final Options options;

  public Streams(Options options) {
    this.options = options;
  }


  public void start() {

    Properties p = toProperties(properties(options));

    log.info("starting streams " + options);

    final Topology topology = streamsBuilder().build(p);

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
        boolean isClean = streams.close(SHUTDOWN);
        if (!isClean) {
          // use system out, since logger may have been shut down successfully.
          System.out.println("unable to close cleanly.");
        }
      }
    }));

    final StateObserver observer = new StateObserver(streams, options.getWindowType());
    final ServletDeployment servletDeployment = new ServletDeployment(observer, options.getPort());

    //final StatePurger purger = new StatePurger(streams, options);

    servletDeployment.start();
  }

  private StreamsBuilder streamsBuilder() {
    return switch (options.getWindowType()) {
      case TUMBLING -> streamsBuilderTumbling();
      case HOPPING -> streamsBuilderHopping();
      case SLIDING -> streamsBuilderSliding();
      case SESSION -> streamsBuilderSession();
      case NONE -> streamsBuilderNone();
      case NONE_REPARTITIONED -> streamsBuilderNoneRepartitioned();
    };
  }

  private StreamsBuilder streamsBuilderTumbling() {

    final StreamsBuilder builder = new StreamsBuilder();

    final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("TUMBLING-aggregate-purchase-order")
            //.withRetention(Duration.ofHours(2L))
            //.withCachingDisabled()
            ;

    builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("TUMBLING-line-item"))
            .peek((k, v) -> log.info("key={}", k), Named.as("TUMBLING-peek-incoming"))
            .groupByKey(Grouped.as("TUMBLING-groupByKey"))
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofSeconds(options.getWindowSize()), Duration.ofSeconds(options.getGracePeriod())))
            .aggregate(Streams::initialize,
                    Streams::aggregator,
                    Named.as("TUMBLING-aggregate"),
                    store)
            .toStream(Named.as("TUMBLING-toStream"))
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("TUMBLING-peek-outgoing"))
            .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
            .mapValues(Streams::minimize, Named.as("TUMBLING-mapValues"))
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("TUMBLING-peek-outgoing-2"))
            .to(options.getOutputTopic() + "-" + options.getWindowType(), Produced.as("TUMBLING-to"));

    if (INCLUDE_REREAD_TASK) {
      builder.<String, ProductAnalyticOut>stream(options.getOutputTopic() + "-" + options.getWindowType(), Consumed.as("TUMBLING-to-consumer"))
              .peek((k, v) -> log.debug("key={}", k), Named.as("TUMBLING-to-consumer-peek"));
    }

    return builder;
  }

  private StreamsBuilder streamsBuilderHopping() {

    final StreamsBuilder builder = new StreamsBuilder();

    final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("HOPPING-aggregate-purchase-order")
            //.withLoggingDisabled()
            //.withCachingDisabled()
            ;

    builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("HOPPING-line-item"))
            .peek((k, v) -> log.info("key={}", k), Named.as("HOPPING-peek-incoming"))
            .groupByKey(Grouped.as("HOPPING-groupByKey"))
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofSeconds(options.getWindowSize()), Duration.ofSeconds(options.getGracePeriod()))
                            .advanceBy(Duration.ofSeconds(options.getWindowSize() / 2)))
            .aggregate(Streams::initialize,
                    Streams::aggregator,
                    Named.as("HOPPING-aggregate"),
                    store)
            .toStream(Named.as("HOPPING-toStream"))
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("HOPPING-peek-outgoing"))
            .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
            .mapValues(Streams::minimize, Named.as("HOPPING-mapValues"))
            .to(options.getOutputTopic() + "-" + options.getWindowType(), Produced.as("HOPPING-to"));

    // e2e
    if (INCLUDE_REREAD_TASK) {
      builder.<String, ProductAnalyticOut>stream(options.getOutputTopic() + "-" + options.getWindowType(), Consumed.as("HOPPING-to-consumer"))
              .peek((k, v) -> log.debug("key={}", k), Named.as("HOPPING-to-consumer-peek"));
    }

    return builder;
  }

  private StreamsBuilder streamsBuilderSliding() {

    final StreamsBuilder builder = new StreamsBuilder();

    final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("SLIDING-aggregate-purchase-order")
            //.withLoggingDisabled()
            //.withCachingDisabled()
            // if suppression is added, this is needed, otherwise the windowing causes confusion and a class-cast exception occurs
            .withKeySerde(Serdes.String())
            ;
            //.withRetention(Duration.ofMinutes(2));
            //.withRetention(Duration.ofDays(5L));

    builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("SLIDING-line-item"))
            //.merge(builder.stream("foo"))
            .peek((k, v) -> log.info("key={}", k), Named.as("SLIDING-peek-incoming"))
            .groupByKey(Grouped.as("SLIDING-groupByKey"))
            .windowedBy(SlidingWindows.ofTimeDifferenceAndGrace(
                    Duration.ofSeconds(options.getWindowSize()),
                    Duration.ofSeconds(options.getGracePeriod())))
            .aggregate(Streams::initialize,
                    Streams::aggregator,
                    Named.as("SLIDING-aggregate"),
                    store)
            .suppress(
                    Suppressed.<Windowed<String>>untilWindowCloses(Suppressed.BufferConfig.unbounded()).withName("SLIDING-suppression")
            )
            .toStream(Named.as("SLIDING-toStream"))
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("SLIDING-peek-outgoing"))
            .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
            .mapValues(Streams::minimize, Named.as("SLIDING-mapValues"))
            .to(options.getOutputTopic() + "-" + options.getWindowType(), Produced.as("SLIDING-to"));

    // e2e
    if (INCLUDE_REREAD_TASK) {
      builder.<String, ProductAnalyticOut>stream(options.getOutputTopic() + "-" + options.getWindowType(), Consumed.as("SLIDING-to-consumer"))
              .peek((k, v) -> log.debug("key={}", k), Named.as("SLIDING-to-consumer-peek"));
    }

    return builder;
  }

  private StreamsBuilder streamsBuilderSession() {

    final StreamsBuilder builder = new StreamsBuilder();

    final Materialized<String, ProductAnalytic, SessionStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, SessionStore<Bytes, byte[]>>as("SESSION-aggregate-purchase-order")
            //.withRetention(Duration.ofDays(1000L))
            //.withLoggingDisabled()
            //.withCachingDisabled()
            ;

    builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("SESSION-line-item"))
            .peek((k, v) -> log.info("key={}", k), Named.as("SESSION-peek-incoming"))
            .groupByKey(Grouped.as("SESSION-groupByKey"))
            .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofSeconds(options.getWindowSize())))
            .aggregate(Streams::initialize,
                    Streams::aggregator,
                    Streams::mergeSessions,
                    Named.as("SESSION-aggregate"),
                    store)
            .toStream(Named.as("SESSION-toStream"))
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("SESSION-peek-outgoing"))
            .filter((k, v) -> v != null)
            .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
            .mapValues(Streams::minimize, Named.as("SESSION-mapValues"))
            .to(options.getOutputTopic() + "-" + options.getWindowType(), Produced.as("SESSION-to"));

    // e2e
    if (INCLUDE_REREAD_TASK) {
      builder.<String, ProductAnalyticOut>stream(options.getOutputTopic() + "-" + options.getWindowType(), Consumed.as("SESSION-to-consumer"))
              .peek((k, v) -> log.debug("key={}", k), Named.as("SESSION-to-consumer-peek"));
    }

    return builder;
  }

  private StreamsBuilder streamsBuilderNone() {

    final StreamsBuilder builder = new StreamsBuilder();

    final Materialized<String, ProductAnalytic, KeyValueStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, KeyValueStore<Bytes, byte[]>>as("NONE-aggregate-purchase-order")
            //.withLoggingDisabled()
            //.withCachingDisabled()
            ;

    builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("NONE-line-item"))
            //.peek((k, v) -> log.info("key={}", k), Named.as("NONE-peek-incoming"))
            .groupByKey(Grouped.as("NONE-groupByKey"))
            .aggregate(Streams::initialize,
                    Streams::aggregator,
                    Named.as("NONE-aggregate"),
                    store)
            .toStream(Named.as("NONE-toStream"))
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("NONE-peek-outgoing"))
            .processValues(() -> new FixedKeyProcessor<String, ProductAnalytic, ProductAnalytic>() {
              private FixedKeyProcessorContext<String, ProductAnalytic> context;
              @Override
              public void init(FixedKeyProcessorContext<String, ProductAnalytic> context) {
                this.context = context;
                this.context.schedule(Duration.ofMillis(options.getWindowSize()*1000L/2L), PunctuationType.WALL_CLOCK_TIME, new Punctuator() {
                  @Override
                  public void punctuate(long timestamp) {
                    log.info("running punctuate, timestamp={}", timestamp);
                    final TimestampedKeyValueStore<String, ProductAnalytic> store = context.getStateStore("NONE-aggregate-purchase-order");
                    try (final KeyValueIterator<String, ValueAndTimestamp<ProductAnalytic>> iterator = store.all()) {
                      iterator.forEachRemaining(element -> {
                        if (element.value.timestamp() < System.currentTimeMillis() - (options.getWindowSize() * 1000L)) {
                          log.info("key={} is over {} seconds old, deleting.", element.key, options.getWindowSize());
                          store.delete(element.key);
                          // also emitting it one last time.
                          final Headers headers = new RecordHeaders();
                          headers.add("deleted", "true".getBytes(StandardCharsets.UTF_8));
                          context.forward(FixedKeyRecordFactory.create(element.key, element.value.value(), element.value.timestamp(), headers));
                        }
                      });
                    }
                  }
                });
              }
              @Override
              public void process(FixedKeyRecord<String, ProductAnalytic> record) {
                log.info("has_metadata={}, key={}", context.recordMetadata().isPresent(), record.key());
                context.forward(record);
              }
            }, "NONE-aggregate-purchase-order")
            .mapValues(Streams::minimize, Named.as("NONE-mapValues"))
            .to(options.getOutputTopic() + "-" + options.getWindowType(), Produced.as("NONE-to"));

    if (INCLUDE_REREAD_TASK) {
      builder.<String, ProductAnalyticOut>stream(options.getOutputTopic() + "-" + options.getWindowType(), Consumed.as("NONE-to-consumer"))
              .peek((k, v) -> log.debug("key={}", k), Named.as("NONE-to-consumer-peek"));
    }

    return builder;
  }

  /**
   * TODO - work in progress.
   */
  private StreamsBuilder streamsBuilderNoneRepartitioned() {

    final StreamsBuilder builder = new StreamsBuilder();

    final Materialized<String, ProductAnalytic, KeyValueStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, KeyValueStore<Bytes, byte[]>>as("NONE_REPARTITIONED-aggregate-purchase-order")
            //.withLoggingDisabled()
            .withCachingDisabled();


    builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("NONE_REPARTITIONED-line-item"))
            .repartition(Repartitioned.<String, PurchaseOrder>as("__NONE_REPARTITIONED__").withNumberOfPartitions(8))
            .peek((k, v) -> log.info("key={}", k), Named.as("NONE_REPARTITIONED-peek-incoming"))
            .groupByKey(Grouped.as("NONE_REPARTITIONED-groupByKey"))
            .aggregate(Streams::initialize,
                    Streams::aggregator,
                    Named.as("NONE_REPARTITIONED-aggregate"),
                    store)
            .toStream(Named.as("NONE_REPARTITIONED-toStream"))
//                .merge(restore)
//                .transformValues(() -> new ValueTransformerWithKey<String, ProductAnalytic, ProductAnalytic>() {
//
//                    private ProcessorContext context;
//                    private KeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store;
//
//                    @Override
//                    public void init(ProcessorContext context) {
//                        this.context = context;
//                        store = context.getStateStore("NONE_REPARTITIONED-aggregate-purchase-order");
//                    }
//
//                    @Override
//                    public ProductAnalytic transform(String key, ProductAnalytic value) {
//
//                        if (context.headers().lastHeader("RESTORE") != null) {
//
//                            log.info("RESTORE!!! ");
//
//                            if (store.get(key) == null) {
//                                store.put(key, ValueAndTimestamp.make(value, context.timestamp()));
//                                log.info("stored!");
//                            }
//
//                        }
//                        //store.all();
//                        log.info(value.toString());
//                        return value;
//                    }
//
//                    @Override
//                    public void close() {
//                    }
//                }, "NONE_REPARTITIONED-aggregate-purchase-order")
            .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("NONE_REPARTITIONED-peek-outgoing"))
            .processValues(() -> new FixedKeyProcessor<String, ProductAnalytic, ProductAnalytic>() {

              private FixedKeyProcessorContext<String, ProductAnalytic> context;
              private KeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store;

              @Override
              public void init(FixedKeyProcessorContext<String, ProductAnalytic> context) {

                this.context = context;

                this.context.schedule(Duration.ofSeconds(5L), PunctuationType.WALL_CLOCK_TIME, new Punctuator() {
                  @Override
                  public void punctuate(long timestamp) {
                    log.info("running punctuate, timestamp={}", timestamp);

                    StateStore store = context.getStateStore("NONE_REPARTITIONED-aggregate-purchase-order");

                    System.out.println(">>>>> + " + store.getClass());
                  }
                });
              }

              @Override
              public void process(FixedKeyRecord<String, ProductAnalytic> record) {
                log.info("has_metadata={}, key={}", context.recordMetadata().isPresent(), record.key());
                context.forward(record);
              }
            }, "NONE_REPARTITIONED-aggregate-purchase-order")
            .mapValues(Streams::minimize)
            .to(options.getOutputTopic() + "-" + options.getWindowType(), Produced.as("NONE_REPARTITIONED-to"));


    KStream<String, ProductAnalytic> restore = builder
            .<String, ProductAnalytic>stream(options.getRepartitionTopicRestore(), Consumed.as("NONE_REPARTITIONED-restore"))
            .peek((k, v) -> log.info("!!!! key={}", k), Named.as("NONE_REPARTITIONED-restore-peek-incoming"))
            .processValues(() -> new FixedKeyProcessor<String, ProductAnalytic, ProductAnalytic>() {

              private FixedKeyProcessorContext<String, ProductAnalytic> context;
              private KeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store;

              @Override
              public void init(FixedKeyProcessorContext<String, ProductAnalytic> context) {
                this.context = context;
                store = context.getStateStore("NONE_REPARTITIONED-aggregate-purchase-order");
              }

              @Override
              public void process(FixedKeyRecord<String, ProductAnalytic> record) {

                if (record.headers().lastHeader("RESTORE") != null) {

                  log.info("RESTORE!!! ");

                  if (store.get(record.key()) == null) {
                    store.put(record.key(), ValueAndTimestamp.make(record.value(), record.timestamp()));
                    log.info("stored!");
                  }

                }
                //store.all();
                log.info(record.value().toString());

                context.forward(record);
              }

            }, "NONE_REPARTITIONED-aggregate-purchase-order");
//                .transformValues(() -> new ValueTransformerWithKey<String, ProductAnalytic, ProductAnalytic>() {
//
//                    private ProcessorContext context;
//                    private KeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store;
//
//                    @Override
//                    public void init(ProcessorContext context) {
//                        this.context = context;
//                        store = context.getStateStore("NONE_REPARTITIONED-aggregate-purchase-order");
//                    }
//
//                    @Override
//                    public ProductAnalytic transform(String key, ProductAnalytic value) {
//
//                        if (context.headers().lastHeader("RESTORE") != null) {
//
//                            log.info("RESTORE!!! ");
//
//                            if (store.get(key) == null) {
//                                store.put(key, ValueAndTimestamp.make(value, context.timestamp()));
//                                log.info("stored!");
//                            }
//
//                        }
//                        //store.all();
//                        log.info(value.toString());
//                        return value;
//                    }
//
//                    @Override
//                    public void close() {
//                    }
//                }, "NONE_REPARTITIONED-aggregate-purchase-order");


    // e2e
    if (INCLUDE_REREAD_TASK) {
      builder.<String, ProductAnalyticOut>stream(options.getOutputTopic() + "-" + options.getWindowType(), Consumed.as("NONE_REPARTITIONED-to-consumer"))
              .peek((k, v) -> log.debug("key={}", k), Named.as("NONE_REPARTITIONED-to-consumer-peek"))
//                    .transformValues(() -> new ValueTransformerWithKey<String, ProductAnalyticOut, ProductAnalyticOut>() {
//
//                        private ProcessorContext context;
//                        private KeyValueStore<String, ValueAndTimestamp<ProductAnalyticOut>> store;
//
//                        @Override
//                        public void init(ProcessorContext context) {
//                            this.context = context;
//                            store = context.getStateStore("NONE_REPARTITIONED-aggregate-purchase-order");
//                        }
//
//                        @Override
//                        public ProductAnalyticOut transform(String key, ProductAnalyticOut value) {
//
//                            store.all().forEachRemaining((v) -> {
//                                log.info("v: " + v.key);
//                                log.info("v: " + v.value);
//                            });
//                            log.info(value.toString());
//                            return value;
//                        }
//
//                        @Override
//                        public void close() {
//                        }
//                    }, "NONE_REPARTITIONED-aggregate-purchase-order")
      ;
    }

    return builder;
  }


  private static ProductAnalytic initialize() {
    return new ProductAnalytic();
  }

  private static ProductAnalytic aggregator(final String key, final PurchaseOrder value, final ProductAnalytic aggregate) {
    aggregate.setSku(key);
    aggregate.setQuantity(aggregate.getQuantity() + quantity(value, key));
    aggregate.addOrderId(value.getOrderId());
    aggregate.setTimestamp(value.getTimestamp());
    return aggregate;
  }

  // merge into left thinking this would keep the list of orderIds the same - but merge could go either way.
  private static ProductAnalytic mergeSessions(final String key, final ProductAnalytic left, final ProductAnalytic right) {

    log.debug("merging session windows for key={}", key);

    left.setQuantity(left.getQuantity() + right.getQuantity());
    left.getOrderIds().addAll(right.getOrderIds());

    if (left.getTimestamp() == null || right.getTimestamp().isAfter(left.getTimestamp())) {
      left.setTimestamp(right.getTimestamp());
    }

    return left;
  }

  private static long quantity(final PurchaseOrder value, final String sku) {
    return value.getItems().stream().filter(i -> i.getSku().equals(sku)).findFirst().map(i -> (long) i.getQuantity()).orElse(0L);
  }

  private static Properties toProperties(final Map<String, Object> map) {
    final Properties properties = new Properties();
    properties.putAll(map);
    return properties;
  }

  private static String convert(final Instant ts) {
    if (ts == null) {
      return null;
    }
    return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER_SSS);
  }

  private static ProductAnalyticOut minimize(final ProductAnalytic productAnalytic) {

    if (productAnalytic == null) {
      return null;
    }
    return ProductAnalyticOut.create(productAnalytic);
  }
}

