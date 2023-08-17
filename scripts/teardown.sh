#!/bin/sh

alias dc='docker compose'

(cd applications; dc down -v)
(cd monitoring; dc down -v)

#kraft cluster
(cd cluster; dc down -v)

#zookeeper cluster
(cd cluster-zk; dc down -v)

#sasl cluster
(cd cluster-sasl; dc down -v)

#kraft load-balanced cluster
(cd cluster-lb; dc down -v)

rm -fr applications/stores/analytics_tumbling
rm -fr applications/stores/analytics_hopping
rm -fr applications/stores/analytics_sliding
rm -fr applications/stores/analytics_session

#docker network rm ksd
