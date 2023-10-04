package io.kineticedge.ksd.builder;

import io.kineticedge.ksd.common.domain.Product;
import io.kineticedge.ksd.common.domain.Store;
import io.kineticedge.ksd.common.domain.User;
import io.kineticedge.ksd.common.domain.Zip;
import io.kineticedge.ksd.tools.serde.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class BuildSystem {

    private static final short REPLICATION_FACTOR = 3;
    private static final int PARTITIONS = 8;
    private static final Map<String, String> CONFIGS = Map.ofEntries(
            Map.entry("retention.ms", "86400000") // 1 day
    );


    private static final short GLOBAL_REPLICATION_FACTOR = 3;
    private static final int GLOBAL_PARTITIONS = 1;
    private static final Map<String, String> GLOBAL_CONFIGS = Map.ofEntries(
            Map.entry("cleanup.policy", "compact"),
            Map.entry("retention.ms", "-1")
    );

    private static final short METRICS_REPLICATION_FACTOR = 3;
    private static final int METRICS_PARTITIONS = 8;
    private static final Map<String, String> METRICS_CONFIGS = Map.ofEntries(
            Map.entry("retention.ms", "3600000"), // 1 hour
            Map.entry("min.insync.replicas", "1")
    );

    private static final Random RANDOM = new Random();

    private final Options options;
    private final List<Zip> zips = loadZipcodes();

    public BuildSystem(final Options options) {
        this.options = options;
    }

    public void start() {
        createTopics();
        populateTables();
    }

    private void createTopics() {

        final AdminClient admin = KafkaAdminClient.create(properties(options));

        NewTopic store = new NewTopic(options.getStoreTopic(), GLOBAL_PARTITIONS, GLOBAL_REPLICATION_FACTOR);
        store.configs(GLOBAL_CONFIGS);

        NewTopic user = new NewTopic(options.getUserTopic(), PARTITIONS, REPLICATION_FACTOR);
        user.configs(GLOBAL_CONFIGS);

        NewTopic product = new NewTopic(options.getProductTopic(), PARTITIONS, REPLICATION_FACTOR);
        product.configs(GLOBAL_CONFIGS);

        NewTopic purchaseOrders = new NewTopic(options.getPurchaseTopic(), PARTITIONS, REPLICATION_FACTOR);
        purchaseOrders.configs(CONFIGS);

        NewTopic pickupOrders = new NewTopic(options.getPickupTopic(), PARTITIONS, REPLICATION_FACTOR);
        pickupOrders.configs(CONFIGS);

        NewTopic repartition = new NewTopic(options.getRepartitionTopic(), PARTITIONS, REPLICATION_FACTOR);
        repartition.configs(CONFIGS);

        NewTopic repartitionAgain = new NewTopic(options.getRepartitionTopic() + "-REPARTITIONED", PARTITIONS * 2, REPLICATION_FACTOR);
        repartitionAgain.configs(CONFIGS);

        NewTopic repartitionRestore = new NewTopic(options.getRepartitionTopicRestore(), PARTITIONS * 2, REPLICATION_FACTOR);
        repartitionRestore.configs(CONFIGS);

        NewTopic outputTopicTumbling = new NewTopic(options.getOutputTopicPrefix() + "-TUMBLING", PARTITIONS, REPLICATION_FACTOR);
        outputTopicTumbling.configs(CONFIGS);

        NewTopic outputTopicHopping = new NewTopic(options.getOutputTopicPrefix() + "-HOPPING", PARTITIONS, REPLICATION_FACTOR);
        outputTopicHopping.configs(CONFIGS);

        NewTopic outputTopicSliding = new NewTopic(options.getOutputTopicPrefix() + "-SLIDING", PARTITIONS, REPLICATION_FACTOR);
        outputTopicSliding.configs(CONFIGS);

        NewTopic outputTopicSession = new NewTopic(options.getOutputTopicPrefix() + "-SESSION", PARTITIONS, REPLICATION_FACTOR);
        outputTopicSession.configs(CONFIGS);

        NewTopic outputTopicNone = new NewTopic(options.getOutputTopicPrefix() + "-NONE", PARTITIONS, REPLICATION_FACTOR);
        outputTopicNone.configs(CONFIGS);


        NewTopic customMetricsTopic = new NewTopic(options.getCustomMetricsTopic(), METRICS_PARTITIONS, METRICS_REPLICATION_FACTOR);
        customMetricsTopic.configs(METRICS_CONFIGS);

        final List<NewTopic> topics = Arrays.asList(
                store,
                user,
                product,
                purchaseOrders,
                pickupOrders,
                repartition,
                repartitionAgain,
                repartitionRestore,
                outputTopicTumbling,
                outputTopicHopping,
                outputTopicSliding,
                outputTopicSession,
                outputTopicNone,
                customMetricsTopic
        );

        if (options.isDeleteTopics()) {
            admin.deleteTopics(topics.stream().map(NewTopic::name).collect(Collectors.toList())).topicNameValues().forEach((k, v) -> {
                try {
                    v.get();
                } catch (final InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                        log.debug("not deleting a topic that doesn't exist, topic={}", k);
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        admin.createTopics(topics).values().forEach((k, v) -> {
            try {
                v.get();
            } catch (final InterruptedException | ExecutionException e) {
                if (e.getCause() instanceof TopicExistsException) {
                    log.warn("{}", e.getCause().getMessage());
                } else {
                    throw new RuntimeException(e);
                }
            }
        });

//        admin.describeTopics(topics.stream().map(NewTopic::name).collect(Collectors.toList())).values().forEach((k, v) -> {
//            try {
//                TopicDescription description = v.get();
//                //TODO -- validate
//            } catch (final InterruptedException | ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        });

    }

    private void populateTables() {

        Map<String, Object> properties = new HashMap<>(properties(options));
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 10L);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 20_000);

        final KafkaProducer<String, Object> kafkaProducer = new KafkaProducer<>(properties);

        IntStream.range(0, options.getNumberOfStores()).forEach(i -> {
            Store store = getRandomStore(i);

            log.info("Sending key={}, value={}", store.getStoreId(), store);
            kafkaProducer.send(new ProducerRecord<>(options.getStoreTopic(), null, store.getStoreId(), store), (metadata, exception) -> {
                if (exception != null) {
                    log.error("error producing to kafka", exception);
                } else {
                    log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        });

        kafkaProducer.flush();

        IntStream.range(0, options.getNumberOfUsers()).forEach(i -> {
            User user = getRandomUser(i);

            log.info("Sending key={}, value={}", user.getUserId(), user);
            kafkaProducer.send(new ProducerRecord<>(options.getUserTopic(), null, user.getUserId(), user), (metadata, exception) -> {
                if (exception != null) {
                    log.error("error producing to kafka", exception);
                } else {
                    log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        });

        kafkaProducer.flush();

        IntStream.range(0, options.getNumberOfProducts()).forEach(i -> {
            Product product = getRandomProduct(i);

            log.info("Sending key={}, value={}", product.getSku(), product);
            kafkaProducer.send(new ProducerRecord<>(options.getProductTopic(), null, product.getSku(), product), (metadata, exception) -> {
                if (exception != null) {
                    log.error("error producing to kafka", exception);
                } else {
                    log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        });

        kafkaProducer.flush();
        kafkaProducer.close();
    }

    private Map<String, Object> properties(final Options options) {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),

//                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092"),
//                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT"),
//                Map.entry(SaslConfigs.SASL_MECHANISM, "PLAIN"),
//                Map.entry(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kafka-admin\" password=\"kafka-admin-secret\";"),

                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName())
        );
    }

    private User getRandomUser(int userId) {
        final User user = new User();

        user.setUserId(Integer.toString(userId));
        user.setName(generateName());
        user.setEmail(user.getName() + "@foo.com");

        return user;
    }

    private Product getRandomProduct(int sku) {
        final Product product = new Product();

        product.setSku(StringUtils.leftPad(Integer.toString(sku), 10, '0'));
        product.setPrice(BigDecimal.valueOf(RANDOM.nextDouble() * 100.).setScale(2, RoundingMode.HALF_EVEN));

        return product;
    }

    private Store getRandomStore(int storeId) {
        final Store store = new Store();

        store.setStoreId(Integer.toString(storeId));
        store.setName(generateName());

        Zip zip = getRandomZip();

        store.setPostalCode(zip.getZip());
        store.setCity(zip.getCity());
        store.setState(zip.getState());

        return store;
    }

    private Zip getRandomZip() {
        return zips.get(RANDOM.nextInt(zips.size()));
    }

    private static List<Zip> loadZipcodes() {

        //final Map<String, Zip> map = new HashMap<>();
        final List<Zip> list = new ArrayList<>();

        try {

            CSVFormat format = CSVFormat.RFC4180.withHeader().withDelimiter(',');

            InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("zipcodes.csv");
            InputStreamReader reader = new InputStreamReader(input);

            CSVParser parser = new CSVParser(reader, format);

            for (CSVRecord record : parser) {
                final Zip zip = new Zip();
                zip.setZip(record.get("zipcode"));
                zip.setCity(record.get("city"));
                zip.setState(record.get("state_abbr"));
                list.add(zip);
            }

            parser.close();

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    private static String generateName() {
        return StringUtils.capitalize(ADJECTIVE[RANDOM.nextInt(ADJECTIVE.length)]) + " " + StringUtils.capitalize(NAMES[RANDOM.nextInt(NAMES.length)]);
    }

    private static final String[] ADJECTIVE = {
            "affable",
            "agreeable",
            "ambitious",
            "amusing",
            "brave",
            "bright",
            "calm",
            "careful",
            "charming",
            "courteous",
            "creative",
            "decisive",
            "determined",
            "diligent",
            "discreet",
            "dynamic",
            "emotional",
            "energetic",
            "exuberant",
            "faithful",
            "fearless",
            "forceful",
            "friendly",
            "funny",
            "generous",
            "gentle",
            "good",
            "helpful",
            "honest",
            "humorous",
            "impartial",
            "intuitive",
            "inventive",
            "loyal",
            "modest",
            "neat",
            "nice",
            "optimistic",
            "patient",
            "persistent",
            "placid",
            "plucky",
            "polite",
            "powerful",
            "practical",
            "quiet",
            "rational",
            "reliable",
            "reserved",
            "sensible",
            "sensitive",
            "shy",
            "sincere",
            "sociable",
            "tidy",
            "tough",
            "versatile",
            "willing",
            "witty"
    };


    private final static String[] NAMES = {

            "Broker",
            "Cleaner",
            "Cluster",
            "CommitLog",
            "Connect",
            "Consumer",
            "Election",
            "Jitter",
            "KIP",
            "Kafka",
            "Leader",
            "Listener",
            "Message",
            "Offset",
            "Partition",
            "Processor",
            "Producer",
            "Protocol",
            "Purgatory",
            "Quota",
            "Replica",
            "Retention",
            "RocksDB",
            "Segment",
            "Schema",
            "Streams",
            "Subject",
            "Timeout",
            "Tombstone",
            "Topic",
            "Topology",
            "Transaction",
            "Zombie",
            "Zookeeper"
    };
}
