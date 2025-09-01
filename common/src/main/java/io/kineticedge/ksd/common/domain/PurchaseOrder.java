package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class PurchaseOrder {

  public static class LineItem {
    private String sku;
    private int quantity;
    private BigDecimal quotedPrice;
    private BigDecimal price;

    public String getSku() {
      return sku;
    }

    public void setSku(String sku) {
      this.sku = sku;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }

    public BigDecimal getQuotedPrice() {
      return quotedPrice;
    }

    public void setQuotedPrice(BigDecimal quotedPrice) {
      this.quotedPrice = quotedPrice;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public void setPrice(BigDecimal price) {
      this.price = price;
    }
  }

  private Instant timestamp;
  private String orderId;
  private String userId;
  private String storeId;
  private List<LineItem> items;

  private BigDecimal tax;
  private User user;
  private Store store;

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getStoreId() {
    return storeId;
  }

  public void setStoreId(String storeId) {
    this.storeId = storeId;
  }

  public List<LineItem> getItems() {
    return items;
  }

  public void setItems(List<LineItem> items) {
    this.items = items;
  }

  public BigDecimal getTax() {
    return tax;
  }

  public void setTax(BigDecimal tax) {
    this.tax = tax;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Store getStore() {
    return store;
  }

  public void setStore(Store store) {
    this.store = store;
  }
}
