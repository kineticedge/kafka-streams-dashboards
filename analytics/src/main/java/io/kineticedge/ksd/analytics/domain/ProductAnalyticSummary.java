package io.kineticedge.ksd.analytics.domain;

import io.kineticedge.ksd.common.domain.ProductAnalytic;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@AllArgsConstructor
public class ProductAnalyticSummary {

    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Window window;
    private String sku;
    private Long qty;
    private List<String> orderIds;
    private String timestamp;

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
