# Session 23: the Airbnb backend as a container image, built in two stages.
# Build from the REPOSITORY ROOT (the module needs the parent pom and the Maven wrapper):
#   docker build -f airbnb-backend/Dockerfile -t airbnb-backend .

# ---- stage 1: build the jar with the full JDK, and split it into layers ----
FROM eclipse-temurin:21-jdk AS build
# The Maven wrapper needs `unzip` to check the downloaded Maven against its pinned checksum
# (found while building the Splitwise image, Session 17).
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN ./mvnw -B -q -pl airbnb-backend -am package -DskipTests
# Spring Boot can split its jar into layers that change at different speeds: the libraries (rarely),
# then our own classes (every build). Each layer becomes one image layer below, so a code change
# rebuilds and uploads only the small application layer, not the 70 MB of libraries again.
RUN java -Djarmode=tools -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar \
        extract --layers --launcher --destination /extracted

# ---- stage 2: only a Java runtime and the layers ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# never run as root inside the container
RUN useradd --system --uid 10001 airbnb
# least often changed first, so Docker can reuse the earlier layers from its cache
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER airbnb
EXPOSE 8080
# the prod profile reads the database settings from environment variables (application-prod.properties)
ENV SPRING_PROFILES_ACTIVE=prod
# Docker asks the readiness endpoint every 10 s; after 3 failed answers the container is "unhealthy".
# start-period: no verdict during the first 60 s while Spring Boot starts. (curl is in the temurin image.)
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1
# MaxRAMPercentage: the JVM sizes its heap from the CONTAINER's memory limit, leaving 25 % for the rest
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
