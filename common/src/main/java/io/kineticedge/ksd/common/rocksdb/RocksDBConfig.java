package io.kineticedge.ksd.common.rocksdb;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.Utils;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;
import org.rocksdb.InfoLogLevel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RocksDBConfig extends AbstractConfig {

  private static final String GLOBAL_PREFIX = "rocksdb.";
  private static final String STORE_PREFIX = "rocksdb.store.{{store}}.";

  // CORE
  public static final String SHARED_BLOCK_CACHE_SIZE = "shared.block.cache.size"; // <- a single cache used for all state-stores
  public static final String BLOCK_CACHE_SIZE = "block.cache.size";
  public static final String COMPACTION_STYLE = "compaction.style";
  public static final String COMPRESSION_TYPE = "compression.type";
  public static final String MAX_BACKGROUND_JOBS = "max.background.jobs";
  public static final String MAX_OPEN_FILES = "max.open.files";
  public static final String MAX_WRITE_BUFFER_NUMBER = "max.write.buffer.number";
  public static final String WRITE_BUFFER_SIZE = "write.buffer.size";
  // TODO
  public static final String MEMORY_MANAGED = "memory.managed";
  public static final String MEMORY_HIGH_PRIORITY_POOL_RATIO = "memory.high.prio.pool.ratio";
  public static final String MEMORY_STRICT_CAPACITY_LIMIT = "memory.strict.capacity.limit";
  public static final String MEMORY_WRITE_BUFFER_RATIO = "memory.write.buffer.ratio";
  //LOGGING
  public static final String LOG_DIR = "log.dir";
  public static final String LOG_LEVEL = "log.level";
  public static final String LOG_MAX_FILE_SIZE = "log.max.file.size";
  //STATISTICS
  public static final String STATS_DUMP_PERIOD_SEC = "stats.dump.period.sec";


  RocksDBConfig(final String storeName, final Map<String, ?> originals) {
    super(configDef(), resolve(storeName, originals), Collections.emptyMap(), false);
  }

  // to configure a store named 'foo-bar', 'FOO-BAR', 'FOO.BAR, 'FOO_BAR', etc. use the prefix 'rocksdb.store.foobar'.
  //
  // the reason for this is the rules of how configurations are created and how apache kafka assumes all properties are lowercase and delimited by a period.
  // if the application has distinct store name of 'foo-bar' and 'foobar', it really should be refactored.
  private static String normalizeStoreName(String storeName) {
    return storeName.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private static Map<String, ?> resolve(String storeName, Map<String, ?> originals) {

    final Map<String, Object> map = new HashMap<>();
    map.putAll(Utils.entriesWithPrefix(originals, GLOBAL_PREFIX, true));
    // TODO remove rocksdb.store.{{store}}.shared.block.cache.size <- not allowed...
    map.putAll(Utils.entriesWithPrefix(originals, STORE_PREFIX.replace("{{store}}", normalizeStoreName(storeName)), true));
    return map;
  }

  static ConfigDef configDef() {
    return new ConfigDef()
            //CORE
            .define(SHARED_BLOCK_CACHE_SIZE, ConfigDef.Type.LONG, null, ConfigDef.Importance.MEDIUM, "a single cache used by all state-stores, total size to be used for caching uncompressed data blocks")
            .define(BLOCK_CACHE_SIZE, ConfigDef.Type.LONG, null, ConfigDef.Importance.MEDIUM, "total size to be used for caching uncompressed data blocks")
            .define(COMPACTION_STYLE, ConfigDef.Type.STRING, null, new CompactionStyleValidator(), ConfigDef.Importance.LOW, "compaction style")
            .define(COMPRESSION_TYPE, ConfigDef.Type.STRING, null, new CompressionTypeValidator(), ConfigDef.Importance.LOW, "compression type")
            .define(MAX_OPEN_FILES, ConfigDef.Type.INT, null, ConfigDef.Importance.LOW, "maximum number of open files that can be used per RocksDB instance")
            .define(MAX_BACKGROUND_JOBS, ConfigDef.Type.INT, null, ConfigDef.Importance.MEDIUM, "maximum number of concurrent background jobs (both flushes and compactions combined)")
            .define(MAX_WRITE_BUFFER_NUMBER, ConfigDef.Type.INT, null, ConfigDef.Importance.MEDIUM, "maximum number of memtables in memory before flushing to SST files")
            .define(WRITE_BUFFER_SIZE, ConfigDef.Type.LONG, null, ConfigDef.Importance.MEDIUM, "size of a single memtable")
            //LOGGING
            .define(LOG_DIR, ConfigDef.Type.STRING, null, ConfigDef.Importance.MEDIUM, "log directory")
            .define(LOG_LEVEL, ConfigDef.Type.STRING, null, new InfoLogLevelValidator(), ConfigDef.Importance.MEDIUM, "logging level")
            .define(LOG_MAX_FILE_SIZE, ConfigDef.Type.INT, null, ConfigDef.Importance.MEDIUM, "rocksDB maximum log file size")
            // Stats
            .define(STATS_DUMP_PERIOD_SEC, ConfigDef.Type.INT, null, ConfigDef.Importance.MEDIUM, "rocksDB statistics dump period (seconds)")
            // Memory Management
            //.define(MEMORY_HIGH_PRIORITY_POOL_RATIO, ConfigDef.Type.DOUBLE, null, ConfigDef.Range.between(0, 1), ConfigDef.Importance.MEDIUM, "ratio of cache memory that is reserved for high priority blocks")
            //.define(MEMORY_MANAGED, ConfigDef.Type.BOOLEAN, null, ConfigDef.Importance.MEDIUM, "enable automatic memory management")
            //.define(MEMORY_STRICT_CAPACITY_LIMIT, ConfigDef.Type.BOOLEAN, null, ConfigDef.Importance.LOW, "block cache with strict capacity limit")
            //.define(MEMORY_WRITE_BUFFER_RATIO, ConfigDef.Type.DOUBLE, null, ConfigDef.Range.between(0, 1), ConfigDef.Importance.MEDIUM, "when 'memory.managed' is true, the ratio of total cache memory reserved for write buffer manager")
            ;
  }


  Optional<Integer> getMaxWriteBufferNumber() {
    return Optional.ofNullable(getInt(MAX_WRITE_BUFFER_NUMBER));
  }

  Optional<Long> getWriteBufferSize() {
    return Optional.ofNullable(getLong(WRITE_BUFFER_SIZE));
  }


//  Optional<Boolean> isMemoryManaged() {
//    return Optional.ofNullable(getBoolean(MEMORY_MANAGED));
//  }

  Optional<Long> getSharedBlockCacheSize() {
    return Optional.ofNullable(getLong(SHARED_BLOCK_CACHE_SIZE));
  }

  Optional<Long> getBlockCacheSize() {
    return Optional.ofNullable(getLong(BLOCK_CACHE_SIZE));
  }

//  Optional<Double> getMemoryHighPrioPoolRatio() {
//    return Optional.ofNullable(getDouble(MEMORY_HIGH_PRIORITY_POOL_RATIO));
//  }

//  Optional<Double> getMemoryWriteBufferRatio() {
//    return Optional.ofNullable(getDouble(MEMORY_WRITE_BUFFER_RATIO));
//  }

  Optional<CompactionStyle> getCompactionStyle() {
    return Optional.ofNullable(getString(COMPACTION_STYLE)).map(String::toUpperCase).map(CompactionStyle::valueOf);
  }

  // <none>, snappy, z, bzip2, lz4, lz4hc, xpress, zstd
  Optional<CompressionType> getCompressionType() {
    return Optional.ofNullable(getString(COMPRESSION_TYPE)).map(CompressionType::getCompressionType);
  }

//  Boolean getMemoryStrictCapacityLimit() {
//    return getBoolean(MEMORY_STRICT_CAPACITY_LIMIT);
//  }

  Optional<Integer> getMaxBackgroundJobs() {
    return Optional.ofNullable(getInt(MAX_BACKGROUND_JOBS));
  }

  Optional<Integer> getMaxOpenFile() {
    return Optional.ofNullable(getInt(MAX_OPEN_FILES));
  }

  Optional<InfoLogLevel> getLogLevel() {
    return Optional.ofNullable(getString(LOG_LEVEL)).map(String::toUpperCase).map(e -> e + "_LEVEL").map(InfoLogLevel::valueOf);
  }

  Optional<Integer> getDumpPeriodSec() {
    return Optional.ofNullable(getInt(STATS_DUMP_PERIOD_SEC));
  }

  Optional<String> getLogDir() {
    return Optional.ofNullable(getString(LOG_DIR));
  }

  Optional<Integer> getMaxLogFileSize() {
    return Optional.ofNullable(getInt(LOG_MAX_FILE_SIZE));
  }

  //

  private final static class CompactionStyleValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(final String name, final Object value) {
      if (value instanceof String v) {
        try {
          CompactionStyle.valueOf(v.toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new ConfigException(name, value, String.format("'%s' is not a valid value for %s, valid values are %s", value, COMPACTION_STYLE, Arrays.stream(CompactionStyle.values()).map(Enum::name).toList()));
        }
      } else if (value != null) {
        throw new ConfigException(name, value, "must be a string");
      }
    }
  }


  private final static class CompressionTypeValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(final String name, final Object value) {
      if (value instanceof String v) {
        if (CompressionType.NO_COMPRESSION == CompressionType.getCompressionType(v)) {
          throw new ConfigException(name, value, String.format("'%s' is not a valid value for %s, valid values are %s", value, COMPRESSION_TYPE, Arrays.stream(CompressionType.values()).map(CompressionType::getLibraryName).filter(Objects::nonNull).toList()));
        }
      } else if (value != null) {
        throw new ConfigException(name, value, "must be a string");
      }
    }
  }

  private final static class InfoLogLevelValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(final String name, final Object value) {
      if (value instanceof String v) {
        try {
          InfoLogLevel.valueOf(v.toUpperCase() + "_LEVEL");
        } catch (IllegalArgumentException ex) {
          throw new ConfigException(name, value, String.format("'%s' is not a valid value for %s, valid values are %s",
                  value,
                  LOG_LEVEL,
                  Arrays.stream(InfoLogLevel.values())
                          .filter(e -> e != InfoLogLevel.NUM_INFO_LOG_LEVELS)
                          .map(Enum::name)
                          .map(e -> e.replace("_LEVEL", ""))
                          .toList()));
        }
      } else if (value != null) {
        throw new ConfigException(name, value, "must be a string");
      }
    }
  }

}
