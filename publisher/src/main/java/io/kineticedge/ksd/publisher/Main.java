package io.kineticedge.ksd.publisher;

import io.kineticedge.ksd.tools.config.OptionsUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    final static Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println("Uncaught exception in thread '" + t.getName() + "': " + e.getMessage());
        }
    };

    public static void main(String[] args) throws Exception {

        final Options options = OptionsUtil.parse(Options.class, args);

        if (options == null) {
            return;
        }

        // TODO read user,store,product to get set of values....


        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = Executors.defaultThreadFactory().newThread(r);
            //t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });

        Future<?> future = executor.submit(() -> {
            new Producer(options).start();
        });

        try {
            future.get();
        } catch (Exception e) {
            e.printStackTrace();;
        }

    }

}

