package io.kineticedge.ksd.common.rocksdb;

import org.rocksdb.Cache;

import java.util.HashSet;
import java.util.Set;

/**
 * A cache manager for RocksDB shared cache with metrics tracking, since caches can be shared
 * but Kafka Streams metrics does not treat them as shared, this manager handles the sharing
 * and metrics management.
 */
public class RocksDBCacheManager {

    private final Cache sharedCache;

    private final Set<String> stores = new HashSet<>();

    public RocksDBCacheManager(long capacityBytes) {
        this.sharedCache = BlockCacheCreator.createCache(capacityBytes);
        RocksDBCacheMetrics.registerJmx("shared", sharedCache, capacityBytes);
    }

    public void close() {
        stores.forEach(s -> RocksDBCacheMetrics.unregister("shared", s));
        stores.clear();
        RocksDBCacheMetrics.unregisterJmx("shared");
    }

    // would be possible to have separate shared stores at some point
    public Cache register(String storeName) {
        stores.add(storeName);
        RocksDBCacheMetrics.register("shared", storeName);
        return sharedCache;
    }

    public void unregister(String storeName) {
//        if (stores.contains(storeName)) {
//            // with stores that are created over time (windowed stores) removing/adding the store
//            // leads to 'no data' scenarios if scraping happens during unregister/registering. So,
//            // not removing metric -- which means if there was a way to dynamically change store configs
//            // at real-time, reporting could not properly reflect, but I do not expect that that
//            // will be the case
//            //RocksDBCacheMetrics.unregister("shared", storeName);
//            //stores.remove(storeName);
//        }
    }

}
