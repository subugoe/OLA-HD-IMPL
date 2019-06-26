FROM openjdk:11-stretch

RUN apt-get update \
    && apt-get install -y maven --no-install-recommends

COPY ./code/ /ola-hd/code
WORKDIR /ola-hd/code

RUN mvn clean package -DskipTests