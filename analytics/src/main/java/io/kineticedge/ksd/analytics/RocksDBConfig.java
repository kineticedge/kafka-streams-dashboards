
package io.kineticedge.ksd.analytics;

import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.CompressionType;
import org.rocksdb.MemTableConfig;
import org.rocksdb.Options;

import java.util.Map;

public class RocksDBConfig implements RocksDBConfigSetter {

    // This object should be a member variable so it can be closed in RocksDBConfigSetter#close.
    // Block cache caches uncompressed blocks.
    // OS cache, on the other hand, caches compressed blocks (since that's the way they are stored in files).
    // Default value is 50MB
    private org.rocksdb.Cache cache = new org.rocksdb.LRUCache(16 * 1024L * 1024L);

    @Override
    public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {

        System.out.println(">>> " + storeName);

        options.setWriteBufferSize(1);
//        options.setMaxWriteBufferNumber(0);


//        // BlockBasedTableConfig tableConfig = (BlockBasedTableConfig) options.tableFormatConfig();
//        // Get a reference to the existing TableFormatConfig rather than create a new one so you don’t accidentally
//        // overwrite defaults such as the BloomFilter, an important optimization.
//        BlockBasedTableConfig tableConfig = (BlockBasedTableConfig) options.tableFormatConfig();
//
//        // Rely on the previously created LRU cache for block cache
//        tableConfig.setBlockCache(cache);
//
//        // RocksDB packs user data in blocks.
//        // When reading a key-value pair from a table file, an entire block is loaded into memory.
//        // Block size is 4KB by default
//        tableConfig.setBlockSize(4 * 1024L);
//
//        // Index and filter blocks will be stored in block cache, together with all other data blocks.
//        // This also means they can be paged out.
 //        // If set to false, a dedicated cache will be provisioned for indexes and filter
//        tableConfig.setCacheIndexAndFilterBlocks(true);
//        options.setTableFormatConfig(tableConfig);
//
//
//    /*
//    All writes to RocksDB are first inserted into an in-memory data structure called memtable.
//    Once the active memtable is full, we create a new one and mark the old one read-only.
//    We call the read-only memtable immutable. At any point in time there is exactly one active memtable and zero or more immutable memtables.
//    Immutable memtables are waiting to be flushed to storage.
//    */
//
//
//        // max_write_buffer_number sets the maximum number of memtables, both active and immutable.
//        // If the active memtable fills up and the total number of memtables is larger than max_write_buffer_number we stall further writes.
//        // Default: 2
//        options.setMaxWriteBufferNumber(2);
//
//        // write_buffer_size sets the size of a single memtable.
//        // Once memtable exceeds this size, it is marked immutable and a new one is created.
//        // Default: 16MB
//        options.setWriteBufferSize(4 * 1024L * 1024L);
//
//        // enable LZ4 compression, this should decrease the required storage
//        // and increase the CPU usage of the machine
//        options.setCompressionType(CompressionType.LZ4_COMPRESSION);
    }

    @Override
    public void close(String storeName, Options options) {
        cache.close();
    }
}