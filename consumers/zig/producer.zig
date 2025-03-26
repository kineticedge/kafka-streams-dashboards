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

     const conf = c.rd_kafka_conf_new();
     if (c.rd_kafka_conf_set(conf, "bootstrap.servers", "localhost:9092", null, 0) != c.RD_KAFKA_CONF_OK) {
         std.log.err("Failed to set bootstrap servers", .{});
         return;
     }

     if (c.rd_kafka_conf_set(conf, "client.id", "zig_producer", null, 0) != c.RD_KAFKA_CONF_OK) {
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

     // Create producer instance
     var errstr: [512]u8 = undefined;
     const rk = c.rd_kafka_new(c.RD_KAFKA_PRODUCER, conf, &errstr[0], errstr.len);
     if (rk == null) {
         std.log.err("Failed to create producer: {s}", .{&errstr});
         return;
     }



     defer c.rd_kafka_destroy(rk); // Clean up producer on exit

     // Topic setup
     const topic_conf = c.rd_kafka_topic_conf_new();
     const rkt = c.rd_kafka_topic_new(rk, "example-topic", topic_conf);
     if (rkt == null) {
         std.log.err("Failed to create topic", .{});
         return;
     }

     defer c.rd_kafka_topic_destroy(rkt);

     // Produce a message to the topic
     const payload = "Hello, Kafka!";

     if (c.rd_kafka_produce(
         rkt,
         c.RD_KAFKA_PARTITION_UA, // Auto-partition
         c.RD_KAFKA_MSG_F_COPY,  // Copy payload
         @constCast(payload.ptr), //@ptrCast([*c]u8, &payload),
         payload.len, // Payload length
         null,        // No key
         0,           // Key length
         null         // No opaque pointer
     ) == -1) {
         //std.log.err("Failed to produce message: {s}", .{@ptrCast([*]const u8, c.rd_kafka_err2str(c.rd_kafka_last_error()))});
         return;
     } else {
         std.log.info("Message successfully sent to topic!", .{});
     }

     // Wait for the message to be delivered (flush)
     _ = c.rd_kafka_flush(rk, 10000); // 10s timeout
     std.log.info("Producer finished!", .{});
 }