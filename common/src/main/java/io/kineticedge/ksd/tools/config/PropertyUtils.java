package io.kineticedge.ksd.tools.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public final class PropertyUtils {

  private PropertyUtils() {
  }

  public static Map<String, ?> loadProperties(final String filename) {
    try {
      final Properties properties = new Properties();
      final File file = new File(filename);
      if (file.exists() && file.isFile()) {
        log.info("loading property file {}.", filename);
        properties.load(new FileInputStream(file));
        return properties.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
      } else {
        log.info("unable to read {}, ignoring.", filename);
        return Collections.emptyMap();
      }
    } catch (final IOException e) {
      log.info("property file {} not found, ignoring.", filename);
      return Collections.emptyMap();
    }
  }

}
