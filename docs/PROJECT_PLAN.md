# Personal Finance Tracker Project Plan

## 1. Document Purpose
This document is the source of truth for building and iterating on the Personal Finance Tracker. It should be updated whenever scope, architecture, delivery order, or implementation assumptions change, so future rework can be done against a stable reference instead of reverse-engineering repo state.

## 2. Current Repository Baseline
The repository currently contains an initial scaffold only.

### Present
- `backend`
  - `pom.xml`
  - Spring Boot application entrypoint
  - `application.yml`
  - initial Flyway migration
- `frontend`
  - `package.json`
  - `vite.config.ts`
  - `tsconfig.json`
  - `index.html`
- `docs`
  - current `PROJECT_PLAN.md`
- root
  - `README.md`
  - original product spec `.docx`

### Not Yet Implemented
- Backend domain entities, repositories, services, controllers, DTOs, security flow, scheduler, reports, notifications
- Frontend routed app, feature pages, forms, API client, state management, dashboard widgets, charts, notification UI
- Automated tests
- CI/deployment config
- production-ready operational setup

## 3. Product Summary
Build a V1 web application for individual users to manage personal finances through:
- authentication and onboarding
- account and category management
- income, expense, and transfer tracking
- monthly budgets
- savings goals
- recurring transactions
- reports and CSV export
- in-app notifications
- responsive desktop/mobile experience

## 4. Product Goals
The product must allow a user to:
- create an account and log in securely
- add a transaction in under 15 seconds
- view current month spending by category
- compare budget versus actual spending
- identify recurring payments and upcoming bills
- review spending and income trends over time

## 5. V1 Scope
### In Scope
- Authentication
- Dashboard
- Transactions CRUD
- Categories CRUD
- Monthly budgets
- Savings goals
- Recurring transactions
- Accounts/wallets
- Reporting and charts
- Search and filters
- Responsive desktop and mobile web UI
- CSV export
- In-app notifications and alert banners

### Out of Scope
- Open banking integrations
- Investment portfolio tracking
- Tax filing support
- AI-driven financial advice
- Advanced shared family accounts
- Automated multi-currency conversion
- PDF export in V1
- Bulk transaction actions in V1

## 6. Primary Users
- Young professionals who need quick transaction logging and monthly budget visibility
- Freelancers who need multiple income sources and cash flow visibility
- Goal-oriented savers who need progress tracking and recurring contribution awareness

## 7. Technical Stack
### Frontend
- React
- React Router
- TanStack Query
- React Hook Form
- Zod
- Axios
- Recharts or equivalent chart library

### Backend
- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL
- Scheduled jobs for recurring transactions

### Cross-Cutting
- JWT access tokens
- refresh tokens
- bcrypt or argon2 password hashing
- structured API error responses
- transactional balance updates
- per-user data scoping everywhere

## 8. Target Architecture
### Backend Layers
- Controllers
- Application services
- DTOs
- Entities
- Repositories
- Security and auth support
- Exception handling
- Scheduler/jobs
- Reporting aggregation layer

### Frontend Structure
- `auth`
- `dashboard`
- `transactions`
- `budgets`
- `goals`
- `reports`
- `recurring`
- `accounts`
- `categories`
- `settings`
- shared `components`, `services`, `hooks`, `types`, `utils`, `store`

## 9. Core Domain Model
### Users
- `id`
- `email`
- `password_hash`
- `display_name`
- `created_at`

Rules:
- email unique
- password never stored in plain text

### Accounts
- `id`
- `user_id`
- `name`
- `type`
- `opening_balance`
- `current_balance`
- `institution_name`
- `created_at`
- `last_updated_at`

Types:
- bank account
- credit card
- cash wallet
- savings account

### Categories
- `id`
- `user_id`
- `name`
- `type`
- `color`
- `icon`
- `is_archived`

Rules:
- separate income and expense categories
- default categories seeded
- archived categories remain valid for historical transactions

### Transactions
- `id`
- `user_id`
- `account_id`
- `category_id`
- `type`
- `amount`
- `transaction_date`
- `merchant`
- `note`
- `payment_method`
- `recurring_transaction_id`
- `tags`
- `created_at`
- `updated_at`

Types:
- income
- expense
- transfer

Rules:
- amount must be greater than zero
- category required except for transfer
- support back-dated entries

### Budgets
- `id`
- `user_id`
- `category_id`
- `month`
- `year`
- `amount`
- `alert_threshold_percent`

Rules:
- one budget per category per month per user

### Goals
- `id`
- `user_id`
- `name`
- `target_amount`
- `current_amount`
- `target_date`
- `linked_account_id`
- `icon`
- `color`
- `status`

Rules:
- target amount greater than zero
- status should support at least `active` and `completed`

### Recurring Transactions
- `id`
- `user_id`
- `title`
- `type`
- `amount`
- `category_id`
- `account_id`
- `frequency`
- `start_date`
- `end_date`
- `next_run_date`
- `auto_create_transaction`
- `paused`

Frequencies:
- daily
- weekly
- monthly
- yearly

## 10. API Contract
### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### Transactions
- `GET /api/transactions`
- `POST /api/transactions`
- `GET /api/transactions/{id}`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`

### Categories
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

### Accounts
- `GET /api/accounts`
- `POST /api/accounts`
- `PUT /api/accounts/{id}`
- `POST /api/accounts/transfer`

### Budgets
- `GET /api/budgets?month={m}&year={y}`
- `POST /api/budgets`
- `PUT /api/budgets/{id}`
- `DELETE /api/budgets/{id}`

### Goals
- `GET /api/goals`
- `POST /api/goals`
- `PUT /api/goals/{id}`
- `POST /api/goals/{id}/contribute`
- `POST /api/goals/{id}/withdraw`

### Reports
- `GET /api/reports/category-spend`
- `GET /api/reports/income-vs-expense`
- `GET /api/reports/account-balance-trend`
- `POST /api/reports/export/csv`

### Recurring
- `GET /api/recurring`
- `POST /api/recurring`
- `PUT /api/recurring/{id}`
- `DELETE /api/recurring/{id}`

### Dashboard and Notifications
- `GET /api/dashboard/summary`
- `GET /api/dashboard/recent-transactions`
- `GET /api/dashboard/upcoming-recurring`
- `GET /api/dashboard/budget-progress`
- `GET /api/dashboard/goals-summary`
- `GET /api/notifications`

## 11. Functional Areas

### Authentication and Session
Features:
- sign up with email, password, display name
- login/logout
- forgot password
- reset password
- stay signed in via refresh token
- protected routes and authenticated API access

Validation:
- unique email
- password minimum 8 characters
- password must include uppercase, lowercase, and number

### Onboarding
Flow:
1. user signs up
2. user creates first account
3. user optionally creates first budget
4. user lands on dashboard

Rules:
- onboarding should be skippable where the spec says optional
- app must remain usable if optional steps are skipped

### Dashboard
Widgets:
- current month income
- current month expense
- net balance
- budget progress cards
- spending by category chart
- income vs expense trend chart
- recent transactions
- upcoming recurring payments
- savings goal progress summary

Actions:
- add transaction
- view all transactions
- create budget
- add recurring bill
- update goal contribution

### Transactions
Features:
- create
- edit
- delete
- list
- filter by date/category/amount/type/account
- search by merchant or note
- support transfer transactions
- pagination or infinite-scroll-compatible API
- refresh balances and dashboard after changes

### Categories
Features:
- default categories
- add custom category
- edit icon/color
- archive category
- separate income and expense types

Default expense categories:
- Food
- Rent
- Utilities
- Transport
- Entertainment
- Shopping
- Health
- Education
- Travel
- Subscriptions
- Miscellaneous

Default income categories:
- Salary
- Freelance
- Bonus
- Investment
- Gift
- Refund
- Other

### Accounts
Features:
- create account
- edit account
- view balances
- transfer funds between accounts

### Budgets
Features:
- set monthly budget by category
- see budget vs actual
- threshold alerts at 80%, 100%, 120%
- duplicate last month budget

### Goals
Features:
- create goal
- contribute
- withdraw
- track progress
- mark completed
- optionally link a goal to an account

### Recurring
Features:
- create recurring bill or recurring income
- show next due date
- auto-generate transactions with scheduled job
- pause or delete recurring item

### Reports
Reports:
- monthly spending
- category breakdown
- income vs expense trend
- account balance trend
- savings progress

Filters:
- date range
- account
- category
- transaction type

Export:
- CSV in V1
- PDF deferred to V1.1

### Notifications
Immediate UI feedback:
- transaction saved successfully
- generic success/error toasts for mutations

Persistent or derived alerts:
- budget 80% used
- budget exceeded
- budget 120% exceeded
- upcoming recurring payment in 3 days
- goal reached
- session expired

Channels in V1:
- in-app toast
- in-app alert banners

## 12. UX and Design System
### Navigation
Main:
- Dashboard
- Transactions
- Budgets
- Goals
- Reports
- Recurring
- Accounts
- Settings

Secondary utilities:
- global add transaction
- search
- date range picker
- notifications
- user profile menu

### Visual Direction
- primary: deep blue or indigo
- success: green
- warning: amber
- danger: red
- background: light neutral gray
- cards: white with subtle shadow

### Components
- app shell with sidebar and topbar
- summary cards
- tables
- modal forms
- charts
- progress bars
- tabs
- toasts
- alert banners
- empty states

### Responsive Behavior
- desktop: full analytics layout
- tablet: collapsed side nav
- mobile: stacked cards and bottom action button

## 13. Non-Functional Requirements
### Performance
- dashboard load under 2 seconds for normal users
- paginated APIs for large transaction volumes

### Security
- JWT auth
- hashed passwords
- rate limit login endpoints
- HTTPS only in deployment
- server-side validation for financial inputs
- strict record ownership by user

### Reliability
- transaction-safe balance updates
- daily backup strategy
- recurring scheduler must avoid duplicate generated transactions

### Accessibility
- keyboard navigable
- AA color contrast
- labels for form fields
- chart summaries available

## 14. Error and Empty States
Empty states:
- no transactions yet
- no budgets yet
- no goals yet
- no report data

Error states:
- API unavailable
- unauthorized/session expired
- validation error on submit
- failed chart/report fetch

## 15. Telemetry and Audit
Product events:
- `signup_completed`
- `first_transaction_added`
- `budget_created`
- `goal_created`
- `recurring_created`
- `report_exported`

Operational logging:
- audit key money-impacting actions
- log scheduler runs and failures
- log auth failures and validation failures

## 16. Implementation Roadmap
### Phase 1: Foundation
- finalize project scaffolding
- define backend package structure
- define frontend feature structure
- configure environment handling
- complete initial schema and migration strategy
- add shared API error contract
- add README setup instructions

### Phase 2: Auth and Session
- implement register/login/logout
- implement refresh tokens
- implement forgot/reset password
- add route protection and backend auth filters
- add auth forms and session-expiry handling

### Phase 3: Onboarding
- build first-account setup flow
- build optional first-budget flow
- route users to dashboard after onboarding

### Phase 4: Accounts and Categories
- implement CRUD APIs and pages
- seed default categories
- support archive/edit behavior
- support account balances and transfer logic

### Phase 5: Transactions
- implement transaction CRUD APIs and UI
- add filters, search, and pagination
- handle transfer-specific behavior
- update balances and dashboard after writes

### Phase 6: Dashboard
- implement summary aggregation endpoints
- build dashboard cards, charts, recent transactions, recurring preview, goal summary
- add dashboard quick actions

### Phase 7: Budgets
- implement budget APIs and UI
- calculate budget vs actual
- duplicate last month
- surface threshold alerts

### Phase 8: Goals
- implement goals CRUD plus contribute/withdraw
- handle linked-account behavior
- update dashboard goal summary

### Phase 9: Recurring
- implement recurring CRUD and pause support
- implement scheduler
- expose upcoming recurring items in dashboard and notifications

### Phase 10: Reports and Export
- implement aggregated report endpoints
- build reports page and filters
- implement CSV export

### Phase 11: Notifications and Polish
- build toast and alert banner system
- connect notifications to dashboard and mutation flows
- complete empty/error states
- complete responsive behavior
- complete accessibility pass

### Phase 12: Testing and Operational Readiness
- add backend unit and integration tests
- add frontend component and flow tests
- verify acceptance scenarios
- document backup and deployment expectations

## 17. Acceptance Criteria
- user can register, log in, stay signed in, and reset password
- new user can complete onboarding and reach a functional dashboard
- user can create accounts and categories before logging transactions
- user can add, edit, delete, search, and filter transactions
- balances update correctly after income, expense, and transfer operations
- dashboard shows month summary, charts, recent transactions, budgets, recurring items, and goals
- user can set budgets and see threshold-driven alerts
- user can create goals and update progress
- user can define recurring items and see upcoming bills
- reports respond to filters and CSV export returns filtered data
- notifications appear at the correct times
- UI works on desktop, tablet, and mobile

## 18. Testing Plan
### Backend Unit Tests
- auth service
- password validation
- transfer and balance update logic
- budget threshold logic
- goal contribution and withdrawal logic
- recurring next-run calculation
- report aggregation logic

### Backend Integration Tests
- auth endpoints
- user-scoped access enforcement
- CRUD endpoints for accounts, categories, transactions, budgets, goals, recurring
- dashboard aggregation endpoints
- notifications endpoint
- CSV export endpoint

### Frontend Tests
- auth flows
- onboarding flow
- transaction form validation and submission
- dashboard widget rendering
- budget progress display
- goal progress updates
- recurring forms
- report filters and export flow
- toast and alert display

### Manual Acceptance Scenarios
- sign up and complete onboarding
- add first account
- add first expense in under 15 seconds
- exceed 80% budget threshold
- create and complete a goal
- add recurring bill due within 3 days
- export a filtered CSV report
- verify mobile layout

## 19. Assumptions and Defaults
- backend stack is fixed to Java Spring Boot
- frontend stack is fixed to React and Vite
- `Settings` exists as a minimal V1 page shell unless more detailed requirements are added later
- notifications are derived/in-app only in V1 and do not require a separate persisted notification center
- PDF export is deferred
- bulk delete and bulk categorize are deferred
- no multi-user collaboration or shared household permissions in V1

## 20. How to Maintain This Plan
Update this document when any of the following change:
- scope added or removed
- endpoint contract changed
- schema changed
- delivery order changed
- deferred work pulled into V1
- implementation assumptions changed
- repo baseline materially changes

When updating:
- keep sections stable so diffs are easy to read
- mark what is already implemented versus planned
- do not remove deferred items; move them between scope sections
- update acceptance criteria if behavior changes
- update testing plan whenever a new subsystem is added

## 21. Repo Status
### Current milestone state
- Foundation scaffold: in progress
- Podman local workflow for frontend + backend + postgres: implemented
- Backend API structure and module routing: implemented as first-pass demo-backed scaffold
- Frontend app shell and feature routing: implemented as first-pass functional shell
- Backend compile verification: passed
- Frontend production build verification: passed
- Frontend initial test setup: passed
- Persistent domain implementation, real auth, and database-backed business logic: pending
- Automated backend and frontend test depth: started, not complete

### Notes for future reimplementation
- The current backend exposes the planned API surface with demo-backed responses so frontend and container workflow can progress in parallel.
- Replace demo data services with repository-backed services module by module rather than rewriting the API contract.
- Keep Podman support aligned with `.env.example`, `podman-compose.yml`, frontend build-time API configuration, and backend environment handling when deployment changes.
