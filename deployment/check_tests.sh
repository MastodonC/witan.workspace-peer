#!/usr/bin/env bash

# replace dots with hyphens in APP_NAME
IMAGE_PREFIX=mastondonc
APP_NAME=$1
SEBASTOPOL_IP=$3

TEST_FILE=test-output
CURL_OUT=./curl_result
TIMEOUT=2m

STATUS=$(timeout $TIMEOUT "curl -s -o ./$CURL_OUT -X GET http://$SEBASTOPOL_IP:9501/marathon/$APP_NAME/$APP_VERSION/files/$TEST_FILE -H \"$SEKRIT_HEADER: 123\"")

STATUS=$(timeout $TIMEOUT "curl -s -o ./$CURL_OUT -X DELETE http://$SEBASTOPOL_IP:9501/marathon/$APP_NAME/files/$TEST_FILE -H \"$SEKRIT_HEADER: 123\"")

echo $CURL_OUT

if [ $STATUS == "200" ]
then exit 0
else exit 1
fi



