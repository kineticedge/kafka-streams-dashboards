package io.kineticedge.ksd.tools.serde;


import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.nio.ByteBuffer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class WindowDeserializer implements Deserializer<String> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final int TIMESTAMP_SIZE = 8;
    private static final int SEQNUM_SIZE = 4;
    private static final int SUFFIX_SIZE = TIMESTAMP_SIZE + SEQNUM_SIZE;

    protected Deserializer<String> inner = new StringDeserializer();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        inner.configure(configs, isKey);
    }

    @Override
    public String deserialize(String topic, byte[] data) {

        final byte[] bytes = new byte[data.length - SUFFIX_SIZE];

        System.arraycopy(data, 0, bytes, 0, bytes.length);

        final String key = inner.deserialize(topic, bytes);


        final ByteBuffer buffer = ByteBuffer.wrap(data);

        final long start = buffer.getLong(data.length - SUFFIX_SIZE);
        final int sequence = buffer.getInt(data.length - SEQNUM_SIZE);

        String ts =  LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()).format(TIME_FORMATTER);

        return key + ":" + ts + ":" + sequence;
    }

}
