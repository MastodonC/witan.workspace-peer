#!/usr/bin/env bash
docker run -p 8080:8080 -e "ONYX_ID=1" -e "NPEERS=1" --link "zookeeper:zk" --privileged -it witan.workspace-peer:$(lein project-version)
