#!/usr/bin/env sh

lein uberjar
docker build -t ventas .
docker build -t datomic datomic
