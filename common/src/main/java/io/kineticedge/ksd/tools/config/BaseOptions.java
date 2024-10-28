package io.kineticedge.ksd.tools.config;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseOptions {

    @Parameter(names = "--help", help = true, hidden = true)
    private boolean help;

    @Parameter(names = { "-b", "--bootstrap-servers" }, description = "cluster bootstrap servers")
    private String bootstrapServers = "localhost:9092";

    @Parameter(names = { "--store-topic" }, description = "compacted topic holding stores")
    private String storeTopic = "orders-store";

    @Parameter(names = { "--user-topic" }, description = "compacted topic holding users")
    private String userTopic = "orders-user";

    @Parameter(names = { "--product-topic" }, description = "compacted topic holding products")
    private String productTopic = "orders-product";

    @Parameter(names = { "--purchase-topic" }, description = "")
    private String purchaseTopic = "orders-purchase";

    @Parameter(names = { "--pickup-topic" }, description = "")
    private String pickupTopic = "orders-pickup";

    @Parameter(names = { "--custom-metrics-topic" }, description = "custom metrics topic")
    private String customMetricsTopic = "_metrics-kafka-streams";

    @Parameter(names = { "--repartition-topic" })
    private String repartitionTopic = "pickup-order-handler-purchase-order-join-product-repartition";

    @Parameter(names = { "--repartition-topic-restore" })
    private String repartitionTopicRestore = "pickup-order-handler-purchase-order-join-product-repartition-restore";

    @Parameter(names = { "--output-topic-prefix" }, description = "")
    private String outputTopicPrefix = "product-statistics";

    // shared by builder and by producer, producer needs to honor whatever builder creates
    private int numberOfStores = 1000;
    private int numberOfUsers = 10_000;
    //private int numberOfUsers = 10;
    private int numberOfProducts = 10_000;
    //private int numberOfProducts = 20;
    private int maxQuantity = 10;

}
