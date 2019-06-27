FROM openjdk:11-stretch

RUN apt-get update \
    && apt-get install -y maven --no-install-recommends

WORKDIR /ola-hd/code
COPY . .

RUN mvn --no-transfer-progress clean package -DskipTests