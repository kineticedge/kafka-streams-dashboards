package io.kineticedge.ksd.analytics.domain;

import io.kineticedge.ksd.common.domain.ProductAnalytic;
import java.util.TreeMap;


public class ByFoo implements By {

    private TreeMap<String, ProductAnalyticSummary> records;

    public ByFoo() {
        this.records = new TreeMap<>();
    }


    public void add(final org.apache.kafka.streams.kstream.Window kstreamWindow, final ProductAnalytic productAnalytic) {
        records.put(productAnalytic.getSku(), ProductAnalyticSummary.create(null, productAnalytic));
    }

    public TreeMap<String, ProductAnalyticSummary> getRecords() {
        return records;
    }

}
