# API Specification

This document reflects the current backend contract and the target API structure used by the project.

## Status
- Endpoint paths below are implemented and aligned with the planned API surface.
- Protected endpoints now use `Authorization: Bearer <accessToken>`.
- Budget write requests use `categoryId` to match the rest of the contract.
- Enum values are case-insensitive at the API boundary, so values such as `expense`, `EXPENSE`, `monthly`, and `MONTHLY` are all accepted.

## Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### Register Request
```json
{
  "displayName": "Amiti Admin",
  "email": "admin@amiti.local",
  "password": "Password1"
}
```

### Login Request
```json
{
  "email": "admin@amiti.local",
  "password": "Password1"
}
```

### Auth Response
```json
{
  "userId": "uuid",
  "displayName": "Amiti Admin",
  "email": "admin@amiti.local",
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token",
  "expiresAt": "2026-03-19T10:00:00Z"
}
```

## Transactions
- `GET /api/transactions`
- `POST /api/transactions`
- `GET /api/transactions/{id}`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`

### Create Transaction Request
```json
{
  "type": "expense",
  "amount": 42.50,
  "date": "2026-03-13",
  "accountId": "uuid",
  "categoryId": "uuid",
  "merchant": "Grocery Mart",
  "note": "Weekly groceries",
  "tags": ["family", "weekly"]
}
```

## Categories
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`

### Category Request
```json
{
  "name": "Food",
  "type": "expense",
  "color": "#16a34a",
  "icon": "shopping-bag",
  "archived": false
}
```

## Accounts
- `GET /api/accounts`
- `POST /api/accounts`
- `PUT /api/accounts/{id}`
- `POST /api/accounts/transfer`

### Account Request
```json
{
  "name": "HDFC Bank",
  "type": "bank_account",
  "openingBalance": 1200.00,
  "institutionName": "HDFC"
}
```

### Transfer Request
```json
{
  "sourceAccountId": "uuid",
  "destinationAccountId": "uuid",
  "amount": 250.00
}
```

## Budgets
- `GET /api/budgets?month=3&year=2026`
- `POST /api/budgets`
- `PUT /api/budgets/{id}`
- `DELETE /api/budgets/{id}`

### Budget Request
```json
{
  "categoryId": "uuid",
  "month": 3,
  "year": 2026,
  "amount": 800.00,
  "alertThresholdPercent": 80
}
```

## Goals
- `GET /api/goals`
- `POST /api/goals`
- `PUT /api/goals/{id}`
- `POST /api/goals/{id}/contribute`
- `POST /api/goals/{id}/withdraw`

### Goal Request
```json
{
  "name": "Emergency Fund",
  "targetAmount": 100000,
  "targetDate": "2026-12-31",
  "linkedAccountId": "uuid",
  "icon": "shield",
  "color": "#3147a6"
}
```

### Goal Amount Request
```json
{
  "amount": 5000,
  "sourceAccountId": "uuid"
}
```

## Reports
- `GET /api/reports/category-spend`
- `GET /api/reports/income-vs-expense`
- `GET /api/reports/account-balance-trend`

### Implemented Extra
- `POST /api/reports/export/csv`

## Recurring
- `GET /api/recurring`
- `POST /api/recurring`
- `PUT /api/recurring/{id}`
- `DELETE /api/recurring/{id}`

### Recurring Request
```json
{
  "title": "Netflix",
  "type": "expense",
  "amount": 649.00,
  "categoryId": "uuid",
  "accountId": "uuid",
  "frequency": "monthly",
  "startDate": "2026-03-01",
  "endDate": null,
  "autoCreateTransaction": true
}
```

## Implemented Supporting Endpoints
These are present in the current application in addition to the core API list.

### Dashboard
- `GET /api/dashboard/summary`
- `GET /api/dashboard/recent-transactions`
- `GET /api/dashboard/upcoming-recurring`
- `GET /api/dashboard/budget-progress`
- `GET /api/dashboard/goals-summary`

### Notifications
- `GET /api/notifications`
