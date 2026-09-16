package io.kineticedge.ksd.analytics;

import io.kineticedge.ksd.analytics.domain.By;
import io.kineticedge.ksd.analytics.domain.BySku;
import io.kineticedge.ksd.analytics.domain.ByWindow;
import io.kineticedge.ksd.common.domain.ProductAnalytic;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlySessionStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

public class StateObserver {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StateObserver.class);

  private final KafkaStreams streams;

  private final Options.WindowType windowType;

  private final String storeName;

  public StateObserver(final KafkaStreams streams, final Options.WindowType windowType) {
    this.streams = streams;
    this.windowType = windowType;
    this.storeName = windowType.name() + "-aggregate-purchase-order";
  }

  public By getState(String type) {

    log.info("query: windowType={}, type={}", windowType, type);

    if ("windowing".equals(type)) {
      if (windowType == Options.WindowType.SESSION) {
        return populateSession(new ByWindow());
      } else if (windowType == Options.WindowType.NONE) {
        return populateKeyValue(new ByWindow());
      } else {
        return populateWindow(new ByWindow());
      }
    } else {
      if (windowType == Options.WindowType.SESSION) {
        return populateSession(new BySku());
      } else if (windowType == Options.WindowType.NONE) {
        return populateKeyValue(new BySku());
      } else {
        return populateWindow(new BySku());
      }
    }
  }

  private By populateWindow(By by) {
    final ReadOnlyWindowStore<String, ProductAnalytic> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.windowStore()));
    try (KeyValueIterator<Windowed<String>, ProductAnalytic> iterator = store.all()) {
      iterator.forEachRemaining(i -> by.add(i.key.window(), i.value));
    }
    return by;
  }

  private By populateSession(By by) {
    final ReadOnlySessionStore<String, ProductAnalytic> session = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.sessionStore()));
    try (KeyValueIterator<Windowed<String>, ProductAnalytic> iterator = session.fetch("0000000000", "9999999999")) {
      iterator.forEachRemaining(i -> by.add(i.key.window(), i.value));
    }
    return by;
  }

  private By populateKeyValue(By by) {
    final ReadOnlyKeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.timestampedKeyValueStore()));
    try (KeyValueIterator<String, ValueAndTimestamp<ProductAnalytic>> iterator = store.all()) {
      iterator.forEachRemaining(i -> by.add(null, i.value.value()));
    }
    return by;
  }


}
