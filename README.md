# Spendwise

A small personal-finance tracker: log income and expenses, see a monthly summary, and let the app
**auto-categorize** transactions from their description using per-category keywords
(e.g. "Uber to airport" → *Transport*).

| Layer    | Tech                                                                 |
|----------|----------------------------------------------------------------------|
| Backend  | Java 25, Spring Boot 4.2, Spring Security 7, Spring Data JPA, Maven   |
| Database | H2 (file-based, `backend/data/`)                                      |
| Auth     | Stateless JWT **Bearer** tokens (HS256, Spring OAuth2 Resource Server)|
| Frontend | Angular 22 (standalone components, signals, zoneless, lazy routes)    |

## Access (local)

| What | URL | Login |
|---|---|---|
| **Web app** | http://localhost:4200 | `demo@spendwise.dev` / `demo12345` — or create your own account with *Register* |
| REST API | http://localhost:8080/api | Bearer token from `POST /api/auth/login` (see [API](#api)) |
| H2 database console | http://localhost:8080/h2-console | JDBC URL `jdbc:h2:file:./data/spendwise`, user `sa`, password empty |

The demo account is created automatically on the first start (`app.seed-demo-data=true`) with sample
transactions for the current month. These are local development credentials only.

## How auth works

1. `POST /api/auth/register` or `POST /api/auth/login` returns `{ accessToken, tokenType: "Bearer", expiresAt, user }`.
2. The Angular `authInterceptor` adds `Authorization: Bearer <token>` to every API call.
3. Spring Security validates the signature and expiry (`BearerTokenAuthenticationFilter`) — no sessions, no cookies.
   The user id is taken from the token's `sub` claim, so users only ever see their own data.
4. On `401` the frontend clears the token and redirects to `/login`; route guards protect private pages.

## Run it

**Prerequisites:** JDK 25, Node.js 24 LTS (or 22.22+). Maven is not needed — the project has a Maven wrapper.

### Backend (IntelliJ IDEA)
1. *File → Open* → select the `backend` folder (IDEA imports it as a Maven project).
2. *File → Project Structure → SDK* → JDK 25 (IDEA can download it: *Add SDK → Download JDK → 25*).
3. Run `SpendwiseApplication`. API is on http://localhost:8080.

or from a terminal:
```bash
cd backend && ./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```
Open http://localhost:4200. `/api` is proxied to the backend (`proxy.conf.json`).

Log in with `demo@spendwise.dev` / `demo12345` (see [Access](#access-local)).

### Tests

| Kind | Command | What runs |
|---|---|---|
| Unit + API (fast) | `cd backend && ./mvnw test` | 33 tests: services with mocks; HTTP endpoints through MockMvc on in-memory H2 |
| **Integration** | `cd backend && ./mvnw verify` | the above **+ 9 `*IT` tests**: the app starts on a random port and is called over real HTTP (Tomcat, security filters, CORS, H2 console, demo account) |
| Frontend | `cd frontend && npx ng test --watch=false` | 24 tests: auth service, interceptor, guards, API |

In IntelliJ: right-click `src/test/java` → *Run 'All Tests'*, or open the Maven tool window → *Lifecycle → verify*.
A detailed description of the logic (auth flow, auto-categorization, summary, tests) is in [LOGIC.md](LOGIC.md).

## API

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>`.

| Method | Path                                   | Description                                   |
|--------|----------------------------------------|-----------------------------------------------|
| POST   | `/api/auth/register`                   | Create account, returns token                 |
| POST   | `/api/auth/login`                      | Log in, returns token                         |
| GET    | `/api/me`                              | Current user                                  |
| GET    | `/api/transactions?month=2026-11`      | Transactions for a month                      |
| GET    | `/api/transactions/summary?month=...`  | Income, expenses, balance, totals by category |
| POST/PUT/DELETE | `/api/transactions[/{id}]`    | CRUD; omit `categoryId` to auto-categorize    |
| GET    | `/api/categories`                      | User's categories                             |
| GET    | `/api/categories/suggest?description=` | Best matching category (204 if none)          |
| POST/PUT/DELETE | `/api/categories[/{id}]`      | CRUD                                          |

Quick check with curl:
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"demo@spendwise.dev","password":"demo12345"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
curl -s localhost:8080/api/transactions/summary -H "Authorization: Bearer $TOKEN"
```

## Project layout
```
backend/src/main/java/com/spendwise
  auth/         register/login, JWT issuing (TokenService)
  category/     categories + CategorySuggester (keyword matching)
  transaction/  transactions + monthly summary
  config/       SecurityConfig (Bearer/JWT, CORS), demo data
  common/       ProblemDetail error handling
frontend/src/app
  core/         AuthService (signals), authInterceptor, guards, ApiService
  pages/        login, dashboard, categories
```

## Ideas for next steps
- Refresh tokens, roles
- CSV import of bank statements
- Replace keyword matching with an LLM call for smarter categorization
- Flyway migrations instead of `ddl-auto=update`
