#!/usr/bin/env bash

APP_NAME=witan.workspace-peer
TEST_FILE=test-output
MARATHON=master.mesos
MARTATHON_PORT=8080

set -o nounset
set -o xtrace

cd /srv/$APP_NAME

curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
chmod a+x ./lein
export LEIN_ROOT=true
./lein clean
./lein deps
./lein test :integration 1> $TEST_FILE 2>&1

curl -include -XPOST http://$MARTATHON:$MARATHON_PORT/v2/artifacts/$APP_NAME/ --form file=@$TEST_FILE

exit 0

