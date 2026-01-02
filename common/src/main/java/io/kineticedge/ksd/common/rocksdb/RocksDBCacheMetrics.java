package io.kineticedge.ksd.common.rocksdb;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.rocksdb.Cache;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public final class RocksDBCacheMetrics {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RocksDBCacheMetrics.class);

    //kafka_stream_state_block_cache_capacity
    //rocksdb_shared_cache_sharedcache_usagebytes
    // kafka-stream-shared-cache
    // kafka.stream.state:type=block-cache
    private static final String JMX_SHARED_CACHE_PREFIX = "rocksdb.shared.cache:type=block-cache,cache-id=%s";
    private static final String JMX_SHARED_CACHE_STORE_PREFIX = "rocksdb.shared.cache:type=block-cache-store,cache-id=%s,state-id=%s";

    private static final boolean JMX_ENABLED = true;
    private static final boolean MICROMETER_ENABLED = false;

    private RocksDBCacheMetrics() {
    }

    @SuppressWarnings("unused")
    public interface SharedCacheMBean {
        long getCapacity();
        long getUsage();
    }

    public static void registerJmx(final String cacheId, final Cache cache, final long capacity) {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName jmxName = new ObjectName(String.format(JMX_SHARED_CACHE_PREFIX, cacheId));
            if (server.isRegistered(jmxName)) {
                server.unregisterMBean(jmxName);
            }
            server.registerMBean(new javax.management.StandardMBean(new SharedCacheMBean() {
                @Override
                public long getCapacity() {
                    return capacity;
                }
                @Override
                public long getUsage() {
                    return cache.getUsage();
                }
            }, SharedCacheMBean.class), jmxName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register SharedRocksCache JMX MBean", e);
        }
    }

    public static void unregisterJmx(String cacheId) {
        try {
            ObjectName jmxName = new ObjectName(String.format(JMX_SHARED_CACHE_PREFIX, cacheId));
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (server.isRegistered(jmxName)) {
                server.unregisterMBean(jmxName);
            }
        } catch (Exception e) {
            log.warn("issues unregistering JMX Bean", e);
        }
    }

    @SuppressWarnings("unused")
    public interface StoreMetricsMBean {
        boolean isSharedCacheEnabled();
    }

    public static void register(String cacheId, String storeName) {
        if (JMX_ENABLED) {
            registerStoreJmx(cacheId, storeName);
        }
        if (MICROMETER_ENABLED) {
            registerMicrometer(storeName);
        }
    }

    public static void unregister(String cacheId, String storeName) {
        if (JMX_ENABLED) {
            unregisterStoreJmx(cacheId, storeName);
        }
        if (MICROMETER_ENABLED) {
            unregisterMicrometer(storeName);
        }
    }

    private static void registerStoreJmx(String cacheId, String storeName) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(String.format(JMX_SHARED_CACHE_STORE_PREFIX, cacheId, storeName));

            if (!server.isRegistered(name)) {
                server.registerMBean(
                        new javax.management.StandardMBean((StoreMetricsMBean) () -> true, StoreMetricsMBean.class),
                        name
                );
            }
//            if (server.isRegistered(name)) {
//                server.unregisterMBean(name);
//            }
//            server.registerMBean(new javax.management.StandardMBean(new StoreMetricsMBean() {
//                @Override
//                public boolean isSharedCacheEnabled() {
//                    return true;
//                }
//            }, StoreMetricsMBean.class), name);
        } catch (Exception e) {
            log.warn("Failed to register JMX bean for store {}", storeName, e);
        }
    }

    private static void unregisterStoreJmx(String cacheId, String storeName) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(String.format(JMX_SHARED_CACHE_STORE_PREFIX, cacheId, storeName));
            if (server.isRegistered(name)) {
                server.unregisterMBean(name);
            }
        } catch (Exception e) {
            log.warn("Failed to unregister JMX bean for store {}", storeName, e);
        }
    }



    // ---- Micrometer --------------------------------------------------------

    private static void registerMicrometer(String storeName) {
        Gauge.builder("rocksdb.shared.cache.enabled", () -> 1)
                .tag("state_id", storeName)
                .register(Metrics.globalRegistry);
    }

    private static void unregisterMicrometer(String storeName) {
        final Meter meter = Metrics.globalRegistry
                .find("rocksdb.shared.cache.enabled")
                .tag("state_id", storeName)
                .meter();
        if (meter != null) {
            Metrics.globalRegistry.remove(meter);
        }
    }

    //    private void registerMicrometer() {
//        if (!micrometerRegistered) {
//            Gauge.builder("rocksdb.shared.cache.capacity.bytes", (Supplier<Number>) () -> capacityBytes)
//                    .description("Shared RocksDB block cache capacity")
//                    .register(Metrics.globalRegistry);
//
//            Gauge.builder("rocksdb.shared.cache.usage.bytes", sharedCache, Cache::getUsage)
//                    .description("Shared RocksDB block cache usage")
//                    .register(Metrics.globalRegistry);
//        }
//        micrometerRegistered = true;
//    }
//
//    private void unregisterMicrometer() {
//        removeMeter("rocksdb.shared.cache.capacity.bytes");
//        removeMeter("rocksdb.shared.cache.usage.bytes");
//        micrometerRegistered = false;
//    }
}
