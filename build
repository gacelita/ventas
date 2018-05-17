#!/usr/bin/env sh

bower install
lein uberjar
docker build -t ventas .
docker build -t datomic datomic
