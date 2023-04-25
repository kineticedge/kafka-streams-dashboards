
alias d='docker'
alias docker-compose='docker compose'
alias dn='docker network'

if ! [ -x "$(command -v docker)" ]; then
    echo "docker is not installed." >&2
    exit 1
fi

#if ! [ -x "$(command -v docker-compose)" ]; then
#    echo "docker-compose is not installed." >&2
#    exit 1
#fi

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

# using the cluster with zookeeper
(cd cluster-zk; docker-compose up -d --wait)

#(cd cluster-sasl; docker-compose up -d --wait)
#(cd cluster; docker-compose up -d --wait)

./gradlew build

(cd builder; ../gradlew run)
(cd monitoring; docker-compose up -d)
(cd applications; docker-compose up -d)

#avoid starting the analytic applications
#(cd applications; docker-compose up -d otel publisher stream)

# to start with a local publisher
#(cd applications; docker-compose up -d --wait $(docker-compose config --services | grep -v publisher))
#(cd publisher; ../gradlew run --args="--max-sku 100")
