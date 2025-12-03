package io.kineticedge.io.ksd.observer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class Observer {

  private static final String USER = "user";
  private static final String ASSISTANT = "assistant";
  private static final String SYSTEM = "system";

  public static void main(String[] args) throws Exception {
    new Observer().inquire("how are you today");
  }

  private List<Message> messages = new ArrayList<>();

  private static final Logger log = LoggerFactory.getLogger(Observer.class);



  private static final ObjectMapper objectMapper =
          new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .setTimeZone(TimeZone.getDefault())
                  .registerModule(new JavaTimeModule());

  record Message(String role, String content) {}

  record OllamaRequest(
          String model,
          Boolean stream,
          List<Message> messages,
          BigDecimal top_k,
          BigDecimal temperature
  ) {}

  record OllamaResponse(
          String model,
          String created_at,
          Message message,
          Boolean done,
          String done_reason,
          long total_duration,
          long load_duration,
          long prompt_eval_count,
          long prompt_eval_duration,
          long eval_count,
          long eval_duration
  ) {}


  /*
  {
  "stream": false,
  "model": "deepseek-r1",
  "messages": [
    { "role": "system", "content": "You are a helpful assistant." },
    { "role": "user", "content": "What is Kafka?" },
    { "role": "assistant", "content": "Kafka is a distributed event streaming platform..." },
    { "role": "user", "content": "How does it handle SSL?" }
  ]
}

  */

  private String ollamaUrl = "http://localhost:11434/api/chat";
  private String modelName = "deepseek-r1";

  private final CloseableHttpClient httpClient;

  public Observer() {

    httpClient = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    //.setConnectTimeout(Timeout.ofSeconds(10))
                    .setResponseTimeout(Timeout.ofSeconds(60))
                    .build())
            .build();


    messages.add(new Message(SYSTEM, "the time windows are small for demo purposes, so don't worry about that; it is correct."));
    messages.add(new Message(SYSTEM, "focus on trends, predict future purchases"));
    messages.add(new Message(SYSTEM, "You are a concise assistant. Do not include any <think> sections or internal reasoning in your responses. Only return direct answers or summaries."));

  }



  public void inquire(String prompt) throws IOException {

    List<Message> request = new ArrayList<>(this.messages);
    request.add(new Message("user", prompt));

    HttpPost post = new HttpPost(ollamaUrl);
    post.setHeader("Accept", "application/json");
    post.setHeader("Content-Type", "application/json");
    post.setEntity(new StringEntity(
            objectMapper.writeValueAsString(
                    new OllamaRequest(
                            modelName,
                            false,
                            request,
                            null,
                            null
                    )
            ), ContentType.APPLICATION_JSON)
    );

    OllamaResponse response = httpClient.execute(post, (ClassicHttpResponse resp) -> {
      final int status = resp.getCode();
      if (status >= 200 && status < 300) {
        return objectMapper.readValue(resp.getEntity().getContent(), OllamaResponse.class);
      }
      throw new RuntimeException("status=" + status);
    });

    //store response for future analysis calls, trim/condense as needed.
    messages.add(response.message());

  }

}