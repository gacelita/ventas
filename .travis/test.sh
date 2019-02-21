#!/usr/bin/env sh

set -e 

npm install 
npm install -g karma-cli

lein test
shadow-cljs compile :admin-test
karma start --single-run
