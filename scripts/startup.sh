#!/bin/bash

set -e

function heading() {
  tput setaf 2; printf "\n\n$@"; tput sgr 0
  #pause
  tput setaf 2; printf "\n\n"; tput sgr 0
}

function subheading() {
  tput setaf 3; printf "$@\n"; tput sgr 0
}

function error_msg() {
  tput setaf 1; printf "\n$@\n\n"; tput sgr 0
}

cd "$(dirname -- "$0")/.." || exit


CLUSTERS=(
    "cluster-1"
    "cluster"
    "cluster-native"
    "cluster-3ctrls"
    "cluster-hybrid"
    "cluster-zk"
    "cluster-lb"
    "cluster-cm"
    "cluster-sasl"
)

CLUSTER_DESCRIPTIONS=(
    "cluster-1       --  1 node (broker and controller)"
    "cluster         --  4 brokers, 1 raft controller, kafka-exporter"
    "cluster-native  --  4 brokers, 1 raft controller, apache/kafka-native images"
    "cluster-3ctrls  --  4 brokers, 3 raft controllers"
    "cluster-hybrid  --  4 brokers, 1 dedicated raft controller, 2 brokers are also kraft controllers"
    "cluster-zk      --  4 brokers, 1 zookeeper controller"
    "cluster-lb      --  4 brokers, 1 raft controller, an nginx lb (9092)"
    "cluster-cm      --  3 brokers, 1 raft controller, kafka-exporter, otel collector client-metrics reporter"
    "cluster-sasl    --  3 brokers (SASL authentication), 1 raft controller, kafka-exporter, otel collector client-metrics reporter"
)

display_menu() {
    heading "Select a cluster:"
    for ((i=1; i<=${#CLUSTERS[@]}; i++)); do
        subheading "    $i. ${CLUSTER_DESCRIPTIONS[$i-1]}"
    done
    echo ""
}

if [ $# -eq 0 ]; then
  display_menu
  tput setaf 3; printf "Enter the number of your choice: "; tput sgr 0
  read -p "" choice
  if [[ $choice -ge 1 && $choice -le ${#CLUSTERS[@]} ]]; then
    CLUSTER=${CLUSTERS[$choice-1]}
  else
    echo "invalid selection"
    exit
  fi
else
  CLUSTER=$1
  shift
fi


if [[ ! $CLUSTER == "cluster"* ]]; then
  error_msg "invalid cluster, $CLUSTER."
  exit 0 
fi

if [ ! -d $CLUSTER ]; then
  error_msg "$CLUSTER does not exist"
  exit 0
fi


alias d='docker'
alias dn='docker network'

if ! [ -x "$(command -v docker)" ]; then
    echo "docker is not installed." >&2
    exit 1
fi

docker info > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "docker server is not running." >&2
  exit
fi

#
# creates a network unique to this project that can be shared between docker compose instances
# kafka-streams-dashboard -> ksd
#
NETWORK=$(docker network inspect -f '{{.Name}}' ksd 2>/dev/null)
if [ "$NETWORK" != "ksd" ]; then
  (docker network create ksd >/dev/null)
fi

heading "starting kafka cluster $CLUSTER"

(cd $CLUSTER; docker compose up -d --wait)

#if [[ "$CLUSTER" == "cluster-cm" || "$CLUSTER" == "cluster-sasl" ]]; then

APPLICATIONS_DIR="applications"

if [[ "$CLUSTER" == "cluster-cm" ]]; then
  heading "enabling client metrics communicated to the brokers."
  kafka-client-metrics --bootstrap-server localhost:9092 --alter --name EVERYTHING --metrics org.apache.kafka.  --interval 10000
fi

if [[ "$CLUSTER" == "cluster-sasl" ]]; then
  heading "creating sasl scram users (with full access) for applications."
  ./cluster-sasl/create-users.sh
  heading "enabling client metrics communicated to the brokers."
  kafka-client-metrics --bootstrap-server localhost:19092 --command-config ./cluster-sasl/secrets/admin.conf --alter --name EVERYTHING --metrics org.apache.kafka.  --interval 10000

  APPLICATIONS_DIR="applications-sasl"
fi

./gradlew build -x test

(cd builder; ./run.sh)
(cd monitoring; docker compose up -d)
#(cd monitoring; docker compose up -d $(docker compose config --services | grep -v tempo))

(cd "$APPLICATIONS_DIR"; docker compose up -d)
#(cd "$APPLICATIONS_DIR"; docker compose up -d publisher stream analytics-tumbling)
#(cd "$APPLICATIONS_DIR"; docker compose up -d $(docker compose config --services | grep -v otel))
