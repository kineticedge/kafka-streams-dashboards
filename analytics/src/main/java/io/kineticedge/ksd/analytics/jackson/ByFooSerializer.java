package io.kineticedge.ksd.analytics.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kineticedge.ksd.analytics.domain.ByFoo;

import java.io.IOException;

public class ByFooSerializer extends StdSerializer<ByFoo> {

    public ByFooSerializer() {
        this(null);
    }

    public ByFooSerializer(final Class<ByFoo> vc) {
        super(vc);
    }

    public void serialize(final ByFoo value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        value.getRecords().forEach((k, v) -> {
            try {
                gen.writeObject(v);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndArray();
    }


}
