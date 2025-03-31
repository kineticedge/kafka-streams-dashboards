package io.kineticedge.ksd.kroxylicious.filters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Plugin(configType = JsonMasking.Config.class)
public class JsonMasking implements ByteBufferTransformationFactory<JsonMasking.Config> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .registerModule(new JavaTimeModule());

    public enum MaskType {
        MASK,
        REMOVE
    }

    public record Config(String charset, List<String> fields, MaskType maskType, String mask) {}


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

        if (config.fields() == null || config.fields().isEmpty()) {
            throw new PluginConfigurationException("At least one field must be specified");
        }
    }

    @Override
    public Transformation createTransformation(Config configuration) {
        return new Transformation(configuration);
    }


    static class Transformation implements ByteBufferTransformation {

        private final Charset charset;
        private final List<String> fields;
        private final MaskType maskType;
        private final String mask;

        Transformation(Config config) {
            this.charset = Charset.forName(config.charset());
            this.fields = config.fields();
            this.maskType = config.maskType() != null ? config.maskType() : MaskType.MASK;
            this.mask = config.mask() != null ? config.mask() : "****";
        }

        @Override
        public ByteBuffer transform(String topicName, ByteBuffer in) {
            try {
                final Map<String, Object>  map = OBJECT_MAPPER.readValue(new String(charset.decode(in).array()), MAP_TYPE_REFERENCE);
                return ByteBuffer.wrap(OBJECT_MAPPER.writeValueAsBytes(maskValues(map, fields)));
            } catch (Exception e) {
                return ByteBuffer.wrap("***** unable to parse JSON *****".getBytes(charset));
            }
        }


        @SuppressWarnings("unchecked")
        public Map<String, Object> maskValues(Map<String, Object> map, List<String> keys) {

            final Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();

                // Remove the key if it matches
                if (keys.contains(entry.getKey())) {

                    if (maskType == MaskType.REMOVE) {
                        iterator.remove();
                    } else {
                        entry.setValue(mask);
                    }
//                    if (entry.getValue() instanceof String) {
//                        entry.setValue(mask);
//                    } else if (entry.getValue() instanceof Number) {
//                        if (entry.getValue() instanceof Double) {
//                            entry.setValue(Double.MIN_VALUE);
//                        } else if (entry.getValue() instanceof Float) {
//                            entry.setValue(Float.MIN_VALUE);
//                        } else if (entry.getValue() instanceof Long) {
//                            entry.setValue(Long.MIN_VALUE);
//                        } else if (entry.getValue() instanceof Integer) {
//                            entry.setValue(Integer.MIN_VALUE);
//                        } else if (entry.getValue() instanceof Short) {
//                            entry.setValue(Short.MIN_VALUE);
//                        } else if (entry.getValue() instanceof Byte) {
//                            entry.setValue(Byte.MIN_VALUE);
//                        } else if (entry.getValue() instanceof BigDecimal) {
//                            entry.setValue(BigDecimal.ZERO);
//                        } else if (entry.getValue() instanceof BigInteger) {
//                            entry.setValue(BigInteger.ZERO);
//                        } else {
//                            iterator.remove();
//                        }
//                    } else {
//                        // datatype that has no masking ability, so removing it.
//                        iterator.remove();
//                    }
                    continue;
                }

                // If the value is a Map, recurse
                if (entry.getValue() instanceof Map) {
                    maskValues((Map<String, Object>) entry.getValue(), keys);
                }

                // If the value is a List, iterate over it and apply recursion for Maps
                if (entry.getValue() instanceof Iterable) {
                    for (Object item : (Iterable<?>) entry.getValue()) {
                        if (item instanceof Map) {
                            maskValues((Map<String, Object>) item, keys);
                        }
                        // if array of values, do we want to remove? if selected
                    }
                }
            }

            return map;
        }

    }



}
