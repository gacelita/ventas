#!/usr/bin/env sh

set -e 

npm install 
npm install -g karma-cli

lein test
lein doo chrome-headless test once
