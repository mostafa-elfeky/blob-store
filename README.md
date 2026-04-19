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

## Admin Experience

The admin UI is the main entry point for configuring the platform, managing modules, reviewing uploads, checking system logs, and viewing the available API details.

Name suggestion for `Blob Store` came from Ahmed Lotfy.

This is an open-source project released under the MIT License.
