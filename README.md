# Personal Finance Tracker

Full-stack personal finance tracker built with React, Spring Boot, and PostgreSQL.

## Structure

- `backend` - Spring Boot API, Flyway migrations, JWT auth, scheduling, reporting, and domain services
- `frontend` - Vite React app with routed pages, auth flow, dashboard, and finance modules
- `infra` - container and infrastructure-related files
- `docs` - supporting project and API documentation

## Features

- JWT-based authentication with register, login, refresh, forgot password, and reset password flows
- Accounts, transactions, budgets, goals, recurring transactions, shared accounts, insights, reports, categories, and settings
- Swagger UI for backend API exploration
- Seeded demo data for local and fresh-environment testing
- Podman-based full-stack local development
- Azure App Service backend deployment
- Azure Static Web Apps frontend deployment

## Local Development

### Prerequisites

- Java 17
- Maven 3.9+
- Node.js 22+
- npm 10+
- Podman

### Environment Setup

Copy the repo example file and adjust values as needed:

```bash
copy .env.example .env
```

Recommended local defaults:

- `DB_NAME=finance_tracker`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`
- `JWT_SECRET=change-me-change-me-change-me-change-me`
- `APP_ALLOWED_ORIGINS=http://localhost:5173`
- `VITE_API_BASE_URL=http://localhost:8080`
- `APP_FRONTEND_URL=http://localhost:5173`

### Run Full Stack With Podman

From the repository root:

```bash
podman compose up --build
```

Local services:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres: `localhost:5432`

Useful commands:

```bash
podman compose down
podman compose down -v
podman compose logs -f
```

Use `podman compose down -v` when you want a clean database reset or Flyway history reset.

### Run Backend Only

```bash
cd backend
mvn spring-boot:run
```

### Run Frontend Only

```bash
cd frontend
npm install
npm run dev
```

### Local Verification

- frontend app: `http://localhost:5173`
- backend Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- backend OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Seeded Login

For fresh environments seeded by the backend demo seeder:

- email: `admin@amiti.local`
- password: `Password1`

## Azure Deployment

### Backend

The backend is deployed through GitHub Actions using:

- [master_pfinance-tracker-source.yml](/d:/AmitiWorkspace/.github/workflows/master_pfinance-tracker-source.yml)

This workflow builds the Spring Boot app from `backend/` and deploys the generated JAR to Azure App Service.

Important backend app settings in Azure App Service:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_ALLOWED_ORIGINS`
- `APP_FRONTEND_URL`
- optional SMTP settings for password reset and invite emails

Swagger endpoints in Azure:

- `/swagger-ui/index.html`
- `/v3/api-docs`

### Frontend

The frontend is deployed through GitHub Actions using:

- [azure-static-web-apps-nice-flower-04c5e7500.yml](/d:/AmitiWorkspace/.github/workflows/azure-static-web-apps-nice-flower-04c5e7500.yml)

Important GitHub Actions configuration for the frontend:

- repository variable: `VITE_API_BASE_URL`
- repository secret: `AZURE_STATIC_WEB_APPS_API_TOKEN_NICE_FLOWER_04C5E7500`

`VITE_API_BASE_URL` must point to the Azure backend App Service URL, not `localhost`.

### Azure Static Web Apps Routing

The frontend uses React Router with `BrowserRouter`, so direct hits and refreshes on client-side routes require Azure Static Web Apps SPA fallback configuration.

This is handled by:

- [staticwebapp.config.json](/d:/AmitiWorkspace/frontend/staticwebapp.config.json)

This enables routes such as:

- `/auth`
- `/transactions`
- `/budgets`
- `/goals`
- `/reports`
- `/settings`

without Azure returning a static 404 page on refresh or direct navigation.

## Notes

- The backend Maven project is rooted in `backend/`, not the repo root.
- The frontend falls back to `http://localhost:8080` only when `VITE_API_BASE_URL` is missing at build time.
- Azure frontend and backend must be aligned:
  - frontend built with the real backend URL
  - backend `APP_ALLOWED_ORIGINS` set to the Static Web Apps URL
- On fresh Azure databases, Flyway runs the schema migrations automatically.
