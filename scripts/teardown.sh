#!/bin/sh

alias dc='docker compose'

(cd applications; dc down -v)
(cd monitoring; dc down -v)

# just go througha all clusters, regardless of which one was started.
(cd cluster; dc down -v)
(cd cluster-1; dc down -v)
(cd cluster-3ctrls; dc down -v)
(cd cluster-hybrid; dc down -v)
(cd cluster-zk; dc down -v)
(cd cluster-sasl; dc down -v)
(cd cluster-lb; dc down -v)
(cd cluster-native; dc down -v)
(cd cluster-cm; dc down -v)

rm -fr applications/stores/analytics_tumbling
rm -fr applications/stores/analytics_hopping
rm -fr applications/stores/analytics_sliding
rm -fr applications/stores/analytics_session
rm -fr applications/stores/analytics_none

#docker network rm ksd
