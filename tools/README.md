## Tools

The tools project provides custom deserializers to use to inspect key elements on a change-log topic.

* `scripts/enable-custom-tools-derserializer` will create a symbolic link to the tools jar file. This allows
  for `kafka-console-consumer` to utilize those deserializers. Inspect the script before running, to understand
  the modification it will do (expecially if your installation of Apache Kafka is not Confluent's.)

```
kafka-console-consumer \
   --bootstrap-server localhost:19092 \
   --property print.timestamp=true \
   --property print.partition=true \
   --property print.key=true \
   --property key.separator=\| \
   --key-deserializer=io.kineticedge.ksd.tools.serde.SessionDeserializer \
   --topic analytics_session-SESSION-aggregate-purchase-order-changelog
```