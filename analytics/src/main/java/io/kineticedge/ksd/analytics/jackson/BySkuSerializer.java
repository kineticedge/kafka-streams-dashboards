package io.kineticedge.ksd.analytics.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kineticedge.ksd.analytics.domain.BySku;
import java.io.IOException;

public class BySkuSerializer extends StdSerializer<BySku> {

    public BySkuSerializer() {
        this(null);
    }

    public BySkuSerializer(final Class<BySku> vc) {
        super(vc);
    }

    public void serialize(final BySku value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        value.getRecords().forEach((k, v) -> {
            try {
                gen.writeStartObject();
                //gen.writeObjectField("start", k.getStart());
                //gen.writeObjectField("end", k.getEnd());
                gen.writeObjectField("mode", "span");
                gen.writeObjectField("html", false);
                gen.writeObjectField("label", k);
                gen.writeObjectField("children", v.values());
                gen.writeEndObject();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndArray();
    }


}
