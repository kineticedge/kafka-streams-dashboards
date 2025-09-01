package io.kineticedge.ksd.streams.domain;

import io.kineticedge.ksd.common.domain.PurchaseOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class PurchaseOrderSummary {

    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Getter
    @AllArgsConstructor
    private static class Item {
        private String sku;
        private String quantity;
        private String price;
    }

    private String orderId;
    private String userId;
    private String userName;
    private String storeId;
    private String storeName;
    private List<Item> items;
    private String timestamp;


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

}
