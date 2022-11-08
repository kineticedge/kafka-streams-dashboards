package io.kineticedge.ksd.analytics.domain;

import io.kineticedge.ksd.common.domain.ProductAnalytic;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
public class ByWindow implements By {

    private LocalDateTime toLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("America/Chicago"))
                .toLocalDateTime();
    }

    private String toDate(final LocalDateTime time) {
        return DateTimeFormatter.ofPattern("MM/dd/yy").format(time);
    }

    private String toTime(final LocalDateTime time) {
        return DateTimeFormatter.ofPattern("hh:mm:ss a").format(time);
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TreeMap<Window, TreeMap<String, ProductAnalyticSummary>> records;

    public ByWindow() {
        this.records = new TreeMap<>();
    }

    public void add(final org.apache.kafka.streams.kstream.Window kstreamWindow, final ProductAnalytic productAnalytic) {
        final Window window = Window.convert(kstreamWindow);
        if (!records.containsKey(window)) {
            records.put(window, new TreeMap<>());
        }
        records.get(window).put(productAnalytic.getSku(), ProductAnalyticSummary.create(window, productAnalytic));
    }


//    @JsonSerialize(keyUsing = MyKeySerializer.class) // no need of converter
    public TreeMap<Window, TreeMap<String, ProductAnalyticSummary>> getRecords() {
        return records;
    }

//    public static void main(String[] args) throws  Exception{
//
//        ByWindow byWindow = new ByWindow();
//
//        ProductAnalytic productAnalytic = new ProductAnalytic();
//        productAnalytic.setSku("A");
//        productAnalytic.setQuantity(10L);
//        productAnalytic.setOrderIds(List.of("1", "2"));
//        productAnalytic.setTimestamp(Instant.now());
//        byWindow.add(new TimeWindow(System.currentTimeMillis(), System.currentTimeMillis()+1000), productAnalytic);
//
//         ObjectMapper objectMapper =
//                new ObjectMapper()
//                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//                        .registerModule(new SimpleModule("uuid-module", new Version(1, 0, 0, null, "", ""))
//                                .addSerializer(ByWindow.class, new ByWindowSerializer())
//                        )
//                        .registerModule(new JavaTimeModule());
//
//
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(byWindow));
//    }
}





