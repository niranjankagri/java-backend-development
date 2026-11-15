# Topic 4: The container image

[`Dockerfile`](../../../airbnb-backend/Dockerfile), built from the repository root (the module needs the
parent pom and the Maven wrapper):

```bash
docker build -f airbnb-backend/Dockerfile -t airbnb-backend .
```

## Two stages

1. **build** (`eclipse-temurin:21-jdk`): the JDK and Maven build the jar. Everything in this stage is
   thrown away afterwards.
2. **run** (`eclipse-temurin:21-jre`): only a Java runtime and the application. No compiler, no Maven,
   no source code: a smaller image with less for an attacker to use.

The repository's [`.dockerignore`](../../../.dockerignore) keeps `target/` and `.git` out of the build
context, so a local build never leaks into the image.

## Layers: rebuild only what changed

A Spring Boot jar can be split into layers that change at different speeds:

```dockerfile
RUN java -Djarmode=tools -jar airbnb-backend/target/airbnb-backend-1.0.0-SNAPSHOT.jar \
        extract --layers --launcher --destination /extracted
...
COPY --from=build /extracted/dependencies/ ./           # the libraries: change rarely
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./            # our classes: change with every build
```

Each `COPY` is one image layer, and Docker reuses unchanged layers. The real sizes
(`docker history airbnb-backend`):

```
365kB   COPY /extracted/application/ ./
4.1kB   COPY /extracted/snapshot-dependencies/ ./
696kB   COPY /extracted/spring-boot-loader/ ./
62.6MB  COPY /extracted/dependencies/ ./
```

A code change rebuilds and uploads 365 kB instead of a 63 MB fat jar.

## Safe and container-aware defaults

| Line | Why |
|---|---|
| `RUN useradd --system --uid 10001 airbnb` and `USER airbnb` | never run as root inside the container |
| `ENV SPRING_PROFILES_ACTIVE=prod` | the image always starts with production settings (topic 1) |
| `-XX:MaxRAMPercentage=75` | the heap is sized from the **container's** memory limit, leaving room for the rest of the JVM |
| `HEALTHCHECK ... curl -fsS http://localhost:8080/actuator/health/readiness` | Docker marks the container healthy or unhealthy (topic 5) |
| `ENTRYPOINT [..., "org.springframework.boot.loader.launch.JarLauncher"]` | starts the extracted layers |

The whole image is 577 MB, most of it the Ubuntu-based Java runtime. Smaller bases exist (Alpine, a
distroless image, or a jlink'ed runtime) at the cost of fewer tools for debugging.

## Interview questions

- **Why a multi-stage build?** Build tools stay in a throwaway stage; the final image has only what runs.
- **What do layered jars buy you?** Faster builds and pushes: only the changed layer is rebuilt and
  uploaded.
- **Why not run as root?** If the application is broken into, the attacker is a user without rights
  inside the container, not root.
- **How does the JVM know the container's memory?** It reads the cgroup limit (container support is on by
  default); `MaxRAMPercentage` sets the heap as a share of it.
