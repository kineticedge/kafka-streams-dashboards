package io.kineticedge.ksd.streams.domain;

import io.kineticedge.ksd.common.domain.PurchaseOrder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseOrderSummary {

    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static class Item {
        private String sku;
        private String quantity;
        private String price;

      public Item(String sku, String quantity, String price) {
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
      }

      public String getSku() {
        return sku;
      }

      public String getQuantity() {
        return quantity;
      }

      public String getPrice() {
        return price;
      }
    }

    private String orderId;
    private String userId;
    private String userName;
    private String storeId;
    private String storeName;
    private List<Item> items;
    private String timestamp;

  public PurchaseOrderSummary(String orderId, String userId, String userName, String storeId, String storeName, List<Item> items, String timestamp) {
    this.orderId = orderId;
    this.userId = userId;
    this.userName = userName;
    this.storeId = storeId;
    this.storeName = storeName;
    this.items = items;
    this.timestamp = timestamp;
  }


  public static PurchaseOrderSummary create(final PurchaseOrder purchaseOrder
    ) {
        return new PurchaseOrderSummary(
                purchaseOrder.getOrderId(),
                purchaseOrder.getUserId(),
                purchaseOrder.getUser().name(),
                purchaseOrder.getStoreId(),
                purchaseOrder.getStore().name(),
                purchaseOrder.getItems().stream().map(i -> new Item(i.getSku(), Integer.toString(i.getQuantity()), "" + i.getPrice())).collect(Collectors.toList()),
                convert(purchaseOrder.getTimestamp())
        );
    }

    private static String convert(final Instant ts) {
        if (ts == null) {
            return null;
        }
        return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER_SSS);
    }

  public String getOrderId() {
    return orderId;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public String getStoreId() {
    return storeId;
  }

  public String getStoreName() {
    return storeName;
  }

  public List<Item> getItems() {
    return items;
  }

  public String getTimestamp() {
    return timestamp;
  }
}
