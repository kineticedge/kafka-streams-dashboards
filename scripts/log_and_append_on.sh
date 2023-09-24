#!/bin/bash

echo ""
echo "changing message.timestamp.type of 'product-statistics-HOPPING' to LogAppendTime"
echo ""

kafka-configs --bootstrap-server localhost:19092 --alter --entity-type topics --entity-name product-statistics-HOPPING --add-config message.timestamp.type=LogAppendTime

