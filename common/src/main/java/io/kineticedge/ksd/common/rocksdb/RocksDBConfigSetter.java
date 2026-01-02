package io.kineticedge.ksd.common.rocksdb;

import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.Options;
import org.rocksdb.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;


/**
 * This {@link RocksDBConfigSetter} implementation allows fine-tuning of RocksDB instances.
 * It also allows managing shared memory across all RocksDB instances and to enable dump of RocksDB statistics.
 */
public class RocksDBConfigSetter implements org.apache.kafka.streams.state.RocksDBConfigSetter {

    private static final Logger log = LoggerFactory.getLogger(RocksDBConfigSetter.class);

    /**
     * Matches segments like ".1766580780000" or ".1766580780000.1766580780000" at the end of a string.
     * Restricts the numeric part to 10-20 digits to avoid matching version numbers like ".v1" or ".1".
     */
    private static final Pattern WINDOWED_SEGMENT_PATTERN = Pattern.compile("\\.[0-9]{10,20}(\\.[0-9]{10,20})?$");
    //private static final Pattern WINDOWED_SEGMENT_PATTERN = Pattern.compile("\\.[0-9]+(\\.[0-9]+)?$");

    private Statistics statistics;

    // A new RocksDBConfigSetter is created for each state-store, so this has to be static.
    // (assumption that this would be created once and called / state-store was incorrect).
    private static RocksDBCacheManager manager;

    @Override
    public void setConfig(final String rawStoreName, final Options options, final Map<String, Object> configs) {

        final String storeName = maybeExtractBaseStoreName(rawStoreName);

        log.debug("Setting RocksDB config for store: {} ({}) identity={}", storeName, rawStoreName, System.identityHashCode(this));

        RocksDBConfig rocksDBConfig = new RocksDBConfig(storeName, configs);

        rocksDBConfig.getCompactionStyle().ifPresent(options::setCompactionStyle);
        rocksDBConfig.getCompressionType().ifPresent(options::setCompressionType);
        rocksDBConfig.getMaxBackgroundJobs().ifPresent(options::setMaxBackgroundJobs);
        rocksDBConfig.getMaxOpenFile().ifPresent(options::setMaxOpenFiles);
        rocksDBConfig.getMaxWriteBufferNumber().ifPresent(options::setMaxWriteBufferNumber);
        rocksDBConfig.getWriteBufferSize().ifPresent(options::setWriteBufferSize);

        rocksDBConfig.getMaxLogFileSize().ifPresent(options::setMaxLogFileSize);
        rocksDBConfig.getLogDir().ifPresent(options::setDbLogDir);
        rocksDBConfig.getLogLevel().ifPresent(options::setInfoLogLevel);
        rocksDBConfig.getDumpPeriodSec().ifPresent(options::setStatsDumpPeriodSec);

        final BlockBasedTableConfig blockBasedTableConfig = (BlockBasedTableConfig) options.tableFormatConfig();

//        options.setMaxWriteBufferNumber(2);
//        options.setWriteBufferSize(64 * 1024);
//        //?????
//        blockBasedTableConfig.setCacheIndexAndFilterBlocks(true);
//        blockBasedTableConfig.setPinTopLevelIndexAndFilter(true);
//        blockBasedTableConfig.setCacheIndexAndFilterBlocks(true);
//        blockBasedTableConfig.setCacheIndexAndFilterBlocksWithHighPriority(true);
//        blockBasedTableConfig.setBlockSize(128);

        rocksDBConfig.getBlockCacheSize().ifPresentOrElse(cacheSize -> {
            blockBasedTableConfig.setBlockCache(BlockCacheCreator.createCache(cacheSize));
        }, () -> {
            // use the shared cache for this state-store
            rocksDBConfig.getSharedBlockCacheSize().ifPresent(sharedBlockCacheSize -> {
                if (manager == null) {
                    manager = new RocksDBCacheManager(sharedBlockCacheSize);
                }
                blockBasedTableConfig.setBlockCache(manager.register(storeName));
            });
        });

        options.setTableFormatConfig(blockBasedTableConfig);

        // log.warn("DEBUG_STORE " + storeName + " using Cache Instance: " + System.identityHashCode(manager.getSharedCache()));
    }

    @Override
    public void close(final String rawStoreName, final Options options) {

        final String storeName = maybeExtractBaseStoreName(rawStoreName);

        log.info("Closing additional resources for RocksDB store {}", storeName);

        // manager.close();
        if (manager != null) {
            manager.unregister(storeName);
        }

        if (statistics != null) {
            statistics.close();
            statistics = null;
        }
    }

    //1766580780000

    private static String maybeExtractBaseStoreName(String storeName) {
        if (storeName == null) {
            return null;
        }
        return WINDOWED_SEGMENT_PATTERN.matcher(storeName).replaceAll("");
    }

////    public static void main(String[] string) {
////        System.out.println(maybeExtractBaseStoreName("HOPPING-aggregate-purchase-order.3.1766581920000"));
//    }
}

//    if (rocksDBConfig.isStatisticsEnable()) {
//
//      log.info("Enabling RocksDB statistics for state store '{}'", storeName);
//
//      statistics = new Statistics();
//      statistics.setStatsLevel(StatsLevel.ALL);
//      options.setStatistics(statistics);
//
//      rocksDBConfig.getDumpPeriodSec().ifPresent(options::setStatsDumpPeriodSec);
//    } else {
//      options.setStatsDumpPeriodSec(0);
//    }


//

// options.setWriteBufferSize(1);

