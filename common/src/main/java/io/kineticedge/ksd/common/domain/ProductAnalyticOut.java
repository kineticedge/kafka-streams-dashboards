package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public record ProductAnalyticOut(
        String sku,
        Long qty,
        String ts
) {

    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static ProductAnalyticOut create(final ProductAnalytic productAnalytic) {
        return new ProductAnalyticOut(productAnalytic.getSku(), productAnalytic.getQuantity(), convert(productAnalytic.getTimestamp()));
    }

    private static String convert(final Instant ts) {
        if (ts == null) {
            return null;
        }
        return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER_SSS);
    }

}
