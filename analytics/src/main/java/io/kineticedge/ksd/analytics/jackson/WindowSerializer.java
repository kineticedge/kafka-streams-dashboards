package io.kineticedge.ksd.analytics.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kineticedge.ksd.analytics.domain.Window;
import java.io.IOException;

public class WindowSerializer extends StdSerializer<Window> {

    public WindowSerializer() {
        this(null);
    }

    public WindowSerializer(final Class<Window> vc) {
        super(vc);
    }

    public void serialize(final Window value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}
