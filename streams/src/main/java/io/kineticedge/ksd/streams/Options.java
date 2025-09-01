package io.kineticedge.ksd.streams;

import com.beust.jcommander.Parameter;
import io.kineticedge.ksd.tools.config.BaseOptions;

import java.util.UUID;

public class Options extends BaseOptions {

  @Parameter(names = {"-g", "--application-id"}, description = "application id")
  private String applicationId = "pickup-order-handler";

  @Parameter(names = {"--client-id"}, description = "client id")
  private String clientId = "s-" + UUID.randomUUID();

  @Parameter(names = {"--group-instance-id"}, description = "group instance id")
  private String groupInstanceId;

  @Parameter(names = {"--auto-offset-reset"}, description = "where to start consuming from if no offset is provided")
  private String autoOffsetReset = "earliest";

  // Window scenarios

  @Parameter(names = {"--window-size"}, description = "")
  private Integer windowSize = 10;

  @Parameter(names = {"--grace-period"}, description = "")
  private Integer gracePeriod = 2;

  @Parameter(names = {"--port"}, description = "the port use for introspection of window state")
  private Integer port = 8080;

  public String getApplicationId() {
    return applicationId;
  }

  public String getClientId() {
    return clientId;
  }

  public String getGroupInstanceId() {
    return groupInstanceId;
  }

  public String getAutoOffsetReset() {
    return autoOffsetReset;
  }

  public Integer getWindowSize() {
    return windowSize;
  }

  public Integer getGracePeriod() {
    return gracePeriod;
  }

  public Integer getPort() {
    return port;
  }
}
