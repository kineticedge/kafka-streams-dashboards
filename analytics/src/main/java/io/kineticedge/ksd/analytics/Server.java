package io.kineticedge.ksd.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kineticedge.ksd.analytics.domain.BySku;
import io.kineticedge.ksd.analytics.domain.ByWindow;
import io.kineticedge.ksd.analytics.domain.Window;
import io.kineticedge.ksd.analytics.jackson.BySkuSerializer;
import io.kineticedge.ksd.analytics.jackson.ByWindowSerializer;
import io.kineticedge.ksd.analytics.jackson.WindowSerializer;
import io.kineticedge.ksd.common.domain.util.HttpUtils;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .registerModule(new SimpleModule("uuid-module", new Version(1, 0, 0, null, "", ""))
                            .addSerializer(ByWindow.class, new ByWindowSerializer())
                            .addSerializer(BySku.class, new BySkuSerializer())
                            .addSerializer(Window.class, new WindowSerializer())
                    ).registerModule(new JavaTimeModule());

    private final StateObserver stateObserver;
    private final PrometheusMeterRegistry prometheusMeterRegistry;
    private final int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public Server(StateObserver stateObserver,
                  PrometheusMeterRegistry prometheusMeterRegistry,
                  int port) {
        this.stateObserver = stateObserver;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
        this.port = port;
    }

    public void start() {
        // Daemon thread factories
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };

        bossGroup = new NioEventLoopGroup(1, daemonFactory, SelectorProvider.provider(), DefaultSelectStrategyFactory.INSTANCE);
        workerGroup = new NioEventLoopGroup(4, daemonFactory, SelectorProvider.provider(), DefaultSelectStrategyFactory.INSTANCE);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(1_048_576),
                                    new RequestHandler()
                            );
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            serverChannel = f.channel();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close(); // do NOT .sync() here, just fire-and-forget
            }
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
            }
        }
    }

    private class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

            try {
                if (!req.decoderResult().isSuccess()) {
                    sendError(ctx, req, HttpResponseStatus.BAD_REQUEST);
                    return;
                }

                String uri = req.uri();

                if (uri.startsWith("/metrics")) {
                    handleMetrics(ctx, req);
                } else {
                    handleState(ctx, req);
                }
            } catch (Exception e) {
                sendError(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }

        private void handleMetrics(ChannelHandlerContext ctx, FullHttpRequest req) {
            try {
                String body = prometheusMeterRegistry.scrape();
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

                FullHttpResponse res = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(bytes)
                );

                res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                res.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
                finalizeAndWrite(ctx, req, res);
            } catch (Exception e) {
                sendError(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

        private void handleState(ChannelHandlerContext ctx, FullHttpRequest req) {
            Map<String, String> params = HttpUtils.queryToMap(req.uri().contains("?")
                    ? req.uri().substring(req.uri().indexOf("?") + 1)
                    : null);

            String groupType = params.getOrDefault("group-type", "windowing");

            try {
                String json = OBJECT_MAPPER
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(stateObserver.getState(groupType));

                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

                FullHttpResponse res = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(bytes)
                );

                res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                res.headers().set("Access-Control-Allow-Origin", "*");
                res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
                finalizeAndWrite(ctx, req, res);

            } catch (Exception e) {
                sendError(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    private void finalizeAndWrite(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {

        boolean keepAlive = HttpUtil.isKeepAlive(req);
        if (keepAlive) {
            if (!req.protocolVersion().isKeepAliveDefault()) {
                res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
        } else {
            res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        ChannelFuture f = ctx.writeAndFlush(res);
        if (!keepAlive) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void sendError(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, String message) {
        String fullMessage = status.toString() + (message != null ? " (" + message + ")" : "");
        byte[] bytes = fullMessage.getBytes(StandardCharsets.UTF_8);

        FullHttpResponse res = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(bytes)
        );

        res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());

        finalizeAndWrite(ctx, req, res);
    }

    private void sendError(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
        sendError(ctx, req, status, null);
    }
}