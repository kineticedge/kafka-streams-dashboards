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
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.TimeZone;

public class Abc {

  private long[] context = new long[] {151669,2610,525,264,1985,9149,6358,17847,624,4854,882,498,525,2598,11,498,686,387,2661,501,1909,11250,315,3871,323,12194,14733,624,785,2266,504,3681,6738,686,387,3897,369,279,13656,9149,7388,382,515,5507,25,330,16,15,25,15,15,12,16,15,25,18,15,756,10144,64772,4913,39929,1,549,330,32,16,17,18,497,330,13523,1,549,220,16,17,15,1583,4913,39929,1,549,330,33,19,20,21,497,330,13523,1,549,220,24,20,532,921,532,151670,151667,198,32313,11,279,1196,702,2661,752,264,3239,311,738,705,438,264,1985,9149,6358,17847,13,2379,2299,8241,458,3110,2266,448,264,882,3241,315,330,16,15,25,15,15,12,16,15,25,18,15,1,323,1378,3871,11,362,16,17,18,323,425,19,20,21,11,448,32676,220,16,17,15,323,220,24,20,15576,382,2121,18183,39350,11,358,1184,311,3535,419,7225,13,576,1196,6801,752,311,23643,1985,18339,3118,389,12194,14733,821,916,5189,882,11030,13,2379,3003,3897,458,3110,3561,369,279,2266,1995,382,40,1265,1744,911,1246,311,5944,847,2033,13,8704,807,8990,944,2661,264,3151,3239,3602,11,358,3278,1184,311,3783,369,1105,1573,8241,5042,9149,6358,13,1988,304,419,1142,11,2474,902,3405,374,4588,11,358,3278,4658,387,3601,311,3410,1045,3093,315,2856,2033,476,7600,738,382,785,1196,4977,8014,304,821,41048,323,22845,481,8365,807,2299,264,10806,6645,476,2562,18237,14849,6625,2337,3151,4115,1075,16162,11919,3039,13,2379,2578,1366,25709,389,1985,22538,12624,6814,279,1899,2041,10161,5961,382,40,1265,2908,1128,1995,358,646,8649,504,419,3110,2266,25,715,12,5643,362,16,17,18,702,5080,12194,320,16,17,15,8,1091,425,19,20,21,320,24,20,340,12,576,882,3241,374,1172,220,18,15,4420,11,892,13388,6358,7990,198,12,2308,13656,821,311,9429,2348,271,2461,847,2033,5944,11,358,3278,1184,311,510,840,20772,1128,3093,315,4755,807,646,2548,752,911,1985,18339,198,60424,10295,3118,389,279,2661,2266,198,3430,5527,369,3853,19556,429,2578,2924,5248,11030,476,803,6351,7388,271,40,1265,1083,1744,911,4650,1795,5239,4755,279,1196,2578,614,13,2379,1410,1366,2155,882,18346,29139,11,35495,1948,3871,323,11059,11,476,25709,1119,3170,3654,12624,3000,382,5050,2033,3880,311,387,10950,714,537,22024,448,6358,3080,807,2548,11689,13,358,3278,5244,389,1660,2797,911,1128,1995,358,646,3410,3118,389,862,19556,624,151668,198,32313,11,358,2776,5527,0,5209,2968,752,697,3239,382,40,3535,279,2266,1447,9,256,3070,1462,13642,66963,220,16,15,25,15,15,12,16,15,25,18,15,320,64,1602,3151,323,2805,69953,340,9,256,3070,17746,609,31441,1361,25,1019,262,353,256,77986,362,16,17,18,25,220,16,17,15,8153,198,262,353,256,77986,425,19,20,21,25,220,24,20,8153,271,40,646,23643,18339,369,3871,3118,389,862,12194,14733,821,2878,5189,882,11030,13,1446,646,2548,752,4755,1075,1447,9,256,330,3838,572,279,9149,315,1985,362,16,17,18,304,279,3241,1565,55,63,47369,9,256,330,27374,1985,362,16,17,18,323,425,19,20,21,2337,279,4168,1565,56,63,10040,9,256,320,29982,11,979,803,13656,821,374,3897,8,330,4340,1558,419,9429,311,3681,18339,11974,334,5501,3410,697,3151,3239,476,882,3241,369,6358,13,56177,2461,3110,11,498,1410,2548,1447,1,2082,55856,6625,18339,304,279,3241,364,16,15,25,15,15,12,16,15,25,18,15,6,2217,2195,8365,2937,1447,1,27374,1985,362,16,17,18,594,5068,2337,364,16,15,25,15,15,12,16,15,25,18,15,6,323,364,16,20,25,15,15,12,16,20,25,18,15,6,504,13671,1189};
  public static void main(String[] args) throws Exception {

    final Options options = OptionsUtil.parse(Options.class, args);

    new Abc(options).inquire();
  }

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
          String done_reason,
          long[] context,
          long total_duration,
          long load_duration,
          long prompt_eval_count,
          long prompt_eval_duration
  ) {
  }

  private final Options options;

  private final CloseableHttpClient httpClient;

  public Abc(final Options options) {
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
You are a product trend analysis assistant.
Each time you are called, you will be given new top-N of products and quantity purchased.
The context from previous calls will be provided for the historical trend requests.



{
window: "10:30-11:00",
products:[
{"sku" : "A123", "quantity" : 110},
{"sku" : "B456", "quantity" : 93}
]
}
            """;

    prompt = "can you tell me what this context (provided with this request) says?";

    HttpPost post = new HttpPost(options.getOllamaUrl());
    post.setHeader("Accept", "application/json");
    post.setHeader("Content-Type", "application/json");
    post.setEntity(new StringEntity(
            objectMapper.writeValueAsString(
                    new OllamaRequest(
                            options.getModelName(),
                            prompt,
                            context,
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

    OllamaResponse x = httpClient.execute(post, (ClassicHttpResponse resp) -> {
      int status = resp.getCode();
      //String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
      if (status >= 200 && status < 300) {
        return objectMapper.readValue(resp.getEntity().getContent(), OllamaResponse.class);
        //return respBody;
      }
      throw new RuntimeException("HTTP " + status + ": " + "TBD");
    });

//    OllamaResponse ollamaResponse = objectMapper.readValue(x, OllamaResponse.class);
    System.out.println("Model: " + x.model());
    System.out.println("Response: " + x.response());
    System.out.println("LEN: " + x.context().length);
//    System.out.println(x)
  //  ;
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