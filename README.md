# Blob Store

`Blob Store` is an open-source platform for organizing, storing, and serving business files and media through structured upload modules. It helps teams manage documents, images, and videos with clear module-based rules, admin controls, and reliable access patterns.

## Features

- Create upload modules with a normalized unique code
- Support image and video module types
- Validate file type and file size per module
- Generate resized image variants such as `thumb` or `medium`
- Download files by UUID or retrieval name
- Browse modules, recent uploads, and API details from the admin dashboard

## Technical Details

Technical setup, tech stack, local run steps, testing notes, project structure, and API guidance live in [TECHNICAL.md](/Users/mostafa/IdeaProjects/blob-store/TECHNICAL.md:1).

## Running Locally

1. Start the application:

```bash
./gradlew bootRun
```

2. Open `http://localhost:4817`.
3. Configure the target database connection from the admin dashboard.
4. Ensure the configured database user has access to the target schema if your database vendor uses schemas.

## Running In Docker

Build the image:

```bash
docker build -t blob-store .
```

Run it directly:

```bash
docker run --rm -p 4817:4817 \
  -e BLOBSTORE_ADMIN_USERNAME=admin \
  -e BLOBSTORE_ADMIN_PASSWORD=admin123 \
  -e BLOBSTORE_DB_VENDOR=POSTGRESQL \
  -e BLOBSTORE_DB_HOST=host.docker.internal \
  -e BLOBSTORE_DB_PORT=5432 \
  -e BLOBSTORE_DB_NAME=blobstore \
  -e BLOBSTORE_DB_SCHEMA=blob_store \
  -e BLOBSTORE_DB_USERNAME=blobstore \
  -e BLOBSTORE_DB_PASSWORD=blobstore \
  -e BLOBSTORE_API_BASIC_AUTH_ENABLED=true \
  -e BLOBSTORE_API_BASIC_USERNAME=integration \
  -e BLOBSTORE_API_BASIC_PASSWORD=integration123 \
  -v blob-store-data:/var/lib/blob-store \
  blob-store
```

To embed it into another project, copy [docker/docker-compose.example.yml](/Users/mostafa/IdeaProjects/blob-store/docker/docker-compose.example.yml:1) or lift just the `blob-store` service into your own `docker-compose.yml`. The container can bootstrap its admin login, database connection, storage path, JWT mode, and API basic-auth settings from environment variables, so you do not need to open the dashboard just to make the service start correctly.

Relevant environment variables:

- `BLOBSTORE_ADMIN_USERNAME`, `BLOBSTORE_ADMIN_PASSWORD`
- `BLOBSTORE_DB_VENDOR`, `BLOBSTORE_DB_HOST`, `BLOBSTORE_DB_PORT`, `BLOBSTORE_DB_NAME`, `BLOBSTORE_DB_SCHEMA`, `BLOBSTORE_DB_USERNAME`, `BLOBSTORE_DB_PASSWORD`
- `BLOBSTORE_STORAGE_ROOT_DIR`
- `BLOBSTORE_API_JWT_VALIDATION_MODE`, `BLOBSTORE_API_JWT_SHARED_SECRET`, `BLOBSTORE_API_JWT_JWK_SET_URI`, `BLOBSTORE_API_JWT_ISSUER`, `BLOBSTORE_API_JWT_AUDIENCE`
- `BLOBSTORE_API_BASIC_AUTH_ENABLED`, `BLOBSTORE_API_BASIC_USERNAME`, `BLOBSTORE_API_BASIC_PASSWORD`
- `JAVA_OPTS`

## Publish To Docker Hub

The repo includes [docker-publish.yml](/Users/mostafa/IdeaProjects/blob-store/.github/workflows/docker-publish.yml:1), which publishes the image to Docker Hub when you push a Git tag that starts with `v`, for example `v0.1.0`.

Set these GitHub repository secrets first:

- `DOCKERHUB_USERNAME`: your Docker Hub username or organization
- `DOCKERHUB_TOKEN`: a Docker Hub access token with permission to push images

Then release a version with:

```bash
git tag v0.1.0
git push origin v0.1.0
```

That workflow will publish:

- `YOUR_DOCKERHUB_USERNAME/blob-store:v0.1.0`

If the tag is pushed from the default branch, it will also publish:

- `YOUR_DOCKERHUB_USERNAME/blob-store:latest`

## Admin Experience

The admin UI is the main entry point for configuring the platform, managing modules, reviewing uploads, checking system logs, and viewing the available API details.

Name suggestion for `Blob Store` came from Ahmed Lotfy.

This is an open-source project released under the MIT License.
