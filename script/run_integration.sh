#!/usr/bin/env bash

APP_NAME=witan.workspace-peer
TEST_FILE=test-output
INTEGRATION_TEST_PORT=6358

set -o nounset
set -o xtrace

cd /srv/$APP_NAME

curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
chmod a+x ./lein
export LEIN_ROOT=true
touch $TEST_FILE
./lein clean
./lein deps


./lein test :integration 1> $TEST_FILE 2>&1

{ echo -ne "HTTP/1.0 200 OK\r\n\r\n"; cat $TEST_FILE; } | nc -l -p $INTEGRATION_TEST_PORT 
#Potential to lose the last bit of the file, if so introduce a wait or have a better idea

exit 0

