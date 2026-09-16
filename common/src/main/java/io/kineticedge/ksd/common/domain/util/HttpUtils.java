package io.kineticedge.ksd.common.domain.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class HttpUtils {

  private HttpUtils() {
  }

  public static Map<String, String> queryToMap(final String query) {
    return Optional.ofNullable(query)
            .map(q -> Arrays.stream(q.split("&"))
                    .map(param -> param.split("=", 2))
                    .collect(Collectors.toMap(entry -> entry[0], entry -> entry.length > 1 ? entry[1] : "")))
            .orElseGet(HashMap::new);
  }

}
