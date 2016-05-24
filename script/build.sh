#!/bin/bash
set -e
lein clean
lein uberjar
docker build -t witan.workspace-peer:0.1.0 . 
