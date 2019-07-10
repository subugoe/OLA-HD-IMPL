#!/usr/bin/env bash

docker-compose down --remove-orphans
docker-compose up -d --build

