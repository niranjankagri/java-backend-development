# Session 24: Deployment of the Airbnb Backend Application (Continued)

From a container on one machine (Session 23) to a release process for the internet: a pipeline that
builds, starts and smoke-tests the image and publishes it; a cloud deployment described as code with a
managed PostgreSQL and HTTPS; releases without downtime and with a way back; logs a machine can search.

| # | Topic | README |
|---|---|---|
| 1 | The delivery pipeline: build, start, smoke-test | [01-delivery-pipeline](topics/01-delivery-pipeline/README.md) |
| 2 | The container registry and image versions | [02-container-registry](topics/02-container-registry/README.md) |
| 3 | Cloud deployment on Render | [03-cloud-deployment-on-render](topics/03-cloud-deployment-on-render/README.md) |
| 4 | Releasing safely: no downtime, a check after every release, a way back | [04-releasing-safely](topics/04-releasing-safely/README.md) |
| 5 | Logs in production | [05-production-logs](topics/05-production-logs/README.md) |

## What works after this session

- [`airbnb-image.yml`](../.github/workflows/airbnb-image.yml): for every change to the app, the image is
  built, started with PostgreSQL and smoke-tested; on `main` the tested image is pushed to
  `ghcr.io/niranjankagri/airbnb-backend` as `sha-<commit>` and `latest`.
- [`scripts/smoke-test.sh`](../airbnb-backend/scripts/smoke-test.sh): checks any running deployment
  through its API (ready, register, list, search, book, no double booking).
- [`render.yaml`](../render.yaml): a managed PostgreSQL (not reachable from the internet) and the web
  service, deployed after all checks passed, switched over by readiness.
- The `prod` profile takes the database as a URL or as host/port/name, listens on `PORT`, and logs JSON.
- 51 tests in the module (new: [`CloudSettingsTest`](../airbnb-backend/src/test/java/jbdl/airbnb/CloudSettingsTest.java)).

**Verified locally:** the workflows with actionlint, the pipeline's compose + smoke-test steps, the JSON
logs, and the Render-style configuration against PostgreSQL 18. **Not yet:** the first pipeline run on
GitHub, the first image on GHCR, and the live Render deployment; they need this session on `main`
(topic 3 lists the steps).

## What you can do after this session

- Build a pipeline that tests the deployable artifact itself and publishes exactly what was tested.
- Version images by commit and explain why `latest` is not a version.
- Describe a cloud environment as code, connect an app to a managed database without handling its
  password, and know what free plans leave out.
- Explain a zero-downtime release, backwards-compatible migrations, smoke tests and rollbacks.
- Switch an application to structured logs and say what must never be logged.

## Run it

```bash
cd airbnb-backend
DATABASE_PASSWORD=choose-one docker compose up --build --detach
scripts/smoke-test.sh http://localhost:8080
docker compose logs app            # JSON lines (prod profile)
docker compose down --volumes
```
