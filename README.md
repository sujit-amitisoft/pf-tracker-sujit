# Personal Finance Tracker

Full-stack personal finance tracker built with React, Spring Boot, and PostgreSQL.

## Structure

- `backend` - Spring Boot API, Flyway migrations, scheduling, reporting, and domain services
- `frontend` - React app shell and feature modules
- `docs` - project plans and supporting documentation

## Current State

This repository includes the initial implementation foundation:

- Spring Boot backend scaffold with domain model, migrations, services, controllers, security skeleton, and dashboard/report aggregation endpoints
- React frontend scaffold with routed app shell, feature pages, theming, and notification plumbing
- Project plan derived from the provided product specification

## Local Development

### Backend

1. Configure PostgreSQL and environment variables as needed.
2. Run from `backend`:

```bash
mvn spring-boot:run
```

### Podman

1. Use `.env`.
2. Start the full stack:

```bash
podman compose up --build
```

The repository also keeps the original Podman-specific file at `podman-compose.yml`, but `compose.yaml` is included so compose providers that only auto-detect standard filenames work without extra flags.

Services exposed locally:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres: `localhost:5432`

The frontend is built into a static container and served through Nginx. `VITE_API_BASE_URL` is injected at build time, so if the browser should call a different backend origin you should change it in `.env` before rebuilding.

### Frontend

1. Install dependencies in `frontend`.
2. Start the dev server:

```bash
npm install
npm run dev
```

## Environment

Backend defaults live in `backend/src/main/resources/application.yml`.

Recommended overrides:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRATION`
- `JWT_REFRESH_EXPIRATION`
- `APP_ALLOWED_ORIGINS`
- `VITE_API_BASE_URL`

## Notes

- CSV export is implemented in V1 scope.
- PDF export, banking integrations, AI advice, and bulk transaction operations remain out of scope.
- Podman now supports `frontend + backend + postgres` for local deployment.
