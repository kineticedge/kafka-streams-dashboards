package io.kineticedge.ksd.analytics.domain;

import io.kineticedge.ksd.common.domain.ProductAnalytic;

public interface By {

    void add(org.apache.kafka.streams.kstream.Window window, ProductAnalytic productAnalytic);
}
