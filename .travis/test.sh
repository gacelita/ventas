#!/usr/bin/env sh

set -e 

npm install 
npm install -g karma-cli

lein test
lein compile-cljs-tests
karma start --single-run
