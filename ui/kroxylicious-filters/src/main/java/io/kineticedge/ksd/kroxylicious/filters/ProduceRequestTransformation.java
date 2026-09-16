package io.kineticedge.ksd.kroxylicious.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginImplConfig;
import io.kroxylicious.proxy.plugin.PluginImplName;
import io.kroxylicious.proxy.plugin.Plugins;

/**
 * A {@link FilterFactory} for {@link ProduceRequestTransformationFilter}.
 */
@Plugin(configType = ProduceRequestTransformation.Config.class)
public class ProduceRequestTransformation implements FilterFactory<ProduceRequestTransformation.Config, ProduceRequestTransformation.Config> {


  public record Config(
          @PluginImplName(ByteBufferTransformationFactory.class) @JsonProperty(required = true) String transformation,
          @PluginImplConfig(implNameProperty = "transformation") Object transformationConfig) {
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ProduceRequestTransformationFilter createFilter(FilterFactoryContext context,
                                                         Config configuration) {
    ByteBufferTransformationFactory factory = context.pluginInstance(ByteBufferTransformationFactory.class, configuration.transformation());
    return new ProduceRequestTransformationFilter(factory.createTransformation(configuration.transformationConfig()));
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public Config initialize(FilterFactoryContext context, Config config) {
    Plugins.requireConfig(this, config);
    var transformationFactory = context.pluginInstance(ByteBufferTransformationFactory.class, config.transformation());
    transformationFactory.validateConfiguration(config.transformationConfig());
    return config;
  }

}
