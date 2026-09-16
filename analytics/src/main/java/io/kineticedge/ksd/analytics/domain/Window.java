package io.kineticedge.ksd.analytics.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


public class Window implements Comparable<Window> {

    public static final Window NONE = new Window(0L, 0L);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final long start;
    private final long end;


  public Window(long start, long end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Window window = (Window) o;
    return start == window.start && end == window.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
    public int compareTo(final Window o) {
        if (this == o) {
            return 0;
        } else if (start < o.start) {
            return -1;
        } else if (start > o.start) {
            return 1;
        } else if (end < o.end) {
            return -1;
        } else if (end > o.end) {
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return "[" + convert(start) + ", " + convert(end) + "]";
    }

    public String start() {
        return convert(start);
    }

    public String end() {
        return convert(end);
    }

    private String convert(final long ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    /**
     * need a comparable window.
     */
    public static Window convert(final org.apache.kafka.streams.kstream.Window window) {

        if (window == null) {
           return Window.NONE;
        }

        return new Window(window.start(), window.end());
    }

//    public static void main(String[] args) {
//        System.out.println(Utils.toPositive(Utils.murmur2("0000005348".getBytes())) % 4);
//    }

}
