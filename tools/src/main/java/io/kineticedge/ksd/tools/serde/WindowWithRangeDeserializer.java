package io.kineticedge.ksd.tools.serde;


import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Takes window size as a parameter so console can include the calculated end timestamp.
 */
public class WindowWithRangeDeserializer implements Deserializer<String> {

    //private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static final String WINDOW_SIZE = "window.size";
    public static final String SEPARATOR = "separator";

    private static final int TIMESTAMP_SIZE = 8;
    private static final int SEQNUM_SIZE = 4;
    private static final int SUFFIX_SIZE = TIMESTAMP_SIZE + SEQNUM_SIZE;


    protected Deserializer<String> inner = new StringDeserializer();

    private long windowSize;
    private String separator;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        inner.configure(configs, isKey);

        Object value;

        value = configs.get(WINDOW_SIZE);

        if (value instanceof String) {
            windowSize = Long.parseLong((String) value);
        } else if (value instanceof Number) {
            windowSize = ((Number) value).longValue();
        } else {
            windowSize = 0L;
        }

        value = configs.get(SEPARATOR);
        if (value instanceof String) {
            separator = (String) value;
        } else {
            separator = "|";
        }
    }

    @Override
    public String deserialize(String topic, byte[] data) {

        final byte[] bytes = new byte[data.length - SUFFIX_SIZE];

        System.arraycopy(data, 0, bytes, 0, bytes.length);

        final String key = inner.deserialize(topic, bytes);

        final ByteBuffer buffer = ByteBuffer.wrap(data);

        final long start = buffer.getLong(data.length - SUFFIX_SIZE);
        final int sequence = buffer.getInt(data.length - SEQNUM_SIZE);

        return key + separator + format(start, TIME_FORMATTER) + separator + format(start + windowSize, TIME_FORMATTER);
    }

    private String format(final long ts, final DateTimeFormatter formatter) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(formatter);
    }

}
