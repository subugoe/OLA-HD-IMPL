#!/usr/bin/env bash
docker-compose down --remove-orphans

rm -rf docker/ola-hd_service/code
cp -r code/ docker/ola-hd_service/code

docker-compose up -d --build
