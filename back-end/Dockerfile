# Build the project
FROM openjdk:11-stretch AS build
RUN apt-get update \
    && apt-get install -y maven --no-install-recommends
WORKDIR /build
COPY . .
RUN mvn -q clean package -DskipTests

# Production image
FROM openjdk:13-jdk-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /ola-hd
COPY --from=build /build/target/long-term-storage*.jar app.jar
ENV OLA_HD_MIN_MEMORY 6G
ENV OLA_HD_MAX_MEMORY 6G
ENV OLA_HD_PORT 8080
EXPOSE ${OLA_HD_PORT}
CMD java -Xms${OLA_HD_MIN_MEMORY} -Xmx${OLA_HD_MAX_MEMORY} -jar -Dserver.port=${OLA_HD_PORT} app.jar