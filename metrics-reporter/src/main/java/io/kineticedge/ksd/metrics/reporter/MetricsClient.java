package io.kineticedge.ksd.metrics.reporter;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

final class MetricsClient implements AutoCloseable {

  private static final long GRPC_CHANNEL_TIMEOUT_MS = 30_000L;

  private static final Logger log = LoggerFactory.getLogger(MetricsClient.class);

  private final ManagedChannel channel;
  private final MetricsServiceGrpc.MetricsServiceStub client;
  private final String endpoint;

  public MetricsClient(final ManagedChannel channel) {
    this.channel = channel;
    client = MetricsServiceGrpc.newStub(channel);
    endpoint = this.channel.authority();
    log.info("started grpc client, endpoint={}.", endpoint);
  }

  public void export(final List<ResourceMetrics> resourceMetrics) {
    final ExportMetricsServiceRequest request = ExportMetricsServiceRequest.newBuilder()
            .addAllResourceMetrics(resourceMetrics)
            .build();
      client.export(request, new StreamObserver<>() {
      @Override
      public void onNext(ExportMetricsServiceResponse value) {
        // do nothing.
      }
      @Override
      public void onError(Throwable t) {
        log.error("unable to export metrics, endpoint={}, state={}.", endpoint, channel.getState(true), t.getCause());
      }
      @Override
      public void onCompleted() {
        log.debug("successfully exported metrics, endpoint={}.", endpoint);
      }
    });
  }

  @Override
  public void close() {
    try {
      log.debug("shutting down grpc channel, endpoint={}.", endpoint);
      channel.shutdown().awaitTermination(GRPC_CHANNEL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (final Exception e) {
      Thread.currentThread().interrupt();
      log.error("failed shutting down grpc channel, endpoint={}.", endpoint, e);
    }
  }

}
