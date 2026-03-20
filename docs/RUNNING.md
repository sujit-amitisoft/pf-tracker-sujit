# Running the Application

This project can be started in two ways:

- full stack with Podman
- frontend locally with backend and database in Podman

## 1. Prerequisites

Make sure these are installed:

- Podman
- Java 17
- Maven 3.9+
- Node.js 22+
- npm 10+

If you want to use Podman on Windows, make sure the Podman machine is initialized and running.

Example:

```bash
podman machine init
podman machine start
```

## 2. Environment Setup

Copy the example environment file at the repo root:

```bash
copy .env.example .env
```

Recommended default values:

- `DB_NAME=finance_tracker`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`
- `JWT_SECRET=change-me-change-me-change-me-change-me`
- `APP_ALLOWED_ORIGINS=http://localhost:5173`
- `VITE_API_BASE_URL=http://localhost:8080`

## 3. Run Everything with Podman

From the repository root:

```bash
podman compose up --build
```

Services:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres: `localhost:5432`

What this does:

- builds a PostgreSQL image with timezone data installed
- starts PostgreSQL with a persistent named volume
- builds and starts the Spring Boot backend
- builds the Vite frontend and serves it through Nginx

To stop everything:

```bash
podman compose down
```

To stop and remove volumes too:

```bash
podman compose down -v
```

Use `-v` when:

- you changed database timezone/container settings
- you want a clean seed reset
- Flyway or seeded data got into a bad state

## 4. Seeded Login for Testing

You can log in with the seeded admin account:

- email: `admin@amiti.local`
- password: `Password1`

## 5. Run Frontend Locally, Backend in Podman

Start only the backend and database:

```bash
podman compose up --build postgres backend
```

Then in a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Open:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`

## 6. Run Fully Local Without Podman

### Backend

Make sure PostgreSQL is running locally and matches the values in `.env` or `backend/src/main/resources/application.yml`.

Then:

```bash
cd backend
mvn spring-boot:run
```

### Frontend

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

## 7. Verify the App

Quick checks:

- open `http://localhost:5173`
- log in with the seeded admin account or register a new user
- confirm the dashboard loads
- add a transaction from the floating action button
- open `http://localhost:8080/api/dashboard/summary`
- open `http://localhost:8080/api/notifications`

Expected current behavior:

- backend returns persisted data for the seeded admin
- frontend shows the routed shell, dashboard widgets, and live settings/search/profile interactions
- auth, accounts, transactions, budgets, recurring, reports, and notifications are wired

## 8. Useful Commands

Rebuild containers:

```bash
podman compose up --build
```

View container status:

```bash
podman ps
```

View logs:

```bash
podman compose logs -f
```

Rebuild frontend locally:

```bash
cd frontend
npm run build
```

Compile backend locally:

```bash
cd backend
mvn -DskipTests compile
```

Run frontend tests:

```bash
cd frontend
npm test
```

## 9. Troubleshooting

### Podman cannot connect

Check Podman machine status:

```bash
podman system connection list
podman machine start
```

### Frontend cannot reach backend

Check:

- backend is running on port `8080`
- `VITE_API_BASE_URL` is correct in `.env`
- if using Podman frontend, rebuild after changing `VITE_API_BASE_URL`

### Backend cannot connect to PostgreSQL

Check:

- PostgreSQL container is running
- `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` match
- port `5432` is available
- backend waits for postgres health before starting

### DBeaver or DB client shows timezone errors

Use a clean DB/container rebuild after the timezone image change:

```bash
podman compose down -v
podman compose up --build
```

Then connect with:

- host: `localhost`
- port: `5432`
- database: `finance_tracker`
- user: `postgres`
- password: `postgres`

If DBeaver still pushes your local timezone automatically, set the driver/session timezone to `UTC`.

### Port already in use

Stop the conflicting service or change the mapped port in `compose.yaml`.
