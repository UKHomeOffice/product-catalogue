#!/usr/bin/env bash

set -e 

rm -f catalogue_import.json

python ./implode-explode/implode-explode.py -d ./catalogue/data -i

curl -v  -X PUT -H "Content-Type: application/json" --data @"catalogue_import.json" http://0.0.0.0:3000/manage/import;
rm -f catalogue_import.json
