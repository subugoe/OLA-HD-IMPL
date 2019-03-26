FROM openjdk:11-stretch

ARG app_env
ENV APP_ENV $app_env
RUN echo $app_env


RUN apt-get update \
    && apt-get install -y git curl wget vim maven --no-install-recommends


COPY ./code/ /ola-hd/code
WORKDIR /ola-hd/code

RUN mvn clean package


# see CMD in docker-compose.yml
# CMD java -jar target/long-term-storage-0.0.1-SNAPSHOT.jar
