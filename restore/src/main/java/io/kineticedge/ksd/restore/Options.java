package io.kineticedge.ksd.restore;

import com.beust.jcommander.Parameter;
import io.kineticedge.ksd.tools.config.BaseOptions;

public class Options extends BaseOptions {

  @Parameter(names = {"--changelog-topic"}, description = "")
  private String changelogTopic = "analytics_GRADLE-NONE-aggregate-purchase-order-changelog";

  @Parameter(names = {"--restore-topic"}, description = "")
  private String restoreTopic = "pickup-order-handler-purchase-order-join-product-repartition-restore";

  @Parameter(names = {"--group-id"}, description = "")
  private String groupId = "restore";

  public String getChangelogTopic() {
    return changelogTopic;
  }

  public String getRestoreTopic() {
    return restoreTopic;
  }

  public String getGroupId() {
    return groupId;
  }
}
