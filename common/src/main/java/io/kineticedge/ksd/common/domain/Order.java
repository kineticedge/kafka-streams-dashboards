package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "$type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Order(
        String orderId,
        String userId,
        String storeId,
        List<LineItem> items,
        BigDecimal tax
) {

  public record LineItem(
          String sku,
          int orderedQuantity,
          int quantity,
          BigDecimal quotedPrice,
          BigDecimal price
  ) {
  }

  @JsonIgnore
  public BigDecimal getTotal() {
    return null;
  }

}
