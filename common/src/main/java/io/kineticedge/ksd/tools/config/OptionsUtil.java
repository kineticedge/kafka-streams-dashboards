package io.kineticedge.ksd.tools.config;

import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.BooleanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class OptionsUtil {

    private static final Pattern PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

    private static final String PREFIX = "";

    public static <T extends BaseOptions> T parse(final Class<T> optionsClass, final String[] args) {

        final T options = create(optionsClass);

        JCommander jCommander = JCommander.newBuilder()
                .addObject(options)
                .build();

        //easiest way to set configuration through docker.
        //similar to how Confluent does this in their Docker images.
        OptionsUtil.populateByEnvironment(options);

        jCommander.parse(args);

        if (options.isHelp()) {
            jCommander.usage();
            return null;
        }

        return options;
    }

    private static <T> T create(final Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> void populateByEnvironment(final T object) {
        Class<?> clazz = object.getClass();
        while (!clazz.equals(Object.class)) {
            populateByEnvironment(object, clazz);
            clazz = clazz.getSuperclass();
        }
    }


    private static <T> void populateByEnvironment(final T object, final Class<?> clazz) {

        Stream.of(clazz.getDeclaredFields()).forEach(f -> {

            final String environment = getEnvironmentVariable(f.getName());

            final String value = System.getenv(environment);

            if (value != null) {
                try {
                    f.setAccessible(true);
                    if (String.class.equals(f.getType())) {
                        f.set(object, value);
                    } else if (Boolean.TYPE.equals(f.getType()) || Boolean.class.equals(f.getType())) {
                        f.set(object, BooleanUtils.toBoolean(value));
                    } else if (Integer.TYPE.equals(f.getType()) || Integer.class.equals(f.getType())) {
                        f.set(object, Integer.parseInt(value));
                    } else if (Long.TYPE.equals(f.getType()) || Long.class.equals(f.getType())) {
                        f.set(object, Long.parseLong(value));
                    } else if (Enum.class.isAssignableFrom(f.getType())) {
                        f.set(object, create(f.getType(), value));
                    }
                } catch (final IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> create(final Class<?> type, final String value) {
        return Enum.valueOf((Class<Enum>) type, value);
    }

    private static String getEnvironmentVariable(final String string) {
        return PREFIX + PATTERN.matcher(string).replaceAll(match -> "_" + match.group()).toUpperCase();
    }

}
