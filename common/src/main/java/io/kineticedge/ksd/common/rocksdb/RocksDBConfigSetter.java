package io.kineticedge.ksd.common.rocksdb;

import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.Options;
import org.rocksdb.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * This {@link RocksDBConfigSetter} implementation allows fine-tuning of RocksDB instances.
 * It also allows managing shared memory across all RocksDB instances and to enable dump of RocksDB statistics.
 */
public class RocksDBConfigSetter implements org.apache.kafka.streams.state.RocksDBConfigSetter {

  private static final Logger log = LoggerFactory.getLogger(RocksDBConfigSetter.class);

  //public static final long ROCKSDB_BLOCK_CACHE_SIZE_DEFAULT = 50 * 1024 * 1024; // 50 MB

  private Statistics statistics;

  @Override
  public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {

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


//    aused by: java.lang.NullPointerException: Cannot invoke "org.apache.kafka.streams.state.internals.RocksDBStore$ColumnFamilyAccessor.addToBatch(byte[], byte[], org.rocksdb.WriteBatchInterface)" because "this.cfAccessor" is null


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

  }

  @Override
  public void close(final String storeName, final Options options) {
    log.info("Closing additional resources for RocksDB store {}", storeName);

    if (statistics != null) {
      statistics.close();
      statistics = null;
    }
  }

}