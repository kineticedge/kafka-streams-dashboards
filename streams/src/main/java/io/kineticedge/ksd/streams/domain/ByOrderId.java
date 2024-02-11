package io.kineticedge.ksd.streams.domain;

import java.util.SortedMap;
import java.util.TreeMap;


public class ByOrderId implements By {

    private final TreeMap<String, PurchaseOrderSummary> records;

    public ByOrderId() {
        this.records = new TreeMap<>();
    }

    public void add(final PurchaseOrderSummary purchaseOrder) {
        records.put(purchaseOrder.getOrderId(), purchaseOrder);
    }

    public SortedMap<String, PurchaseOrderSummary> getRecords() {
        return records;
    }

}
