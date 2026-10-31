# Topic 3: Cloud deployment on Render

[Render](https://render.com) runs containers and managed PostgreSQL databases, gives every service an
HTTPS address, and can be described as code: a **blueprint**,
[`render.yaml`](../../../render.yaml) in the repository root. (The same ideas apply to any platform:
AWS App Runner, Google Cloud Run, Azure Container Apps, Fly.io, or Kubernetes.)

## What the blueprint creates

```yaml
databases:
  - name: airbnb-db
    plan: free
    region: frankfurt
    databaseName: airbnb
    user: airbnb
    ipAllowList: []                 # no access from the internet: only Render services can connect

services:
  - type: web
    name: airbnb-backend
    runtime: docker
    dockerContext: .
    dockerfilePath: ./airbnb-backend/Dockerfile
    buildFilter:
      paths: ["airbnb-backend/**", "pom.xml"]
    autoDeployTrigger: checksPass
    healthCheckPath: /actuator/health/readiness
    envVars:
      - key: DATABASE_HOST
        fromDatabase: { name: airbnb-db, property: host }
      ...                           # DATABASE_PORT, DATABASE_NAME, DATABASE_USER, DATABASE_PASSWORD
```

| Line | Meaning |
|---|---|
| `ipAllowList: []` | the database accepts no connections from the internet, only from Render's own network |
| `runtime: docker` + `dockerfilePath` | Render builds the image of Session 23 from the repository |
| `buildFilter` | a commit that changes only another session does not redeploy the app |
| `autoDeployTrigger: checksPass` | a commit of `main` is deployed only after **all** its GitHub checks passed: the tests on Java 17 and 25 and the image pipeline (topic 1) |
| `healthCheckPath` | a new version gets traffic only once its readiness endpoint answers UP (topic 4) |
| `fromDatabase` | Render passes the database's own host, port, name, user and password to the service; nobody types or sees the password |

## The application meets the platform

Render gives the database as parts (host, port, name) rather than as a JDBC URL, and tells the service
which port to listen on (`PORT`).
[`application-prod.properties`](../../../airbnb-backend/src/main/resources/application-prod.properties)
accepts both styles:

```properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://${DATABASE_HOST:localhost}:${DATABASE_PORT:5432}/${DATABASE_NAME:airbnb}}
server.port=${PORT:8080}
```

A `DATABASE_URL` wins (compose, Session 23); without it, the URL is built from the parts (Render).
[`CloudSettingsTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/CloudSettingsTest.java) starts the
`prod` profile against PostgreSQL 18 with only the five Render-style settings and checks the URL that
results and that the database is UP.

## HTTPS

Render ends HTTPS at its edge with a certificate it renews itself, and forwards plain HTTP to the
container. That matters here: HTTP Basic sends the password with every request (Session 22), so the
API must only ever be used over `https://`.

## Free plan limits

- The web service sleeps after 15 minutes without requests; the next request waits about a minute while
  it starts (the smoke test waits up to 3 minutes for that reason).
- A free PostgreSQL is deleted after 30 days unless it is upgraded.
- 512 MB of memory: the JVM's heap is 75 % of it (`MaxRAMPercentage`, Session 23).

Good for a demonstration, not for real users.

## How to deploy (once this session is on `main`)

1. render.com: sign in with GitHub, **New > Blueprint**, choose this repository, approve. Render creates
   the database and the service, builds the image, runs the Flyway migrations at the first start.
2. Smoke test the result (topic 4):
   `airbnb-backend/scripts/smoke-test.sh https://<service-name>.onrender.com`
3. From then on every green commit on `main` that touches the app deploys by itself.

**Status:** the blueprint and the Render-style configuration are verified locally (the settings test and
the image); a live deployment needs this session on `main` and the steps above, so there is no public
address yet.

## Interview questions

- **What is infrastructure as code, and what does it buy you?** The environment described in a reviewed
  file (here `render.yaml`): repeatable, versioned, and the same for everyone.
- **Why should the database not be reachable from the internet?** Only the application needs it; every
  open port is attack surface.
- **How does the app get the database password without anyone handling it?** The platform injects it
  from the database it created (`fromDatabase`) as an environment variable.
- **Where does HTTPS end, and is that safe?** At the platform's edge; the hop to the container is inside
  the platform's network. Where that is not acceptable, the container serves TLS itself.
