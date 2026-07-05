FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM docker:27-cli AS docker-cli

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV TZ=Asia/Shanghai \
    SERVER_PORT=25565 \
    PYTHON_HOST=bilibili-api \
    PYTHON_PORT=9000 \
    PYTHON_DOCKER_SERVICE=bilibili-api \
    JAVA_OPTS=""

COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=build /workspace/target/btncm-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 25565

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
