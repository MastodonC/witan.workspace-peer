#!/usr/bin/env bash
docker run -p 10015:10015 -e "ONYX_ID=1" -e "NPEERS=1" --link "zookeeper:zk" --privileged -it mastodonc/witan.workspace-peer:$(lein project-version)
