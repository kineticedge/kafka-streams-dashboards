package io.kineticedge.ksd.metrics.reporter;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import org.apache.kafka.common.requests.RequestContext;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.telemetry.ClientTelemetryPayload;
import org.apache.kafka.server.telemetry.ClientTelemetryReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TelemetryReceiver implements ClientTelemetryReceiver, AutoCloseable {
  
  private static final Logger log = LoggerFactory.getLogger(TelemetryReceiver.class);

  // Kafka-specific labels.
  private static final String KAFKA_BROKER_ID = "kafka.broker.id";
  private static final String KAFKA_CLUSTER_ID = "kafka.cluster.id";

  // Client-specific labels.
  private static final String CLIENT_ID = "client_id";
  private static final String CLIENT_INSTANCE_ID = "client_instance_id";
  private static final String CLIENT_SOFTWARE_NAME = "client_software_name";
  private static final String CLIENT_SOFTWARE_VERSION = "client_software_version";
  private static final String CLIENT_SOURCE_ADDRESS = "client_source_address";
  private static final String CLIENT_SOURCE_PORT = "client_source_port";
  private static final String PRINCIPAL = "principal";

  private final MetricsClient grpcService;

  private Map<String, String> metricsContext;

  public TelemetryReceiver(final String grpcEndpoint) {

    final ManagedChannel grpcChannel = ManagedChannelBuilder.forTarget(grpcEndpoint).usePlaintext().build();
    this.grpcService = new MetricsClient(grpcChannel);
  }

  public void setMetricsContext(Map<String, String> metricsContext) {
    this.metricsContext = metricsContext;
  }

  @Override
  public void exportMetrics(AuthorizableRequestContext context, ClientTelemetryPayload payload) {

    try {
      log.debug("Exporting metrics. Context: {}, payload: {}", context, payload);
      if (payload == null || payload.data() == null) {
        log.warn("exportMetrics - Client did not include payload, skipping export");
        return;
      }

      log.debug("Metrics data compressed size: {}", payload.data().remaining());
      MetricsData metricsData = MetricsData.parseFrom(payload.data());
      if (metricsData == null || metricsData.getResourceMetricsCount() == 0) {
        log.warn("No metrics available to export, skipping export");
        return;
      }

      log.debug("Pre-processed metrics data: {}, count: {}, size: {}",
              metricsData, metricsData.getResourceMetricsCount(), metricsData.getSerializedSize());
      List<ResourceMetrics> metrics = metricsData.getResourceMetricsList();
      // Enhance metrics with labels from request context, payload and broker, if any.
      Map<String, String> labels = fetchLabels(context, payload);
      // Update labels to metrics.
      metrics = appendLabelsToResource(metrics, labels);

      log.debug("Exporting metrics: {}, count: {}, size: {}", metrics, metricsData.getResourceMetricsCount(),
              metricsData.getSerializedSize());
      grpcService.export(metrics);
    } catch (Exception e) {
      log.error("Error processing client telemetry metrics: ", e);
    }
  }

  private Map<String, String> fetchLabels(AuthorizableRequestContext context, ClientTelemetryPayload payload) {

    final RequestContext requestContext = (RequestContext) context;

    final Map<String, String> labels = new HashMap<>();

    set(labels, CLIENT_ID, context.clientId());
    set(labels, CLIENT_INSTANCE_ID, payload.clientInstanceId().toString());
    set(labels, CLIENT_SOFTWARE_NAME, requestContext.clientInformation.softwareName());
    set(labels, CLIENT_SOFTWARE_VERSION, requestContext.clientInformation.softwareVersion());
    set(labels, CLIENT_SOURCE_ADDRESS, requestContext.clientAddress().getHostAddress());
    set(labels, CLIENT_SOURCE_PORT, Integer.toString(requestContext.clientPort.orElse(-1)));
    set(labels, PRINCIPAL, requestContext.principal().getName());

    // Include Kafka cluster and broker id from the MetricsContext, if available.
    set(labels, KAFKA_CLUSTER_ID, metricsContext.get(KAFKA_CLUSTER_ID));
    set(labels, KAFKA_BROKER_ID, metricsContext.get(KAFKA_BROKER_ID));

    return labels;
  }

  private static void set(Map<String, String> map, String key, String value) {
    Optional.ofNullable(value).ifPresent(v -> map.put(key, v));
  }

  private List<ResourceMetrics> appendLabelsToResource(final List<ResourceMetrics> resourceMetrics, final Map<String, String> labels) {

    final List<ResourceMetrics> updatedResourceMetrics = new ArrayList<>();

    resourceMetrics.forEach(rm -> {
      Resource.Builder resource = rm.getResource().toBuilder();
      labels.forEach((k, v) -> resource.addAttributes(
              KeyValue.newBuilder()
                      .setKey(k)
                      .setValue(AnyValue.newBuilder().setStringValue(v))
                      .build()
      ));
      ResourceMetrics updatedMetric = rm.toBuilder()
              .setResource(resource.build())
              .build();
      updatedResourceMetrics.add(updatedMetric);
    });

    return updatedResourceMetrics;
  }

  @Override
  public void close() {
    grpcService.close();
  }

}
