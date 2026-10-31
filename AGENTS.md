# Instructions for AI coding tools

This file tells AI coding tools (Codex, Claude Code through `CLAUDE.md`, Copilot, Cursor and others)
how this repository works. Session 25, topic 5 explains why it exists.

## What this is

A learning course: Java backend development in 28 sessions, for beginners. Every session is a
folder `session-NN-<name>/` with a README and one README per topic in `topics/NN-<topic>/`. Sessions
with code are Maven modules (listed in the root `pom.xml`); the projects `splitwise-backend` and
`airbnb-backend` are shared by several sessions.

## Build and test

- `./mvnw verify` builds everything and runs all tests. One module: `./mvnw -pl <module> verify`.
- The compiler runs with `-Xlint:all -Werror`: every warning fails the build.
- CI runs the build on Java 17 and 25. Code must compile on Java 17.
- Tests that need Docker (Testcontainers) use `@Testcontainers(disabledWithoutDocker = true)`.

## Rules for code

- **Keep it simple for a beginner.** Prefer a plain loop and if/else. If a more advanced construct
  is used (streams, switch expressions, `Optional` chains, generics tricks), add a comment that
  explains it and shows the simpler version.
- Packages are `jbdl.sessionNN.<topic>`; each test class has a class Javadoc saying which topic it
  belongs to, and the steps of a test have short comments.
- No new libraries without a reason written in the pom. Versions live in the root `pom.xml`.
- Never commit secrets. Passwords come from environment variables; examples use obvious demo values.

## Rules for READMEs

- Every topic gets a README, also topics without code.
- Show real output (copied from a real run), never invented output. If something was not run or
  not verified, say so.
- Link to the code the README explains, with paths relative to the README.

## Working in this repository

- One session per pull request. Do not change earlier sessions in the same pull request unless it
  fixes a real bug there (say so in the pull request).
- Run `./mvnw verify` before saying a change is done.
