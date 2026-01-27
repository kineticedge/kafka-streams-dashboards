package io.kineticedge.ksd.common.rocksdb;

import org.rocksdb.Cache;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public final class RocksDBCacheMetricsJmx {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RocksDBCacheMetricsJmx.class);

    private static final String JMX_SHARED_CACHE_PREFIX = "rocksdb.shared.cache:type=block-cache,cache-id=%s";
    private static final String JMX_SHARED_CACHE_STORE_PREFIX = "rocksdb.shared.cache:type=block-cache-store,cache-id=%s,state-id=%s";

    private RocksDBCacheMetricsJmx() {
    }

    @SuppressWarnings("unused")
    public interface SharedCacheMBean {
        long getCapacity();
        long getUsage();
    }

    public interface StoreMetricsMBean {
        boolean isSharedCacheEnabled();
    }


    public static void registerCache(final String cacheId, final Cache cache, final long capacity) {
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

    public static void unregisterCache(String cacheId) {
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

    public static void registerStore(String cacheId, String storeName) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(String.format(JMX_SHARED_CACHE_STORE_PREFIX, cacheId, storeName));

            if (!server.isRegistered(name)) {
                server.registerMBean(
                        new javax.management.StandardMBean((StoreMetricsMBean) () -> true, StoreMetricsMBean.class),
                        name
                );
            }
        } catch (Exception e) {
            log.warn("Failed to register JMX bean for store {}", storeName, e);
        }
    }

    public static void unregisterStore(String cacheId, String storeName) {
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

}
