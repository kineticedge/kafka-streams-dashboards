package io.kineticedge.ksd.tools.serde;


import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.ListDeserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListOfStringsDeserializer implements Deserializer<String> {


    protected ListDeserializer<String> inner = new ListDeserializer<>();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

        Map<String, Object> hardcoded = new HashMap<>();
        hardcoded.put("default.list.value.serde.inner", Serdes.StringSerde.class.getName());
        hardcoded.put("default.list.value.serde.type", ArrayList.class.getName());
        hardcoded.put("default.list.key.serde.inner", Serdes.StringSerde.class.getName());
        hardcoded.put("default.list.key.serde.type", ArrayList.class.getName());

        inner.configure(hardcoded, isKey);
    }

    @Override
    public String deserialize(String topic, byte[] data) {
        List<String> list = inner.deserialize(topic, data);
        return String.join(",", list);
    }

}
