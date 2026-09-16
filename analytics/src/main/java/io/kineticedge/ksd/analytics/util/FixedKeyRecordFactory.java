package io.kineticedge.ksd.analytics.util;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class FixedKeyRecordFactory {

  // generic erasure, K and V are objects.
  private static final Class<?>[] ARGUMENTS = {Object.class, Object.class, Long.TYPE, Headers.class};

  private static final Constructor<?> constructor;
  static {
    try {
      constructor = FixedKeyRecord.class.getDeclaredConstructor(ARGUMENTS);
      constructor.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("unable to get the needed constructor from FixedKeyRecord.", e);
    }
  }

  private FixedKeyRecordFactory() {
  }

  @SuppressWarnings("unchecked")
  public static <K, V> FixedKeyRecord<K, V> create(final K key, final V value, final long timestamp, final Headers headers) {
    try {
      return (FixedKeyRecord<K, V>) constructor.newInstance(key, value, timestamp, headers);
    } catch (InstantiationException | IllegalAccessException |
             InvocationTargetException e) {
      throw new RuntimeException("unable to create FixedKeyRecord", e);
    }
  }
}