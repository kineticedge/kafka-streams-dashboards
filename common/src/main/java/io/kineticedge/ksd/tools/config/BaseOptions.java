package io.kineticedge.ksd.tools.config;

import com.beust.jcommander.Parameter;

public abstract class BaseOptions {

  @Parameter(names = "--help", help = true, hidden = true)
  private boolean help;

  @Parameter(names = {"-b", "--bootstrap-servers"}, description = "cluster bootstrap servers")
  private String bootstrapServers = "localhost:9092";

  @Parameter(names = {"--store-topic"}, description = "compacted topic holding stores")
  private String storeTopic = "orders-store";

  @Parameter(names = {"--user-topic"}, description = "compacted topic holding users")
  private String userTopic = "orders-user";

  @Parameter(names = {"--product-topic"}, description = "compacted topic holding products")
  private String productTopic = "orders-product";

  @Parameter(names = {"--purchase-topic"}, description = "")
  private String purchaseTopic = "orders-purchase";

  @Parameter(names = {"--pickup-topic"}, description = "")
  private String pickupTopic = "orders-pickup";

  @Parameter(names = {"--custom-metrics-topic"}, description = "custom metrics topic")
  private String customMetricsTopic = "_metrics-kafka-streams";

  @Parameter(names = {"--repartition-topic"})
  private String repartitionTopic = "pickup-order-handler-purchase-order-join-product-repartition";

  @Parameter(names = {"--repartition-topic-restore"})
  private String repartitionTopicRestore = "pickup-order-handler-purchase-order-join-product-repartition-restore";

  @Parameter(names = {"--output-topic-prefix"}, description = "")
  private String outputTopicPrefix = "product-statistics";

  // shared by builder and by producer, producer needs to honor whatever builder creates
  private int numberOfStores = 1000;
  private int numberOfUsers = 10_000;
  //private int numberOfUsers = 10;
  private int numberOfProducts = 10_000;
  //private int numberOfProducts = 20;
  private int maxQuantity = 10;

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public String getStoreTopic() {
    return storeTopic;
  }

  public String getUserTopic() {
    return userTopic;
  }

  public String getProductTopic() {
    return productTopic;
  }

  public String getPurchaseTopic() {
    return purchaseTopic;
  }

  public String getPickupTopic() {
    return pickupTopic;
  }

  public String getCustomMetricsTopic() {
    return customMetricsTopic;
  }

  public String getRepartitionTopic() {
    return repartitionTopic;
  }

  public String getRepartitionTopicRestore() {
    return repartitionTopicRestore;
  }

  public String getOutputTopicPrefix() {
    return outputTopicPrefix;
  }

  public int getNumberOfStores() {
    return numberOfStores;
  }

  public int getNumberOfUsers() {
    return numberOfUsers;
  }

  public int getNumberOfProducts() {
    return numberOfProducts;
  }

  public int getMaxQuantity() {
    return maxQuantity;
  }

  public boolean isHelp() {
    return help;
  }
}
