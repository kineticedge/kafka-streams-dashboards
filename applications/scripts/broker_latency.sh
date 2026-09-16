#!/bin/bash

LATENCY="${1}ms"
shift

echo "latency: $LATENCY"

tc qdisc del dev eth0 root
tc qdisc add dev eth0 root handle 1: prio
tc qdisc add dev eth0 parent 1:3 handle 30: netem delay ${LATENCY}


declare -a HOSTS=(broker-1 broker-2 broker-3 broker-4)

for host in "${HOSTS[@]}"; do

  ip=$(ping -c 1 $host | head -1 | awk -F'[()]' '/PING/{print $2}')

  echo "$host : $ip"
  tc filter add dev eth0 protocol ip parent 1:0 prio 3 u32 match ip dst $ip flowid 1:3

done



