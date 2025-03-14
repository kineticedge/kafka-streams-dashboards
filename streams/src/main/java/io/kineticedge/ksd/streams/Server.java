package io.kineticedge.ksd.streams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.kineticedge.ksd.common.domain.util.HttpUtils;
import io.kineticedge.ksd.streams.domain.ByOrderId;
import io.kineticedge.ksd.streams.jackson.ByOrderIdSerializer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public class Server {

  private static final ObjectMapper OBJECT_MAPPER =
          new ObjectMapper()
                  .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .registerModule(new SimpleModule("uuid-module", new Version(1, 0, 0, null, "", ""))
                          .addSerializer(ByOrderId.class, new ByOrderIdSerializer())
                  ).registerModule(new JavaTimeModule());

  private final StateObserver stateObserver;
  private final PrometheusMeterRegistry prometheusMeterRegistry;
  private final int port;

  public Server(StateObserver stateObserver, PrometheusMeterRegistry prometheusMeterRegistry, int port) {
    this.stateObserver = stateObserver;
    this.prometheusMeterRegistry = prometheusMeterRegistry;
    this.port = port;
  }

  public void start() {

    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

      server.createContext("/metrics", exchange -> {
        exchange.getResponseHeaders().set("Content-Type", "html/text");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);
        try (exchange; OutputStream os = exchange.getResponseBody()) {
          prometheusMeterRegistry.scrape(os);
        }
      });

      server.createContext("/", new CustomHandler());

      //server.setExecutor(null); // Use default executor
      server.setExecutor(Executors.newFixedThreadPool(4));

      server.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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