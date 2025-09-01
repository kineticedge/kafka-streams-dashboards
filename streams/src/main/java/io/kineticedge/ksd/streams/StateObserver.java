package io.kineticedge.ksd.streams;

import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.streams.domain.By;
import io.kineticedge.ksd.streams.domain.ByOrderId;
import io.kineticedge.ksd.streams.domain.PurchaseOrderSummary;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StateObserver {

  private static final Logger log = LoggerFactory.getLogger(StateObserver.class);

  private final KafkaStreams streams;
  private final String storeName;

  public StateObserver(final KafkaStreams streams) {
    this.streams = streams;
    this.storeName = "pickup-order-reduce-store";
  }

  public By getState(String type) {

    log.info("query: type={}", type);

      return populateKeyValue(new ByOrderId());
  }

//  private By populateWindow(By by) {
//    final ReadOnlyWindowStore<String, ProductAnalytic> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.windowStore()));
//    try (KeyValueIterator<Windowed<String>, ProductAnalytic> iterator = store.all()) {
//      iterator.forEachRemaining(i -> by.add(i.key.window(), i.value));
//    }
//    return by;
//  }
//
//  private By populateSession(By by) {
//    final ReadOnlySessionStore<String, ProductAnalytic> session = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.sessionStore()));
//    try (KeyValueIterator<Windowed<String>, ProductAnalytic> iterator = session.fetch("0000000000", "9999999999")) {
//      iterator.forEachRemaining(i -> by.add(i.key.window(), i.value));
//    }
//    return by;
//  }

  private By populateKeyValue(By by) {
    final ReadOnlyKeyValueStore<String, ValueAndTimestamp<PurchaseOrder>> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.timestampedKeyValueStore()));
    try (KeyValueIterator<String, ValueAndTimestamp<PurchaseOrder>> iterator = store.all()) {
      iterator.forEachRemaining(i -> by.add(PurchaseOrderSummary.create(i.value.value())));
    }
    return by;
  }


}
