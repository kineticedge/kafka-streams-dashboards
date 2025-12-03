package io.kineticedge.io.ksd.observer.old;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kineticedge.ksd.common.domain.PurchaseOrder;
import io.kineticedge.ksd.tools.config.KafkaEnvUtil;
import io.kineticedge.ksd.tools.serde.JsonDeserializer;
import io.kineticedge.ksd.tools.serde.JsonSerializer;
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
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

public class ObserverBackup {

  private static final Logger log = LoggerFactory.getLogger(ObserverBackup.class);

  private static final ObjectMapper objectMapper =
          new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .setTimeZone(TimeZone.getDefault())
                  .registerModule(new JavaTimeModule());

  record OllamaRequest(String model, String prompt, long[] context, Boolean stream) {
  }

  record OllamaResponse(
          String model,
          String created_at,
          String response,
          Boolean done,
          Boolean done_reason,
          long[] context,
          long total_duration,
          long load_duration,
          long prompt_eval_count,
          long prompt_eval_duration
  ) {
  }


  private final Options options;

  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  private KafkaConsumer<String, PurchaseOrder> consumer;
  private KafkaProducer<String, PurchaseOrder> producer;

  private long lastOffsetCommitMs;
  private boolean shuttingDown = false;
  private boolean exitingOnSendFailure = false;

  private final long offsetCommitIntervalMs;
  private final boolean abortOnSendFailure;
  private final Duration pollDuration;

  private final CloseableHttpClient httpClient;

  public ObserverBackup(final Options options) {
    this.options = options;

    this.offsetCommitIntervalMs = 10_000L;
    this.abortOnSendFailure = true;
    this.pollDuration = Duration.ofMillis(500L);

    consumer = new KafkaConsumer<>(consumerConfig(options));
    producer = new KafkaProducer<>(producerConfig(options));

    httpClient = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    //.setConnectTimeout(Timeout.ofSeconds(3))
                    .setResponseTimeout(Timeout.ofSeconds(5))
                    .build())
            .build();


    lastOffsetCommitMs = System.currentTimeMillis();
  }

  public void start() {
    try {

      consumer.subscribe(Collections.singleton(options.getInputTopic()), new ConsumerRebalanceListener() {
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> collection) {
          log.info("partitions revoked: {}", collection);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> collection) {
          log.info("partitions assigned: {}", collection);
        }
      });

      while (!shuttingDown) {
        var messages = consumer.poll(pollDuration);
        maybeFlushAndCommitOffsets();
      }
    } catch (WakeupException e) {
      // do nothing, only using wakeup during shutdown, for exiting of poll() to allow quicker shutdown.
    } catch (KafkaException e) {
      log.debug("kafka exception shuttingDown={}, exitingOnSendFailure={}", shuttingDown, exitingOnSendFailure);
      // if shutting down, do not throw the exception, ignore it as it is most likely a result of shutting down.
      if (!shuttingDown && !exitingOnSendFailure) {
        throw e;
      }
    } finally {
      try {
        log.debug("shutting down, flushing producer.");
        producer.flush();
        log.debug("committing offsets.");
        commitOffsets();
      } catch (Exception e) {
        log.warn("unable to cleanly flush producer or commit offsets.", e);
      }

      try {
        log.debug("closing producer.");
        producer.close();
        log.debug("closing consumer.");
        consumer.close();
      } finally {
        shutdownLatch.countDown();

        if (!shuttingDown) {
          log.error("abnormal thread termination, mirror-maker exiting.");
        }
      }
    }

  }

  private void maybeFlushAndCommitOffsets() {
    if (System.currentTimeMillis() - lastOffsetCommitMs > offsetCommitIntervalMs) {
      producer.flush();
      commitOffsets();
      lastOffsetCommitMs = System.currentTimeMillis();
    }
  }

  private void commitOffsets() {
    if (!exitingOnSendFailure) {
      int retry = 0;
      boolean retryNeeded = true;
      while (retryNeeded) {
        try {
          //commit();
          retryNeeded = false;
        } catch (final WakeupException e) {
          handleWakeupException();
          throw e;
        } catch (final TimeoutException e) {
          retry += 1;
          //handleTimeoutException(retry);
        } catch (final CommitFailedException e) {
          retryNeeded = false;
          //handleCommitFailedException();
        }
      }
    } else {
      log.info("exiting on send failure, skip committing offsets.");
    }
  }


  private void handleWakeupException() {
    commitOffsets();
  }


  private Map<String, Object> consumerConfig(final Options options) {
    Map<String, Object> defaults = Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
            Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()),
            Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName())
    );
    final Map<String, Object> map = new HashMap<>(defaults);
    map.putAll(new KafkaEnvUtil().to("KAFKA_"));
    return map;
  }

  private Map<String, Object> producerConfig(final Options options) {
    Map<String, Object> defaults = Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
            Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
            Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
            Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
            Map.entry(ProducerConfig.ACKS_CONFIG, "all"),
            Map.entry(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip")
    );
    final Map<String, Object> map = new HashMap<>(defaults);
    map.putAll(new KafkaEnvUtil().to("KAFKA_"));
    return map;
  }

  public static Properties toProperties(final Map<String, Object> map) {
    final Properties properties = new Properties();
    properties.putAll(map);
    return properties;
  }


  private void inquire() throws IOException, ParseException {

    /*
    ## Current Top-Selling SKUs
- Widget A (SKU A123): 120 units
- Widget B (SKU B456): 95 units

## Changes Since Last Snapshot
- Widget A: +20 units, rank +1
- Widget C: −15 units, rank −2
- Widget D: new entry

## Task
Analyze emerging sales trends. Which SKUs are gaining momentum? Which are declining?
     */

    String prompt = """
            You are a price validation assistant. Given the price $100.00,
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
                            null,
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