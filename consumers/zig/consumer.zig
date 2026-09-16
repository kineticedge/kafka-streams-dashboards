const std = @import("std");

const c = @cImport({
    @cInclude("librdkafka/rdkafka.h");
});

pub fn main() !void {

    const allocator = std.heap.c_allocator;

    const args = try std.process.argsAlloc(allocator);
    defer std.process.argsFree(allocator, args);

    // Check if --sasl flag is provided
    var use_sasl = false;

    var i: usize = 1;
    while (i < args.len) : (i += 1) {
        if (std.mem.eql(u8, args[i], "--sasl")) {
            use_sasl = true;
        }
    }

    // Consumer setup
    const conf = c.rd_kafka_conf_new();
    if (c.rd_kafka_conf_set(conf, "bootstrap.servers", "localhost:9092", null, 0) != c.RD_KAFKA_CONF_OK) {
        std.log.err("Failed to set bootstrap servers", .{});
        return;
    }

    if (c.rd_kafka_conf_set(conf, "client.id", "zig_consumer", null, 0) != c.RD_KAFKA_CONF_OK) {
        std.log.err("Failed to set client.id", .{});
        return;
    }

    if (use_sasl) {
        if (c.rd_kafka_conf_set(conf, "security.protocol", "SASL_PLAINTEXT", null, 0) != c.RD_KAFKA_CONF_OK) {
            std.log.err("Failed to set security.protocol", .{});
            return;
        }

        if (c.rd_kafka_conf_set(conf, "sasl.mechanisms", "SCRAM-SHA-512", null, 0) != c.RD_KAFKA_CONF_OK) {
            std.log.err("Failed to set sasl.mechanisms", .{});
            return;
        }

        if (c.rd_kafka_conf_set(conf, "sasl.username", "zig-producer", null, 0) != c.RD_KAFKA_CONF_OK) {
            std.log.err("Failed to set sasl.username", .{});
            return;
        }

        if (c.rd_kafka_conf_set(conf, "sasl.password", "zig-producer-password", null, 0) != c.RD_KAFKA_CONF_OK) {
            std.log.err("Failed to set sasl.password", .{});
            return;
        }
    }

    if (c.rd_kafka_conf_set(conf, "group.id", "example-group", null, 0) != c.RD_KAFKA_CONF_OK) {
        std.log.err("Failed to set group.id", .{});
        return;
    }


    // Create consumer instance
    var errstr: [512]u8 = undefined;
    const rk = c.rd_kafka_new(c.RD_KAFKA_CONSUMER, conf, &errstr[0], errstr.len);
    if (rk == null) {
        std.log.err("Failed to create consumer: {s}", .{&errstr});
        return;
    }

    defer c.rd_kafka_destroy(rk); // Clean up consumer on exit

    // Subscribe to topic
    const topics = [_]*const u8{
        @ptrCast("example-topic".ptr),
        //"example-topic",
    };
   // const topic_list = c.rd_kafka_topic_partition_list_new(@intCast(c.int, topics.len));
    const topic_list = c.rd_kafka_topic_partition_list_new(topics.len);

    defer c.rd_kafka_topic_partition_list_destroy(topic_list);
    for (topics) |topic| {
        _ = c.rd_kafka_topic_partition_list_add(topic_list, topic, c.RD_KAFKA_PARTITION_UA);
    }

    if (c.rd_kafka_subscribe(rk, topic_list) != 0) {
        std.log.err("Failed to subscribe to topic", .{});
        return;
    }

    // Poll for messages
    while (true) {
        const msg = c.rd_kafka_consumer_poll(rk, 1000); // 1s timeout

        if (msg != null) {
            if (msg.*.err != c.RD_KAFKA_RESP_ERR_NO_ERROR) {
                //std.log.err("Consumer error: {s}", .{@ptrCast([*]const u8, c.rd_kafka_message_errstr(msg))});
            } else {
                //const key: [*:0]const u8 = @ptrCast(msg.*.key);
                const value: [*:0]const u8 = @ptrCast(msg.*.payload);
                std.log.info("value={?s}", .{value});
            }
            c.rd_kafka_message_destroy(msg);
        }
    }
}
