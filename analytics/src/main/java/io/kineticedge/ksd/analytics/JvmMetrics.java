package io.kineticedge.ksd.analytics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

import java.util.ArrayList;
import java.util.List;

public final class JvmMetrics implements AutoCloseable {

    private final List<MeterBinder> binders = new ArrayList<>();

    public JvmMetrics(MeterRegistry registry) {
        binders.add(new ClassLoaderMetrics());
        binders.add(new JvmMemoryMetrics());
        binders.add(new JvmGcMetrics());
        binders.add(new ProcessorMetrics());
        binders.add(new JvmThreadMetrics());
        binders.add(new JvmThreadDeadlockMetrics());
        binders.add(new JvmInfoMetrics());

        binders.forEach(b -> {
            if (b instanceof MeterBinder binder) {
                binder.bindTo(registry);
            }
        });
    }

    @Override
    public void close() {
        binders.forEach(b -> {
            try {
                if (b instanceof AutoCloseable a) {
                    a.close();
                }
            } catch (Exception ignored) {

            }
        });
    }
}
