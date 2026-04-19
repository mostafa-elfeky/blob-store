# Blob Store Technical Guide

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Thymeleaf
- PostgreSQL
- Gradle

## Configuration

Default application settings live in [src/main/resources/application.yml](/Users/mostafa/IdeaProjects/blob-store/src/main/resources/application.yml:1).

Key defaults:

- Server port: `4817`
- Database: configured after startup from the admin dashboard
- Schema: `blob_store`
- Storage root: `./data/blob-store`
- Multipart upload limit: `250MB`

## Testing

Run the test suite with:

```bash
./gradlew test
```

Tests use H2 in-memory database configuration and write temporary files under `build/test-storage`.

## API Notes

API details are available from the admin UI. The platform exposes module and file endpoints for uploads, retrieval, and metadata access.

## Project Structure

- [src/main/java/com/baseta/blobstore/module](/Users/mostafa/IdeaProjects/blob-store/src/main/java/com/baseta/blobstore/module:1): module configuration and validation
- [src/main/java/com/baseta/blobstore/file](/Users/mostafa/IdeaProjects/blob-store/src/main/java/com/baseta/blobstore/file:1): file storage, retrieval, and metadata
- [src/main/java/com/baseta/blobstore/web](/Users/mostafa/IdeaProjects/blob-store/src/main/java/com/baseta/blobstore/web:1): REST controllers, admin controller, and exception handling
- [src/main/java/com/baseta/blobstore/storage](/Users/mostafa/IdeaProjects/blob-store/src/main/java/com/baseta/blobstore/storage:1): storage path configuration

## Notes

- Image size definitions use the format `code=WIDTHxHEIGHT`
- Video modules require `videoType`
- Stored files are written to the configured storage root under the module folder
