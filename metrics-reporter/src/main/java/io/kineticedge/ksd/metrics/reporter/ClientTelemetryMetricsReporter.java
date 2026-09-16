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

public class ClientTelemetryMetricsReporter implements MetricsReporter, ClientTelemetry {

  private static final Logger log = LoggerFactory.getLogger(ClientTelemetryMetricsReporter.class);

  private static final String ENDPOINT_CONFIG =  "metric.reporters.clienttelemetry.endpoint";

  private TelemetryReceiver receiver;

  @Override
  public void configure(Map<String, ?> configs) {
    final String url = (String) configs.get(ENDPOINT_CONFIG);
    if (url == null) {
      log.warn("configuration {} not defined, client metrics will not be reported.", ENDPOINT_CONFIG);
      receiver = null;
    } else {
      log.info("ClientTelemetryMetricsReporter: {}", url);
      receiver = new TelemetryReceiver(url);
    }
  }

  @Override
  public void contextChange(MetricsContext metricsContext) {
    if (receiver != null) {
      receiver.setMetricsContext(metricsContext.contextLabels());
    }
  }

  @Override
  public void close() {
    if (receiver != null) {
      receiver.close();
    }
    receiver = null;
  }

  /**
   * Using a Client MetricsReporter with Client Telemetry to report on the client metrics sent from the client to the broker.
   */
  @Override
  public ClientTelemetryReceiver clientReceiver() {
    return receiver;
  }

  @Override
  public void init(List<KafkaMetric> metrics) {
    // Not reporting on any of the client metrics (broker side), continue to use an additional metricsreporter, such as the provided JmxReporter.
  }

  @Override
  public void metricChange(KafkaMetric metric) {
    // Not reporting on any of the client metrics (broker side), continue to use an additional metricsreporter, such as the provided JmxReporter.
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
    // Not reporting on any of the client metrics (broker side), continue to use an additional metricsreporter, such as the provided JmxReporter.
  }

}