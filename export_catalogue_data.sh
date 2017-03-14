#!/usr/bin/env bash

set -ex 

rm -rf ./catalogue/data
mkdir ./catalogue/data

curl http://0.0.0.0:3000/manage/export > ./catalogue/data/export.json

python ./implode-explode/implode-explode.py -d ./catalogue/data -f ./catalogue/data/export.json -e

rm -f ./catalogue/data/export.json
