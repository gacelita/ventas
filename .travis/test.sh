#!/usr/bin/env sh

cp resources/config.example.edn resources/config.edn
npm install karma-chrome-launcher

lein test
lein doo chrome-headless test
