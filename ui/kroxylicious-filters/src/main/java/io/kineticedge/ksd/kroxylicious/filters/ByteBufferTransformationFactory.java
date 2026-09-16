package io.kineticedge.ksd.kroxylicious.filters;

import io.kroxylicious.proxy.plugin.PluginConfigurationException;

public interface ByteBufferTransformationFactory<C> {

  void validateConfiguration(C config) throws PluginConfigurationException;

  default C requireConfig(C config) {
    if (config == null) {
      throw new PluginConfigurationException(this.getClass().getSimpleName() + " requires configuration, but config object is null");
    }
    return config;
  }

  ByteBufferTransformation createTransformation(C configuration);

}
