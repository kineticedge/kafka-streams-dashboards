package io.kineticedge.io.ksd.observer.old;

import com.beust.jcommander.Parameter;
import io.kineticedge.ksd.tools.config.BaseOptions;

public class Options extends BaseOptions {

  @Parameter(names = { "--url" })
  private String ollamaUrl = "http://localhost:11434/api/generate";

  @Parameter(names = { "--model-name" })
  //private String modelName = "deepseek-coder:1.3b-instruct";
  private String modelName = "deepseek-r1";

  @Parameter(names = { "--input" })
  private String inputTopic;

  @Parameter(names = { "--output" })
  private String outputTopic;


  public String getOllamaUrl() {
    return ollamaUrl;
  }

  public String getModelName() {
    return modelName;
  }

  public String getInputTopic() {
    return inputTopic;
  }

  public String getOutputTopic() {
    return outputTopic;
  }
}
