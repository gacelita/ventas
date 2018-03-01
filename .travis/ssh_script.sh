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
docker build -t joelsanchez/ventas:latest . &&
docker build -t joelsanchez/datomic:latest datomic &&
docker login --username joelsanchez --password DOCKER_PASSWORD &&
docker push joelsanchez/ventas:latest && 
docker push joelsanchez/ventas-datomic:latest &&
echo "Restarting service" &&
source /etc/rancher &&
rancher-compose --env-file .env -f docker-compose.prod.yml down ventas && 
rancher-compose --env-file .env -f docker-compose.prod.yml up -d &&
echo "Deploy done, executing REPL commands..." &&
sleep 60 &&
CONTAINER_NAME=$(docker ps --filter "label=io.rancher.stack_service.name=ventas/ventas" --format '{{.ID}}'); \
CONTAINER_IP_MASKED=$(docker inspect -f "{{ index .Config.Labels \"io.rancher.container.ip\"}}" $CONTAINER_NAME); \
CONTAINER_IP=${CONTAINER_IP_MASKED%/16}
lein repl :connect ${CONTAINER_IP}:4001 << ENDREPL
(ventas.database.seed/seed :recreate? true)
(mount.core/stop #'ventas.search/indexer)
(mount.core/start #'ventas.search/indexer)
(ventas.search/reindex)
ENDREPL
