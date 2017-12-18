#!/usr/bin/env sh

lein uberjar
docker build -t ventas .