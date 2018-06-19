#!/bin/bash

set -e

##
## Script assumes ventas is installed and only needs to be updated,
## since the server needs to be provisioned anyway
##

cd /opt/ventas

echo "Pulling from github"
git pull --rebase --prune

echo "Installing bower deps"
bower install

echo "Building Docker images"

source /etc/docker.env
docker login -u $TREESCALE_USER -p $TREESCALE_PASSWORD repo.treescale.com

lein clean &&
lein uberjar &&
docker build -t ventas . &&
docker build -t ventas-datomic datomic &&
docker tag ventas repo.treescale.com/joelsanchez/ventas &&
docker tag ventas-datomic repo.treescale.com/joelsanchez/ventas-datomic &&
docker push repo.treescale.com/joelsanchez/ventas &&
docker push repo.treescale.com/joelsanchez/ventas-datomic &&
echo "Restarting service" &&
rancher-compose --env-file .env -f docker-compose.prod.yml down && 
rancher-compose --env-file .env -f docker-compose.prod.yml rm && 
rancher-compose --env-file .env -f docker-compose.prod.yml up -d