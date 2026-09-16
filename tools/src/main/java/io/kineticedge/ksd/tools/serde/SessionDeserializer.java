package io.kineticedge.ksd.tools.serde;


import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SessionDeserializer implements Deserializer<String> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final int TIMESTAMP_SIZE = 8;

    protected Deserializer<String> inner = new StringDeserializer();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        inner.configure(configs, isKey);
    }

    @Override
    public String deserialize(String topic, byte[] data) {

        final byte[] bytes = new byte[data.length - 2 * TIMESTAMP_SIZE];

        System.arraycopy(data, 0, bytes, 0, bytes.length);

        final String key = inner.deserialize(topic, bytes);

        final ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.position(data.length - 2 * TIMESTAMP_SIZE);
        final long end = buffer.getLong();
        final long start = buffer.getLong();


        String tsStart =  LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()).format(TIME_FORMATTER);
        String tsEnd =  LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()).format(TIME_FORMATTER);

        return key + ":" + tsStart + ":" + tsEnd;
    }

}
