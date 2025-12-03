package io.kineticedge.ksd.analytics;

import io.kineticedge.io.ksd.observer.Observer;
import io.kineticedge.ksd.common.domain.Product;
import io.kineticedge.ksd.common.domain.ProductAnalytic;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.query.QueryResult;
import org.apache.kafka.streams.query.StateQueryRequest;
import org.apache.kafka.streams.query.StateQueryResult;
import org.apache.kafka.streams.query.WindowRangeQuery;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AiAnalysis {

  Observer observer = new Observer();

  record Window(Instant start, Instant end) {
  }

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiAnalysis.class);

  private final KafkaStreams streams;
  private final Duration windowSize;

  public AiAnalysis(final KafkaStreams streams, final Duration windowSize) {
    this.streams = streams;
    this.windowSize = windowSize;
  }


  public void inquire() {
    try {

//        StateQueryRequest<KeyValueIterator<String, Product>> globalRequest =
//                StateQueryRequest.inStore("product").withQuery(RangeQuery.withNoBounds());
//
//        StateQueryResult<KeyValueIterator<String, Product>> globalResult =
//                streams.query(globalRequest);

      final Map<String, Product> products = new HashMap<>();
      final ReadOnlyKeyValueStore<String, Product> x = streams.store(StoreQueryParameters.fromNameAndType("product", QueryableStoreTypes.keyValueStore()));
      try (var iterator = x.all()) {
        while (iterator.hasNext()) {
          KeyValue<String, Product> next = iterator.next();
          //tombstone as needed logic
          products.put(next.key, next.value);
        }
      }

      final List<ProductAnalytic> pa = new ArrayList<>();

      final Window current = currentWindow(windowSize);
      StateQueryRequest<KeyValueIterator<Windowed<String>, ValueAndTimestamp<ProductAnalytic>>> request =
              StateQueryRequest.inStore("TUMBLING-aggregate-purchase-order")
                      .withQuery(WindowRangeQuery.withWindowStartRange(current.start(), current.end()));

      StateQueryResult<KeyValueIterator<Windowed<String>, ValueAndTimestamp<ProductAnalytic>>> result =
              streams.query(request);

      for (QueryResult<KeyValueIterator<Windowed<String>, ValueAndTimestamp<ProductAnalytic>>> partitionResult :
              result.getPartitionResults().values()) {

        if (partitionResult.isSuccess()) {
          try (KeyValueIterator<Windowed<String>, ValueAndTimestamp<ProductAnalytic>> iterator =
                       partitionResult.getResult()) {

            System.out.println(">>>>>\n");
            while (iterator.hasNext()) {
              KeyValue<Windowed<String>, ValueAndTimestamp<ProductAnalytic>> next = iterator.next();
              Windowed<String> windowedKey = next.key;
              ProductAnalytic value = next.value.value();
              System.out.println("Key: " + windowedKey.key() +
                      ", Window: " + windowedKey.window() +
                      ", Value: " + value);

              pa.add(value);
            }
          }
        } else {
          System.err.println("Query failed for partition: " + partitionResult.getFailureReason());
        }

      }


      pa.sort(Comparator.comparingLong(ProductAnalytic::getQuantity));
      pa.reversed();

      StringBuilder sb = new StringBuilder();

      sb.append("predict trends on purchasing, considering previous window purchases\n");
      sb.append("top-5 quantity for current window:\n");
      sb.append(String.format("window [%s, %s)\n", current.start(), current.end()));
      pa.subList(0, Math.min(pa.size(), 5)).forEach(p -> {
        Product xx = products.get(p.getSku());
        sb.append(String.format("sku=%s, qty=%d, attrs=%s\n", p.getSku(), p.getQuantity(), xx.attributes()));
      });

      System.out.println(">>>>");
      System.out.println(sb.toString());

      observer.inquire(sb.toString());


    } catch (Exception e) {
      e.printStackTrace();
    }



  }



  private Window currentWindow(Duration windowSize) {
    long now = System.currentTimeMillis();
    long windowSizeMs = windowSize.toMillis();
    long windowStart = (now / windowSizeMs) * windowSizeMs;
    return new Window(Instant.ofEpochMilli(windowStart), Instant.ofEpochMilli(windowStart + windowSizeMs));
  }

}
