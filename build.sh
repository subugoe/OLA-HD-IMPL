cp -r code/ docker/ola-hd_service/code

docker-compose stop
docker-compose rm -f
docker-compose up -d --build