package io.kineticedge.ksd.common.rocksdb;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.rocksdb.Cache;

public final class RocksDBCacheMetricsMicrometer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RocksDBCacheMetricsMicrometer.class);

    private RocksDBCacheMetricsMicrometer() {
    }

    public static void registerCache(final String cacheId, final Cache sharedCache, final long capacityBytes) {
        //if (!micrometerRegistered) {
        Gauge.builder("rocksdb.shared.cache.capacity.bytes", (java.util.function.Supplier<Number>) () -> capacityBytes)
                .tag("cache_id", cacheId)
                .description("Shared RocksDB block cache capacity")
                .register(Metrics.globalRegistry);

        Gauge.builder("rocksdb.shared.cache.usage.bytes", sharedCache, Cache::getUsage)
                .tag("cache_id", cacheId)
                .description("Shared RocksDB block cache usage")
                .register(Metrics.globalRegistry);
    }

    public static void unregisterCache(final String cacheId) {

        Meter strayCapacity = Metrics.globalRegistry.find("rocksdb.shared.cache.capacity.bytes")
                .tag("cache_id", cacheId)
                .meter();
        if (strayCapacity != null) {
            Metrics.globalRegistry.remove(strayCapacity);
        }

        Meter strayUsage = Metrics.globalRegistry.find("rocksdb.shared.cache.usage.bytes")
                .tag("cache_id", cacheId)
                .meter();
        if (strayUsage != null) {
            Metrics.globalRegistry.remove(strayUsage);
        }
    }

    public static void registerStore(String cacheId, String storeName) {
        Gauge.builder("rocksdb.shared.cache.enabled", () -> 1)
                .tag("cache_id", cacheId)
                .tag("state_id", storeName)
                .register(Metrics.globalRegistry);
    }

    public static void unregisterStore(String cacheId, String storeName) {
        final Meter meter = Metrics.globalRegistry
                .find("rocksdb.shared.cache.enabled")
                .tag("cache_id", cacheId)
                .tag("state_id", storeName)
                .meter();
        if (meter != null) {
            Metrics.globalRegistry.remove(meter);
        }
    }

}
