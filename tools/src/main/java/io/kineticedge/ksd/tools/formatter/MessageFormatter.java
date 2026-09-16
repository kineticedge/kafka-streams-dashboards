package io.kineticedge.ksd.tools.formatter;

import java.io.PrintStream;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class MessageFormatter implements org.apache.kafka.common.MessageFormatter {

    @Override
    public void writeTo(ConsumerRecord<byte[], byte[]> consumerRecord, PrintStream output) {

    }

    @Override
    public void configure(Map<String, ?> configs) {
        org.apache.kafka.common.MessageFormatter.super.configure(configs);
    }

    @Override
    public void close() {
        org.apache.kafka.common.MessageFormatter.super.close();
    }

    //    class DefaultMessageFormatter extends MessageFormatter {
//        var printTimestamp = false
//        var printKey = false
//        var printValue = true
//        var printPartition = false
//        var printOffset = false
//        var printHeaders = false
//        var keySeparator = utfBytes("\t")
//        var lineSeparator = utfBytes("\n")
//        var headersSeparator = utfBytes(",")
//        var nullLiteral = utfBytes("null")
//
//        var keyDeserializer: Option[Deserializer[_]] = None
//        var valueDeserializer: Option[Deserializer[_]] = None
//        var headersDeserializer: Option[Deserializer[_]] = None
//
//        override def configure(configs: Map[String, _]): Unit = {
//            getPropertyIfExists(configs, "print.timestamp", getBoolProperty).foreach(printTimestamp = _)
//            getPropertyIfExists(configs, "print.key", getBoolProperty).foreach(printKey = _)
//            getPropertyIfExists(configs, "print.offset", getBoolProperty).foreach(printOffset = _)
//            getPropertyIfExists(configs, "print.partition", getBoolProperty).foreach(printPartition = _)
//            getPropertyIfExists(configs, "print.headers", getBoolProperty).foreach(printHeaders = _)
//            getPropertyIfExists(configs, "print.value", getBoolProperty).foreach(printValue = _)
//            getPropertyIfExists(configs, "key.separator", getByteProperty).foreach(keySeparator = _)
//            getPropertyIfExists(configs, "line.separator", getByteProperty).foreach(lineSeparator = _)
//            getPropertyIfExists(configs, "headers.separator", getByteProperty).foreach(headersSeparator = _)
//            getPropertyIfExists(configs, "null.literal", getByteProperty).foreach(nullLiteral = _)
//
//            keyDeserializer = getPropertyIfExists(configs, "key.deserializer", getDeserializerProperty(true))
//            valueDeserializer = getPropertyIfExists(configs, "value.deserializer", getDeserializerProperty(false))
//            headersDeserializer = getPropertyIfExists(configs, "headers.deserializer", getDeserializerProperty(false))
//        }
//
//        def writeTo(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]], output: PrintStream): Unit = {
//
//            def writeSeparator(columnSeparator: Boolean): Unit = {
//            if (columnSeparator)
//                output.write(keySeparator)
//            else
//                output.write(lineSeparator)
//    }
//
//            def deserialize(deserializer: Option[Deserializer[_]], sourceBytes: Array[Byte], topic: String) = {
//                val nonNullBytes = Option(sourceBytes).getOrElse(nullLiteral)
//                val convertedBytes = deserializer
//                        .map(d => utfBytes(d.deserialize(topic, consumerRecord.headers, nonNullBytes).toString))
//        .getOrElse(nonNullBytes)
//                convertedBytes
//            }
//
//    import consumerRecord._
//
//            if (printTimestamp) {
//                if (timestampType != TimestampType.NO_TIMESTAMP_TYPE)
//                    output.write(utfBytes(s"$timestampType:$timestamp"))
//                else
//                    output.write(utfBytes("NO_TIMESTAMP"))
//                writeSeparator(columnSeparator =  printOffset || printPartition || printHeaders || printKey || printValue)
//            }
//
//            if (printPartition) {
//                output.write(utfBytes("Partition:"))
//                output.write(utfBytes(partition().toString))
//                writeSeparator(columnSeparator = printOffset || printHeaders || printKey || printValue)
//            }
//
//            if (printOffset) {
//                output.write(utfBytes("Offset:"))
//                output.write(utfBytes(offset().toString))
//                writeSeparator(columnSeparator = printHeaders || printKey || printValue)
//            }
//
//            if (printHeaders) {
//                val headersIt = headers().iterator.asScala
//                if (headersIt.hasNext) {
//                    headersIt.foreach { header =>
//                        output.write(utfBytes(header.key() + ":"))
//                        output.write(deserialize(headersDeserializer, header.value(), topic))
//                        if (headersIt.hasNext) {
//                            output.write(headersSeparator)
//                        }
//                    }
//                } else {
//                    output.write(utfBytes("NO_HEADERS"))
//                }
//                writeSeparator(columnSeparator = printKey || printValue)
//            }
//
//            if (printKey) {
//                output.write(deserialize(keyDeserializer, key, topic))
//                writeSeparator(columnSeparator = printValue)
//            }
//
//            if (printValue) {
//                output.write(deserialize(valueDeserializer, value, topic))
//                output.write(lineSeparator)
//            }
//        }
//
//        private def propertiesWithKeyPrefixStripped(prefix: String, configs: Map[String, _]): Map[String, _] = {
//            val newConfigs = collection.mutable.Map[String, Any]()
//            configs.asScala.foreach { case (key, value) =>
//                if (key.startsWith(prefix) && key.length > prefix.length)
//                    newConfigs.put(key.substring(prefix.length), value)
//            }
//            newConfigs.asJava
//        }
//
//        private def utfBytes(str: String) = str.getBytes(StandardCharsets.UTF_8)
//
//        private def getByteProperty(configs: Map[String, _], key: String): Array[Byte] = {
//            utfBytes(configs.get(key).asInstanceOf[String])
//        }
//
//        private def getBoolProperty(configs: Map[String, _], key: String): Boolean = {
//            configs.get(key).asInstanceOf[String].trim.equalsIgnoreCase("true")
//        }
//
//        private def getDeserializerProperty(isKey: Boolean)(configs: Map[String, _], propertyName: String): Deserializer[_] = {
//            val deserializer = Class.forName(configs.get(propertyName).asInstanceOf[String]).getDeclaredConstructor().newInstance().asInstanceOf[Deserializer[_]]
//            val deserializerConfig = propertiesWithKeyPrefixStripped(propertyName + ".", configs)
//                    .asScala
//                    .asJava
//            deserializer.configure(deserializerConfig, isKey)
//            deserializer
//        }
//
//        private def getPropertyIfExists[T](configs: Map[String, _], key: String, getter: (Map[String, _], String) => T): Option[T] = {
//            if (configs.containsKey(key))
//                Some(getter(configs, key))
//            else
//                None
//        }
//    }

}
