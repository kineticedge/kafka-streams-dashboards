#!/bin/sh

alias dc='docker compose'

(cd applications; dc down -v)
(cd monitoring; dc down -v)
(cd cluster; dc down -v)
#in case started with legacy cluster
(cd cluster_zk; dc down -v)

rm -fr applications/stores/analytics_tumbling
rm -fr applications/stores/analytics_hopping
rm -fr applications/stores/analytics_sliding
rm -fr applications/stores/analytics_session

#docker network rm ksd
