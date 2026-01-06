ARG VERSION=1.0.0

FROM maven:3.9-eclipse-temurin-21 AS builder

ARG VERSION

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

ARG VERSION

WORKDIR /app

RUN apt-get update && \
    apt-get install -y gosu && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /app/config /app/cache /app/media

RUN groupadd -r java && useradd -r -g java java

COPY --from=builder /build/target/xtream2jellyfin-${VERSION}-jar-with-dependencies.jar /app/xtream2jellyfin.jar
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh

RUN chmod +x /usr/local/bin/entrypoint.sh

VOLUME ["/app/config", "/app/cache", "/app/media"]

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

CMD ["java", "-jar", "/app/xtream2jellyfin.jar"]
