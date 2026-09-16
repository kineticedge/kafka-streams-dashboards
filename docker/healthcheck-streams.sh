#!/bin/sh

STATE=$(curl -s "http://localhost:7072/jolokia/read/kafka.streams:type=stream-metrics,client-id=*/state" | jq -r '.value | to_entries | .[0].value.state')

[ "$STATE" = "RUNNING" ]
