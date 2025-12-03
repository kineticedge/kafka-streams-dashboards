package io.kineticedge.io.ksd.observer.old;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kineticedge.ksd.tools.config.OptionsUtil;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.TimeZone;

public class Abc2 {

  public static void main(String[] args) throws Exception {

    final Options options = OptionsUtil.parse(Options.class, args);

    new Abc2(options).inquire();
  }

  private static final ObjectMapper objectMapper =
          new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .setTimeZone(TimeZone.getDefault())
                  .registerModule(new JavaTimeModule());

  record OllamaRequest(String model, String prompt, Boolean stream) {
  }

  record OllamaResponse(String model, String response) {
  }

  private final Options options;

  private final CloseableHttpClient httpClient;

  public Abc2(final Options options) {
    this.options = options;
    httpClient = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    //.setConnectTimeout(Timeout.ofSeconds(3))
                    .setResponseTimeout(Timeout.ofSeconds(60))
                    .build())
            .build();
  }



  private void inquire() throws IOException, ParseException {

    String prompt = """
You are a price validation assistant. For a "MacBook Pro 14" Laptop - M3 Pro chip Built for Apple Intelligence - 18GB Memory - 18-core GPU - 1TB SSD"
 is the price of $1,499.99 ok?
 if it seems unusual or incorrect, respond only with "price looks incorrect".
 If the price seems normal, respond with "price looks normal".
 Keep your response exactly to these phrases only.
            """;

    HttpPost post = new HttpPost(options.getOllamaUrl());
    post.setHeader("Accept", "application/json");
    post.setHeader("Content-Type", "application/json");
    post.setEntity(new StringEntity(
            objectMapper.writeValueAsString(
                    new OllamaRequest(
                            options.getModelName(),
                            prompt,
                            false
                    )
            ), ContentType.APPLICATION_JSON)
    );

//    try (CloseableHttpResponse resp = httpClient.execute(post)) {
//      int status = resp.getCode();
//      String body = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
//      System.out.println("Status: " + status);
//      System.out.println("Body: " + body);
//    }

    String x = httpClient.execute(post, (ClassicHttpResponse resp) -> {
      int status = resp.getCode();
      String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
      if (status >= 200 && status < 300) {
        return respBody;
      }
      throw new RuntimeException("HTTP " + status + ": " + respBody);
    });

    System.out.println(x)
    ;
//    Request request = new Request.Builder()
//            .url(options.getOllamaUrl())
//            .post(
//                    RequestBody.create(
//                            MediaType.parse("application/json"),
//                            objectMapper.writeValueAsString(new OllamaRequest("", prompt, false))
//                    )
//            )
//            .build();
//
//    httpClient.newCall(request) {
//      response ->
//      if (!response.isSuccessful) throw RuntimeException("Failed to call Ollama: ${response.code()}")
//
//      val ollamaResponse = objectMapper.readValue(response.body() ?.string(), OllamaResponse::class.java)
//      return ollamaResponse.response.trim()
//
//    }

  }

}