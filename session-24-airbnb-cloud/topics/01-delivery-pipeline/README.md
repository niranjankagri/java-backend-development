# Topic 1: The delivery pipeline

Session 23 ended with an image that works on one laptop. A team needs every change to be built,
tested and packaged the same way, by a machine, before anyone deploys it.

## Two workflows, two jobs

| Workflow | Question it answers | Runs |
|---|---|---|
| [`tests.yml`](../../../.github/workflows/tests.yml) (since Session 1) | does the code compile, and do all tests pass on Java 17 and 25? | every push and pull request |
| [`airbnb-image.yml`](../../../.github/workflows/airbnb-image.yml) (new) | does the **image** start, with PostgreSQL, and work from the outside? Then publish it. | changes to `airbnb-backend/**` or `pom.xml` |

## The steps of `airbnb-image.yml`

```
start app + PostgreSQL       docker compose up --build --detach   (builds the image from the Dockerfile)
smoke test                   airbnb-backend/scripts/smoke-test.sh http://localhost:8080
logs                         only if something failed: docker compose logs app
stop                         always: docker compose down --volumes
push to the registry         only on main, only if everything above passed (topic 2)
```

- The database password is new for every run (`openssl rand -hex 16`), hidden in the log
  (`::add-mask::`), and gone with the job.
- A pull request runs everything except the push: a broken image is found before it can reach `main`.
- The image that is pushed is **the one that was tested**. Building again for the push could give a
  different image (a newer base image, a changed dependency).

## The smoke test

[`scripts/smoke-test.sh`](../../../airbnb-backend/scripts/smoke-test.sh) checks a **running** service
from the outside, through its public API, with nothing but bash and curl:

1. wait until `/actuator/health/readiness` is UP (up to 3 minutes: a free cloud service may be asleep);
2. register a host and a guest, create a listing, find it in search for the dates, book it;
3. book the same nights again: must be 409 (no double booking in production either);
4. deactivate the listing again.

Every run uses new emails and a new city, so it never mixes with real data or an earlier run. It is not
a replacement for the tests: it proves that **this deployment** is wired up (database, migrations,
security, ports), in a few seconds.

A real run against the compose stack:

```
$ airbnb-backend/scripts/smoke-test.sh http://localhost:8080
Smoke test of http://localhost:8080
ok   ready
ok   201 POST /api/users
ok   201 POST /api/users
ok   201 POST /api/listings
ok   200 GET /api/listings/search?city=Smoke-1793436290-29251&checkIn=2026-12-30&checkOut=2027-01-01
ok   search found listing 1
ok   201 POST /api/bookings
ok   409 POST /api/bookings
ok   204 DELETE /api/listings/1
PASSED
```

And with the application stopped:

```
$ SMOKE_WAIT_SECONDS=10 airbnb-backend/scripts/smoke-test.sh http://localhost:8080
Smoke test of http://localhost:8080
FAILED: not ready after 10 s
$ echo $?
1
```

## Checked before the first run

The workflow runs on GitHub only once this session is merged. Before that it was checked locally:
`actionlint` (a linter for GitHub Actions workflows, including shellcheck on the `run:` scripts) found
nothing, and the same compose + smoke-test steps passed on this machine, as shown above.

## Interview questions

- **CI vs. CD?** Continuous integration: every change is built and tested automatically. Continuous
  delivery/deployment: every change that passes is packaged and ready (delivery) or released
  (deployment) automatically.
- **What is a smoke test, and how is it different from the test suite?** A few end-to-end checks of a
  running deployment, to prove it works at all; the suite tests the code's behaviour in depth.
- **Why push the tested image instead of building again?** The pushed artifact must be exactly the one
  that passed.
- **Why test a pull request's image but not push it?** Find problems before `main`; publish only what is
  merged.
