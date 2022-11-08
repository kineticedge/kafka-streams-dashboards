#!/bin/sh

#curl -s http://localhost:7072/jolokia/read/kafka.streams:type=kafka-metrics-count | sed -E "s/.*\{\"count\":([0-9\.]+)\}.*/\1/"

COUNT=$(curl -s http://localhost:7072/jolokia/read/kafka.streams:type=kafka-metrics-count | jq -r ".value.count")

[ "$COUNT" = "null" ] && echo 1

[ "$COUNT" -ge 1 ]
