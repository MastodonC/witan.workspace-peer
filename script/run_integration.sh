#!/usr/bin/env bash

set -o nounset
set -o xtrace

cd /srv/witan.workspace-peer

curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
chmod a+x ./lein
export LEIN_ROOT=true
./lein clean
./lein jar
./lein test :integration 1> test-output 2>&1

exit 0

