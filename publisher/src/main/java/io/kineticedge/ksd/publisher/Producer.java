package io.kineticedge.ksd.publisher;

import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.tools.serde.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Producer {

    private static final Random RANDOM = new Random();

    private static final String ORDER_PREFIX = RandomStringUtils.randomAlphabetic(2).toUpperCase(Locale.ROOT);

    private final Options options;

    public Producer(final Options options) {
        this.options = options;
    }

    private String getRandomSku(int index) {

        if (options.getSkus() == null) {
            return StringUtils.leftPad(Integer.toString(RANDOM.nextInt(options.getMaxSku())), 10, '0');
        } else {

            final int productId = options.getSkus().get(index);

            if (productId < 0 || productId >= options.getMaxSku()) {
                throw new IllegalArgumentException("invalid product number");
            }

            return StringUtils.leftPad(Integer.toString(productId), 10, '0');
        }
    }

    private String getRandomUser() {
        return Integer.toString(RANDOM.nextInt(options.getNumberOfUsers()));
    }

    private String getRandomStore() {
        return Integer.toString(RANDOM.nextInt(options.getNumberOfStores()));
    }

    private int getRandomItemCount() {

        if (options.getLineItemCount().indexOf('-') < 0) {
            return Integer.parseInt(options.getLineItemCount());
        } else {
            String[] split = options.getLineItemCount().split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            return RANDOM.nextInt(max + 1 - min) + min;
        }
    }

    private int getRandomQuantity() {
        return RANDOM.nextInt(options.getMaxQuantity()) + 1;
    }

    private static int counter = 0;

    private static String orderNumber() {
        return ORDER_PREFIX + "-" + (counter++);
    }

    private PurchaseOrder createPurchaseOrder() {
        PurchaseOrder purchaseOrder = new PurchaseOrder();

        purchaseOrder.setTimestamp(Instant.now());
        purchaseOrder.setOrderId(orderNumber());
        purchaseOrder.setUserId(getRandomUser());
        purchaseOrder.setStoreId(getRandomStore());
        purchaseOrder.setItems(IntStream.range(0, getRandomItemCount())
                .boxed()
                .map(i -> {
                    final PurchaseOrder.LineItem item = new PurchaseOrder.LineItem();
                    item.setSku(getRandomSku(i));
                    item.setQuantity(getRandomQuantity());
                    item.setQuotedPrice(null); // TODO remove from domain
                    return item;
                })
                .collect(Collectors.toList())
        );

        return purchaseOrder;
    }

    public void start() {

//        if (options.getSkus() != null && options.getSkus().size() != options.getLineItemCount()) {
//            System.out.println("XXXX");
//            throw new IllegalArgumentException("!=");
//        }


        final KafkaProducer<String, PurchaseOrder> kafkaProducer = new KafkaProducer<>(properties(options));

        int count = 0;
        while (true) {

            PurchaseOrder purchaseOrder = createPurchaseOrder();

            log.info("Sending key={}, value={}", purchaseOrder.getOrderId(), purchaseOrder);
            kafkaProducer.send(new ProducerRecord<>(options.getPurchaseTopic(), null, purchaseOrder.getTimestamp().toEpochMilli(), purchaseOrder.getOrderId(), purchaseOrder), (metadata, exception) -> {
                if (exception != null) {
                    log.error("error producing to kafka", exception);
                } else {
                    log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            try {
                long pause = options.getPause();
                if (options.getPauses() != null) {
                    pause = options.getPauses().get(count % options.getPauses().size());
                }
                log.info("pausing for={}", pause);
                Thread.sleep(pause);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            count++;
        }

        // kafkaProducer.close();

    }

//    private static final Map<String, Object> defaults =  Map.ofEntries(
//            Map.entry(ProducerConfig.ACKS_CONFIG, "all"),
//            Map.entry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip")
//    );
//
//    private static final Map<String, Object> immutables =  Map.ofEntries(
//            Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
//            Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
//            );
//
//    private Map<String, Object> loadOverrides() {
//        final Map<String, Object> map = new HashMap<>();
//        //
//        return map;
//    }
//
//    private Map<String, Object> loadFromEnvironment() {
//        final Map<String, Object> map = new HashMap<>();
//        //
//        return map;
//    }
//
//    private Map<String, Object> clientProperties(final Options options) {
//
//
//        Map<String, Object> map = new HashMap<>(defaults);
//
//        map.putAll(PropertiesUtil.load("./app.properties"));
//
//        // environment wins
//        map.putAll(KafkaEnvUtil.to("STREAMS_"));
//
//        // Load in the connection settings at the end from a property file. Do not try to build `sasl.jaas.config`,
//        // instead set the entire string -- less work than building in the means to add username/password and
//        // allows for changing from sasl PLAIN to OAUTHBEARER w/out any coding changes, just all within these properties.
//        //
//        // bootstrap.servers=broker-1:9092,broker-2:9092
//        // security.protocols=SASL_SSL
//        // sasl.mechanism=PLAIN
//        // sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka-admin" password="kafka-admin-secret";
//        // ssl.truststore.location=/mnt/secrets/truststore.jks
//        // ssl.truststore.password=truststore_secret
//        //
//        map.putAll(PropertiesUtil.load("/mnt/secrets/connection.properties"));
//
//        map.putAll(overrides);
//
//        map.putAll(secrets);
//
//        map.putAll(immutables);
//
//        return map;
//    }

    private Map<String, Object> properties(final Options options) {
        Map<String, Object> defaults =  Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),

//                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092"),
//                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT"),
//                Map.entry(SaslConfigs.SASL_MECHANISM, "PLAIN"),
//                Map.entry(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka-admin\" password=\"kafka-admin-secret\";"),

                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
                Map.entry(ProducerConfig.ACKS_CONFIG, "all"),
                Map.entry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip")
        );

        Map<String, Object> map = new HashMap<>(defaults);

        /*
        try {
            Class.forName("io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");

            log.info("adding confluent interceptors, since package is on the classpath.");

            map.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");

            map.put("confluent.monitoring.interceptor.bootstrap.servers",
                    options.getBootstrapServers());

        } catch (Throwable t) {
            log.info("confluent interceptors not added, as library is not on the classpath.", t);
        }
        */

        return map;
    }

    private static void dumpRecord(final ConsumerRecord<String, String> record) {
        log.info("Record:\n\ttopic     : {}\n\tpartition : {}\n\toffset    : {}\n\tkey       : {}\n\tvalue     : {}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}
