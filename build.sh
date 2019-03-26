#docker-compose stop
#docker-compose rm -f
docker-compose down

rm -rf docker/ola-hd_service/code
cp -r code/ docker/ola-hd_service/code

docker-compose up -d --build
