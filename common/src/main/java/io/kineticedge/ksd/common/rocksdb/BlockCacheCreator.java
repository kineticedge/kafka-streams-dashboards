package io.kineticedge.ksd.common.rocksdb;

import org.rocksdb.Cache;
import org.rocksdb.LRUCache;
import org.rocksdb.RocksDB;

public final class BlockCacheCreator {

    static {
        RocksDB.loadLibrary();
    }

    // currently not configurable, will relax to configuration once the framework is in
    // place, and we understand what other configurations should be customizable.
    private static final int AUTOMATICALLY_DETERMINE_SHARD_SIZE = -1;
    private static final boolean NO_STRICT_CACHE_CAPACITY_LIMIT = false;
    private static final double INDEX_FILTER_BLOCK_RATIO = 0.1;

    private BlockCacheCreator() {
    }

    public static Cache createCache(long capacityBytes) {
        return new LRUCache(
                capacityBytes,
                AUTOMATICALLY_DETERMINE_SHARD_SIZE,
                NO_STRICT_CACHE_CAPACITY_LIMIT);
                //INDEX_FILTER_BLOCK_RATIO);
    }

}
