#!/bin/sh

set -e

alias dc='docker compose'

(cd applications; dc down -v)
(cd applications-sasl; dc down -v)
(cd monitoring; dc down -v)

# just go through all clusters, regardless of which one was started.
(cd cluster; dc down -v)
(cd cluster-1; dc down -v)
(cd cluster-3ctrls; dc down -v)
(cd cluster-hybrid; dc down -v)
(cd cluster-zk; dc down -v)
(cd cluster-sasl; dc down -v)
(cd cluster-lb; dc down -v)
(cd cluster-native; dc down -v)
(cd cluster-cm; dc down -v)

#docker network rm ksd
