#!/usr/bin/env bash

# replace dots with hyphens in APP_NAME
APP_NAME=witan-workspace-peer
IMAGE_PREFIX=mastondonc
SEBASTOPOL_IP=$1
ENVIRONMENT=$2
CANARY=$3

# using deployment service sebastopol
TAG=git-$(echo $CIRCLE_SHA1 | cut -c1-12)
VPC=sandpit
sed -e "s/@@TAG@@/$TAG/" -e "s/@@ENVIRONMENT@@/$ENVIRONMENT/" -e "s/@@VPC@@/$VPC/" -e "s/@@CANARY@@/$CANARY/" -e "s/@@APP_NAME@@/$APP_NAME/" -e "s/@@IMAGE_PREFIX@@/$IMAGE_PREFIX/" marathon-config.json.template > $APP_NAME$CANARY.json

# we want curl to output something we can use to indicate success/failure
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://$SEBASTOPOL_IP:9501/marathon/witan-workspace-peer$CANARY -H "Content-Type: application/json" -H "$SEKRIT_HEADER: 123" --data-binary "@$APP_NAME$CANARY.json")

echo "HTTP code " $STATUS
if [ $STATUS == "201" ]
then exit 0
else exit 1
fi
