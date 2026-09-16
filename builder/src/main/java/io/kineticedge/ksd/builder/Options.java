package io.kineticedge.ksd.builder;

import com.beust.jcommander.Parameter;
import io.kineticedge.ksd.tools.config.BaseOptions;

public class Options extends BaseOptions {

  @Parameter(names = {"--delete-topics"}, description = "")
  private boolean deleteTopics = false;

  public boolean isDeleteTopics() {
    return deleteTopics;
  }

}
