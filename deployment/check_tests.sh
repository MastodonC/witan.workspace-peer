#!/usr/bin/env bash

# replace dots with hyphens in APP_NAME
APP_NAME=$1
SEBASTOPOL_IP=$2

TEST_FILE=test-output
CURL_OUT=./curl_result
TIMEOUT=2m

touch $CURL_OUT

tail -f $CURL_OUT &

STATUS=$(timeout $TIMEOUT "curl -s -o ./$CURL_OUT -X GET http://$SEBASTOPOL_IP:9501/marathon/$APP_NAME/file -H \"$SEKRIT_HEADER: 123\"")

if [ $STATUS == "200" ]
then exit 0
else exit 1
fi



