package io.kineticedge.ksd.analytics;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.kineticedge.ksd.analytics.domain.BySku;
import io.kineticedge.ksd.analytics.domain.ByWindow;
import io.kineticedge.ksd.analytics.domain.Window;
import io.kineticedge.ksd.analytics.jackson.BySkuSerializer;
import io.kineticedge.ksd.analytics.jackson.ByWindowSerializer;
import io.kineticedge.ksd.analytics.jackson.WindowSerializer;
import io.kineticedge.ksd.common.domain.util.HttpUtils;
import io.kineticedge.ksd.common.metrics.MicrometerConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class Server {

  private HttpServer server;

  private static final ObjectMapper OBJECT_MAPPER =
          new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .registerModule(new SimpleModule("uuid-module", new Version(1, 0, 0, null, "", ""))
                          .addSerializer(ByWindow.class, new ByWindowSerializer())
                          .addSerializer(BySku.class, new BySkuSerializer())
                          .addSerializer(Window.class, new WindowSerializer())
                  ).registerModule(new JavaTimeModule());

  private final StateObserver stateObserver;
  private final int port;
  private final MicrometerConfig micrometerConfig;

  public Server(StateObserver stateObserver, MicrometerConfig micrometerConfig, int port) {
    this.stateObserver = stateObserver;
    this.micrometerConfig = micrometerConfig;
    this.port = port;
  }

  public void start() {
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);

      server.createContext("/metrics", exchange -> {
        exchange.getResponseHeaders().set("Content-Type", "html/text");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);
        try (exchange; OutputStream os = exchange.getResponseBody()) {
          micrometerConfig.scrape(os);
        }
      });

      server.createContext("/", new CustomHandler());

      server.setExecutor(Executors.newFixedThreadPool(4));

      server.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() {
    server.stop(0);
  }

  private class CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String groupType = "windowing";
      Map<String, String> queryParams = HttpUtils.queryToMap(exchange.getRequestURI().getQuery());
      if (queryParams.containsKey("group-type")) {
        groupType = queryParams.get("group-type");
      }

      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

      String jsonResponse = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(stateObserver.getState(groupType));
      exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(jsonResponse.getBytes());
      }
    }
  }

}