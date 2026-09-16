package io.kineticedge.ksd.analytics.domain;

import io.kineticedge.ksd.common.domain.ProductAnalytic;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ProductAnalyticSummary(
        Window window,
        String sku,
        Long qty,
        List<String> orderIds,
        String timestamp
) {

    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static ProductAnalyticSummary create(final Window window, final ProductAnalytic productAnalytic) {
        return new ProductAnalyticSummary(
                window,
                productAnalytic.getSku(),
                productAnalytic.getQuantity(),
                productAnalytic.getOrderIds(),
                convert(productAnalytic.getTimestamp())
        );
    }

    private static String convert(final Instant ts) {
        if (ts == null) {
            return null;
        }
        return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER_SSS);
    }

}
