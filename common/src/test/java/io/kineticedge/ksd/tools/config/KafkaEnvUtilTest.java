package io.kineticedge.ksd.tools.config;

import io.kineticedge.ksd.common.environment.MockEnvironment;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaEnvUtilTest {

  private static Map<String, String> env = Map.ofEntries(
          Map.entry("KAFKA_1", "one"),
          Map.entry("KAFKA_A", "two"),
          Map.entry("KAFKA_b", "three")
  );

  private KafkaEnvUtil kafkaEnvUtil = new KafkaEnvUtil(new MockEnvironment(env));

  @Test
  void x() {
    System.out.println(kafkaEnvUtil.to("KAFKA_"));
  }

}