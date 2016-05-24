#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o xtrace

export BIND_ADDR="${BIND_ADDR:-$(hostname --ip-address)}"
export APP_NAME=$(echo "witan.workspace-peer" | sed s/"-"/"_"/g)
exec java ${PEER_JAVA_OPTS:-} -cp /srv/witan.workspace-peer.jar "$APP_NAME.launcher.launch_prod_peers" $NPEERS 
