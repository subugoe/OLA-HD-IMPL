#!/usr/bin/env bash

docker-compose build packager 

docker-compose stop

#docker-compose up -d --remove-orphans packager
docker-compose up -d packager
