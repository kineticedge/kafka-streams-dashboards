
package io.kineticedge.ksd.kroxylicious.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;
import io.kroxylicious.proxy.plugin.Plugins;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link FilterFactory} for {@link FetchResponseTransformationFilter}.
 */
@Plugin(configType = FetchResponseTransformation.Config.class)
public class FetchResponseTransformation implements FilterFactory<FetchResponseTransformation.Config, FetchResponseTransformation.Config> {


  public record TopicConfig(
          @JsonProperty(required = true)
          Set<String> topics,

          @JsonProperty(required = true)
          @PluginImplName(ByteBufferTransformationFactory.class)
          String transformation,

          @PluginImplConfig(implNameProperty = "transformation")
          Object transformationConfig
  ) {}

  public record Config(
          @JsonProperty(required = true)
          @PluginImplName(ByteBufferTransformationFactory.class) String transformation,
          @PluginImplConfig(implNameProperty = "transformation") Object transformationConfig//,
          //@JsonProperty(required = true) List<TopicConfig> configurations
  ) {}




  @Override
  public Config initialize(FilterFactoryContext context, Config config) {
    return Plugins.requireConfig(this, config);
  }

  @Override
  public FetchResponseTransformationFilter createFilter(FilterFactoryContext context,
                                                        Config configuration) {
    var factory = context.pluginInstance(ByteBufferTransformationFactory.class, configuration.transformation());
    Objects.requireNonNull(factory, "Violated contract of FilterCreationContext");
    return new FetchResponseTransformationFilter(factory.createTransformation(configuration.transformationConfig()));
  }


}
