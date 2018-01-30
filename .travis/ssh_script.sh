#!/bin/bash

set -e

##
## Script assumes ventas is installed and only needs to be updated,
## since the server needs to be provisioned anyway
##

cd ventas

echo "Pulling from github"
git pull

echo "Installing bower deps"
bower install

echo "Building Docker images"

lein uberjar &&
docker build -t ventas . &&
docker build -t datomic datomic &&

echo "Restarting service" &&
systemctl --user restart ventas &&

echo "Done"
