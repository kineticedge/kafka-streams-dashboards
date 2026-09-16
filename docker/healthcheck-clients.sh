#!/bin/sh

COUNT=$(curl -s http://localhost:7072/jolokia/read/java.lang:type=Runtime/Uptime | jq -r ".value")

[ "$COUNT" = "null" ] && echo 1

[ "$COUNT" -ge 1 ]
