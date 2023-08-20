package io.kineticedge.ksd.analytics.util;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

import java.lang.reflect.Constructor;

public class FixedKeyRecordFactory {

  private static final Constructor<?> constructor;

  static {
    final Constructor<?>[] constructors = FixedKeyRecord.class.getDeclaredConstructors();
    if (constructors.length != 1) {
      throw new IllegalStateException("Unexpected number of constructors (expected 1, got %d)".formatted(constructors.length));
    }
    constructors[0].setAccessible(true);
    constructor = constructors[0];
  }

  private FixedKeyRecordFactory() {
  }

  @SuppressWarnings("unchecked")
  public static <K, V> FixedKeyRecord<K, V> create(final K key, final V value, final long timestamp, final Headers headers) {
    try {
      return (FixedKeyRecord<K, V>) constructor.newInstance(key, value, timestamp, headers);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create instance of %s".formatted(FixedKeyRecord.class.getName()), e);
    }
  }

}