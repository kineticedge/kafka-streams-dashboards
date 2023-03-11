package io.kineticedge.ksd.tools.config;

import org.apache.kafka.clients.CommonClientConfigs;

import java.util.Map;
import java.util.stream.Collectors;


public class KafkaEnvUtil {

    private static final String GROUP_INSTANCE_ID_ENV = "GROUP_INSTANCE_ID";
    private static final String GROUP_INSTANCE_ID_CONFIG = "group.instance.id";
    private static final String CLIENT_ID_ENV = "CLIENT_ID";
    private static final String CLIENT_ID_CONFIG = CommonClientConfigs.CLIENT_ID_CONFIG;

    private KafkaEnvUtil() {
    }

    /**
     * Takes all environment variables that start with the given preifx, and return them with the key modified
     * to exclude the prefix, be lower-case, and replace '_' with '.'.
     */
    public static Map<String, String> to(final String prefix) {
        Map<String, String> map = System.getenv().entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> {
                    final String key = e.getKey().substring(prefix.length()).replaceAll("_", ".").toLowerCase();
                    return Map.entry(key, e.getValue());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (System.getenv(GROUP_INSTANCE_ID_ENV) != null) {
            map.put(GROUP_INSTANCE_ID_CONFIG, System.getenv(GROUP_INSTANCE_ID_ENV));
        }

        if (System.getenv(CLIENT_ID_ENV) != null) {
            map.put(CLIENT_ID_CONFIG, System.getenv(CLIENT_ID_ENV));
        }

        return map;
    }
}
