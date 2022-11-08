#!/bin/sh

#
# The IP Address will have a scaled instance
#
IP_ADDRESS=$(ifconfig eth0 | grep 'inet ' | awk '{print $2}')

#
# grab the last numerical field of the hostname and treat that as the unique ID of this scaled instance.
#
INSTANCE_ID=$(dig -x $IP_ADDRESS +short | sed -E "s/(.*)-([0-9]+)\.(.*)/\2/")

export CLIENT_ID=${CLIENT_ID_PREFIX:-}-${INSTANCE_ID}

# RUN THE COMMAND, be sure to use exec so signals are properly handled
exec $*
