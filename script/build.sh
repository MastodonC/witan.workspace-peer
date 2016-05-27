#!/usr/bin/env bash
set -e
lein clean
lein uberjar
docker build -t witan.workspace-peer:$(lein project-version) .
