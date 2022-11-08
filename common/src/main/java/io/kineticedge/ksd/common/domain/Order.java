package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class Order {

    @Data
    public static class LineItem {
        private String sku;
        private int orderedQuantity;
        private int quantity;
        private BigDecimal quotedPrice;
        private BigDecimal price;
    }

    private String orderId;
    private String userId;
    private String storeId;

    private List<LineItem> items;

    private BigDecimal tax;

    @JsonIgnore
    public BigDecimal getTotal() {
        return null;
    }

}
