#!/bin/bash

set -e
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
