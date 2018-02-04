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

lein uberjar &&
docker build -t ventas . &&
docker build -t datomic datomic &&
echo "Restarting service" &&
docker-compose down &&
docker-compose up -d &&
echo "Deploy done, executing REPL commands..." &&
until [ "`docker inspect -f {{.State.Running}} ventas_ventas_1`"=="true" ]; do
    sleep 1;
done &&
CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ventas_ventas_1); \
lein repl :connect $CONTAINER_IP:4001 << ENDREPL
(ventas.database.seed/seed :recreate? true)
(mount.core/stop #'ventas.search/indexer)
(mount.core/start #'ventas.search/indexer)
(ventas.search/reindex)
ENDREPL