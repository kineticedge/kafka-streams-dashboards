#!/bin/bash

echo ""
echo "deleting LogAndAppend message.timestamp.type from 'product-statistics-HOPPING'"
echo ""

kafka-configs --bootstrap-server localhost:19092 --alter --entity-type topics --entity-name product-statistics-HOPPING --delete-config message.timestamp.type


