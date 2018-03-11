#!/usr/bin/env sh

set -e 

cp resources/config.example.edn resources/config.edn

npm install karma karma-cljs-test
npm install -g karma-cli
npm install karma-chrome-launcher

lein test
lein doo chrome-headless test once
