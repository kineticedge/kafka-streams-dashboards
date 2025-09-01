package io.kineticedge.ksd.analytics;

import com.beust.jcommander.Parameter;
import io.kineticedge.ksd.tools.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
//@Setter
public class Options extends BaseOptions {

    public static enum WindowType {
        TUMBLING,                           // [12:00; 12:05), [12:05, 12:10), ...
        HOPPING,                            // [12:00; 12:05), [12:01; 12:06), ...
        SLIDING,
        SESSION,
        NONE,                                // no windowing
        NONE_REPARTITIONED                   // no windowing - repartitioned on incoming
    };

    @Parameter(names = { "-g", "--application-id" }, description = "application id")
    private String applicationId = "analytics_GRADLE";

    @Parameter(names = { "--client-id" }, description = "client id")
    private String clientId = "s-" + UUID.randomUUID();

    @Parameter(names = { "--auto-offset-reset" }, description = "where to start consuming from if no offset is provided")
    private String autoOffsetReset = "earliest";

    @Parameter(names = { "--window-size" }, description = "")
    private Integer windowSize = 30;

    @Parameter(names = { "--grace-period" }, description = "")
    private Integer gracePeriod = 5;

    @Parameter(names = { "--window-type" }, description = "")
    private WindowType windowType = WindowType.TUMBLING;

    @Parameter(names = { "--topic" }, description = "")
    private String topic = "pickup-order-handler-purchase-order-join-product-repartition";

    @Parameter(names = { "--restore-topic" }, description = "")
    private String restoreTopic = "pickup-order-handler-purchase-order-join-product-repartition-restore";

    @Parameter(names = { "--output-topic" }, description = "")
    private String outputTopic = "product-statistics";

    @Parameter(names = { "--commit-interval" }, description = "")
    private Long commitInterval = 5000L;

    @Parameter(names = { "--port" }, description = "the port use for introspection of window state")
    private Integer port = 8080;

}
