package io.kineticedge.ksd.streams.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kineticedge.ksd.streams.domain.ByOrderId;

import java.io.IOException;

public class ByOrderIdSerializer extends StdSerializer<ByOrderId> {

    public ByOrderIdSerializer() {
        this(null);
    }

    public ByOrderIdSerializer(final Class<ByOrderId> vc) {
        super(vc);
    }

    public void serialize(final ByOrderId value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        value.getRecords().forEach((k, v) -> {
            try {
                gen.writeObject(v);
//                gen.writeStartObject();
//                //gen.writeObjectField("start", k.getStart());
//                //gen.writeObjectField("end", k.getEnd());
//                gen.writeObjectField("mode", "span");
//                gen.writeObjectField("html", false);
//                gen.writeObjectField("label", k);
//                gen.writeObjectField("children", v);
//                gen.writeEndObject();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndArray();
    }


}
