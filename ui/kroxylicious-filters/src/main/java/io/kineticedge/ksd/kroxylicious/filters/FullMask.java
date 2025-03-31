package io.kineticedge.ksd.kroxylicious.filters;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

@Plugin(configType = FullMask.Config.class)
public class FullMask implements ByteBufferTransformationFactory<FullMask.Config> {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_MASK = "****";

    public record Config(String mask, String charset) {}

    @Override
    public void validateConfiguration(Config config) throws PluginConfigurationException {
        config = requireConfig(config);
        try {
            Charset.forName(config.charset());
        }
        catch (IllegalCharsetNameException e) {
            throw new PluginConfigurationException("Illegal charset name: '" + config.charset() + "'");
        }
        catch (UnsupportedCharsetException e) {
            throw new PluginConfigurationException("Unsupported charset: :" + config.charset() + "'");
        }
    }

    @Override
    public Transformation createTransformation(Config configuration) {
        return new Transformation(configuration);
    }

    public static class Transformation implements ByteBufferTransformation {

        private final byte[] mask;

        Transformation(Config config) {
          try {
            this.mask = (config.mask() != null ? config.mask() : DEFAULT_MASK).getBytes(config.charset());
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public ByteBuffer transform(String topicName, ByteBuffer in) {
            return ByteBuffer.wrap(mask);
        }
    }
}
