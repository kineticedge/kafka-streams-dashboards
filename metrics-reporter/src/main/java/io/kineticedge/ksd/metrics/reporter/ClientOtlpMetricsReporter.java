package io.kineticedge.ksd.metrics.reporter;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsContext;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.server.telemetry.ClientTelemetry;
import org.apache.kafka.server.telemetry.ClientTelemetryReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ClientOtlpMetricsReporter implements MetricsReporter, ClientTelemetry {

  private static final Logger log = LoggerFactory.getLogger(ClientOtlpMetricsReporter.class);

  // OTEL_EXPORTER_OTLP_ENDPOINT is the standard environment used by
  // the opentelemetry-javaagent.jar java agent, so use it here.
  private static final String ENV_ENDPOINT = "OTEL_EXPORTER_OTLP_ENDPOINT";

  private final TelemetryReceiver receiver;

  public ClientOtlpMetricsReporter() {
    final String grpcEndpoint = System.getenv(ENV_ENDPOINT);
    if (grpcEndpoint != null) {
      log.info("ClientOtlpMetricsReporter: {}", grpcEndpoint);
      receiver = new TelemetryReceiver(grpcEndpoint);
    } else {
      log.warn("environment variable {} not defined, client metrics will not be reported.", ENV_ENDPOINT);
      receiver = null;
    }
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // No configuration needed
  }

  @Override
  public void contextChange(MetricsContext metricsContext) {
    receiver.setMetricsContext(metricsContext.contextLabels());
  }

  @Override
  public void close() {
    receiver.close();
  }

  @Override
  public ClientTelemetryReceiver clientReceiver() {
    return receiver;
  }

  @Override
  public void init(List<KafkaMetric> metrics) {
  }

  @Override
  public void metricChange(KafkaMetric metric) {
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
  }

}