#!/bin/bash
#
# Jolokia JMX Query Script
# Usage:
#   ./jolokia-jmx.sh                    - List all MBeans
#   ./jolokia-jmx.sh <bean>             - List attributes for a bean
#   ./jolokia-jmx.sh <bean> <attribute> - Get attribute value
#
# Examples:
#   ./jolokia-jmx.sh
#   ./jolokia-jmx.sh "kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec"
#   ./jolokia-jmx.sh "kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec" Count
#

set -euo pipefail

#JOLOKIA_URL="${JOLOKIA_URL:-http://localhost:17072/jolokia}"
JOLOKIA_URL="${JOLOKIA_URL:-http://localhost:7072/jolokia}"

# Check if jq is available
if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed. Install with: brew install jq" >&2
    exit 1
fi

# Function to list all MBeans
list_beans() {
    echo "Fetching MBean list from $JOLOKIA_URL/list..."
    curl -s "$JOLOKIA_URL/list" | jq -r '
        .value
        | to_entries[]
        | .key as $domain
        | .value
        | to_entries[]
        | "\($domain):\(.key)"
    ' | sort
}

# Function to list attributes for a bean
list_attributes() {
    local bean="$1"
    echo "Fetching attributes for: $bean"
    curl -s "$JOLOKIA_URL/list" | jq -r --arg bean "$bean" '
        .value
        | to_entries[]
        | .key as $domain
        | .value
        | to_entries[]
        | select(($domain + ":" + .key) == $bean)
        | .value.attr
        | to_entries[]
        | "\(.key) (\(.value.type // "unknown"))"
    ' | sort
}

# Function to get attribute value
get_attribute() {
    local bean="$1"
    local attribute="$2"
    echo "Fetching $bean / $attribute"
    curl -s "$JOLOKIA_URL/read/$bean/$attribute" | jq '.value'
}

# Main logic
case "${1:-}" in
    "")
        # No arguments - list all beans
        list_beans
        ;;
    *)
        if [ $# -eq 1 ]; then
            # One argument - list attributes for bean
            list_attributes "$1"
        elif [ $# -eq 2 ]; then
            # Two arguments - get attribute value
            get_attribute "$1" "$2"
        else
            echo "Error: Too many arguments" >&2
            echo "Usage: $0 [bean] [attribute]" >&2
            exit 1
        fi
        ;;
esac
