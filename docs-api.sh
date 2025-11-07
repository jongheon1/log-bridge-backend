#!/bin/bash

API_KEY="690cbd51462bfeed5aba"
API_SECRET="d62f30ed64e7ae7bce309e529220aff4"
AUTH=$(echo -n "${API_KEY}:${API_SECRET}" | base64)

curl -X GET "https://document-api.channel.io/open/v1/spaces/\$me/articles?language=ko"