package io.kineticedge.ksd.streams;

import io.kineticedge.ksd.tools.config.OptionsUtil;

public class Main {

	public static void main(String[] args) throws Exception{

		final Options options = OptionsUtil.parse(Options.class, args);

		if (options == null) {
			return;
		}

		final Streams stream = new Streams();

		stream.start(options);


		/*
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        // 1. Stop HTTP server immediately (non-blocking)
        server.stop();

        // 2. Close Streams with a tight timeout
        streams.close(Duration.ofSeconds(3));

    } catch (Exception e) {
        log.error("Shutdown error", e);
    }
}));
		 */
	}

}

