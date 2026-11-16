# Topic 2: The container registry and image versions

A registry stores images so that servers can pull them: Docker Hub, the cloud providers' registries, or
**GitHub Container Registry** (`ghcr.io`), which belongs to the repository and needs no extra account.

## Pushing from the pipeline

From [`airbnb-image.yml`](../../../.github/workflows/airbnb-image.yml), on `main` only:

```yaml
permissions:
  packages: write          # push to ghcr.io with the job's own GITHUB_TOKEN (no extra secret)
...
- name: Push the tested image
  run: |
    TAG="sha-${GITHUB_SHA::7}"
    docker tag airbnb-backend:local "$IMAGE:$TAG"
    docker tag airbnb-backend:local "$IMAGE:latest"
    docker push "$IMAGE:$TAG"
    docker push "$IMAGE:latest"
```

`$IMAGE` is `ghcr.io/niranjankagri/airbnb-backend`. The job logs in with the token GitHub gives every
run; `packages: write` allows that token, and only that job, to push. No password is stored anywhere.

[`compose.yaml`](../../../airbnb-backend/compose.yaml) names the image it builds (`airbnb-backend:local`),
so the workflow knows exactly which image passed the smoke test.

## Tags: one that never moves, one that does

| Tag | Points to | Use |
|---|---|---|
| `sha-1a2b3c4` | exactly one commit, forever | deploy, roll back, find out what runs ("which commit is in production?") |
| `latest` | whatever was pushed last | convenience for trying it out; never for production |

A tag is just a name: `latest` today is not `latest` tomorrow. Deployments and rollbacks use the commit
tag, so "go back to yesterday" means "run `sha-<yesterday>`" and nothing else.

## The image says where it comes from

[`Dockerfile`](../../../airbnb-backend/Dockerfile):

```dockerfile
LABEL org.opencontainers.image.source="https://github.com/niranjankagri/java-backend-development" \
      org.opencontainers.image.description="Airbnb-like rental backend (course Sessions 21 to 24)"
```

`org.opencontainers.image.source` is a standard label: GitHub links the package to the repository
(its page, its permissions), and anyone holding the image can find its source.

## Status

The first push happens with the first run on `main` after this session is merged. A new package on
GHCR is private by default; it can be made public in the package settings on GitHub.

## Interview questions

- **Why tag images with the commit?** An immutable, traceable version: what runs is exactly that commit,
  and a rollback is choosing an older tag.
- **What is wrong with deploying `latest`?** It changes under you; two servers can run different code
  under the same name, and a rollback has nothing to point to.
- **How does the pipeline log in to the registry without a stored secret?** The workflow's own
  short-lived `GITHUB_TOKEN`, given `packages: write` for that job only.
