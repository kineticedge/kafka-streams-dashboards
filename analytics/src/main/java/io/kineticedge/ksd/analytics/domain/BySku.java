package io.kineticedge.ksd.analytics.domain;

import io.kineticedge.ksd.common.domain.ProductAnalytic;

import java.util.TreeMap;


public class BySku implements By {

    private TreeMap<String, TreeMap<Window, ProductAnalyticSummary>> records;

    public BySku() {
        this.records = new TreeMap<>();
    }


    public void add(final org.apache.kafka.streams.kstream.Window kstreamWindow, final ProductAnalytic productAnalytic) {
        final Window window = Window.convert(kstreamWindow);

        final String sku = productAnalytic.getSku();

        if (!records.containsKey(sku)) {
            records.put(productAnalytic.getSku(), new TreeMap<>());
        }

        productAnalytic.setSku(window.toString());

        records.get(sku).put(window, ProductAnalyticSummary.create(window, productAnalytic));
    }

    public TreeMap<String, TreeMap<Window, ProductAnalyticSummary>> getRecords() {
        return records;
    }

}
