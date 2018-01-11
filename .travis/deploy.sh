#!/bin/bash
set -e
ssh travis@kazer.es <<'ENDSSH'
cd ventas
echo "Pulling from github"
git pull
echo "Building Docker images"
bower install
lein uberjar
docker build -t ventas .
docker build -t datomic datomic
echo "Restarting service"
systemctl --user restart ventas
echo "Done"
ENDSSH