# EduManage Pro

![CI](https://github.com/onlyDevGuy/EduManagePro/actions/workflows/ci.yml/badge.svg)

A multi-tenant SaaS for running schools. Each school signs up, gets its own isolated workspace, and manages students, courses, enrollments, attendance and billing, with online fee payments through Stripe Checkout.

**Stack:** Java 17 · Spring Boot 3.2 · Spring Security · JPA/Hibernate · PostgreSQL + Flyway · JWT · Stripe · React 18 + TypeScript · TanStack Query · Tailwind CSS · Docker · GitHub Actions

![Admin dashboard](docs/screenshots/dashboard.png)

## Features

- **Self-service onboarding:** a school registers with a unique slug and becomes the admin of its own tenant.
- **Students:** create, search, edit and archive student records with guardian contacts.
- **Courses and enrollments:** a course catalogue with fees, and per-course rosters.
- **Attendance:** record a whole class session in one request (present, absent, late, excused) and see per-student attendance rates.
- **Billing:** raise invoices with per-school sequential numbers (`INV-2026-00001`). Students pay through Stripe Checkout, and a signed webhook marks the invoice paid.
- **Three roles:**

  | Role | Can |
  | --- | --- |
  | Admin | Everything, including billing and deleting records |
  | Teacher | Manage students, courses, enrollments and attendance |
  | Student | See courses, and see and pay their **own** invoices only |

| Students (admin) | Billing as a student: only their own invoice |
| --- | --- |
| ![Students](docs/screenshots/students.png) | ![Student billing view](docs/screenshots/billing-student.png) |

## How multi-tenancy works

Every school's data lives in shared tables, separated by a `tenant_id` column (a discriminator, rather than one schema per school):

1. **Sign-in:** the JWT carries the user's tenant id (`tid` claim).
2. **Per request:** `JwtAuthenticationFilter` validates the token, loads the user *within that tenant*, and binds the tenant to the request thread (`TenantContext`). It always clears it afterwards, because servlet threads are reused.
3. **Writes:** new entities get their `tenant_id` from a JPA `@PrePersist` listener (`TenantAwareEntity`), so it can't be forgotten or spoofed by the client.
4. **Reads:** every repository lookup is scoped, for example `findByIdAndTenantId(id, tenantId)`. A record from another school is simply "not found".

Tests in `AccessControlTest` register two schools and prove that neither can read, change, delete or invoice the other's records.

## Security

| Area | What is in place |
| --- | --- |
| Passwords | BCrypt. Login always runs one hash comparison, so unknown schools or emails take as long as a wrong password and can't be discovered by timing. |
| Brute force | An account locks for 15 minutes after 5 failed sign-ins (`LoginAttemptService`). The answer is identical whether the school, email or password was wrong. |
| Tokens | Short-lived access tokens (15 min) and refresh tokens (7 days), typed so neither can stand in for the other. Disabled users can't refresh. |
| Secrets | The `prod` profile refuses to start if `JWT_SECRET` is missing, shorter than 32 bytes, or still a placeholder. Stripe keys come only from the environment. |
| Authorisation | Roles enforced on the server with `@PreAuthorize`. A student can't list or read other students, view rosters or attendance, or open or pay another student's invoice. Other students' invoices return 404 so ids can't be probed. |
| Stripe webhook | Accepted only with a valid `Stripe-Signature` (HMAC). Repeat deliveries are idempotent. |
| Errors | Every error is an RFC 7807 `ProblemDetail` with a client-safe message. Exception text and stack traces are logged, never returned. |
| Demo data | The seeded demo school (with known passwords) exists only in the `dev` profile. The login page only offers its credentials in development builds. |

## Running locally

**Requirements:** JDK 17 and Node 20+. Maven comes with the wrapper, so it doesn't need installing.

```bash
# API: dev profile, in-memory H2, seeds a demo school (http://localhost:8080)
cd backend
./mvnw spring-boot:run
```

```bash
# Web app (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Sign in to the school **`demo-academy`** with password `Password123!` as any of:

| Email | Role |
| --- | --- |
| `admin@demo-academy.test` | Admin |
| `teacher@demo-academy.test` | Teacher |
| `student@demo-academy.test` | Student (Maya Chen) |

API documentation (OpenAPI/Swagger UI) is served at `http://localhost:8080/swagger-ui.html`.

If port 8080 is taken, run the API with `PORT=8081` and set `VITE_API_BASE_URL=http://localhost:8081` in `frontend/.env.local`.

### Production-like, with Docker

```bash
cp .env.example .env        # then set JWT_SECRET (e.g. openssl rand -base64 48)
docker compose up --build
```

This runs PostgreSQL, the API with the `prod` profile (Flyway migrations, schema validation), and the web app.

### Configuration (`prod` profile)

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | PostgreSQL + Flyway instead of H2 |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Database connection (or `DB_HOST`, `DB_PORT`, `DB_NAME`) |
| `JWT_SECRET` | **Required.** Random, at least 32 bytes. |
| `CORS_ORIGINS` | Allowed frontend origins, comma-separated |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | Optional. Billing returns 503 until set. |
| `BOOTSTRAP_ENABLED` | `true` to seed the demo school (off by default outside dev) |

## Tests

```bash
cd backend
./mvnw verify
```

28 tests:

- **`AuthApiTest`:**
  - Registration and validation.
  - Identical responses for unknown school, unknown user and wrong password.
  - Lockout and its expiry, using a controllable clock.
  - Refresh vs access token rules, tampered tokens, and no internals in error bodies.
- **`AccessControlTest`:**
  - Isolation between schools.
  - Teacher and admin permissions.
  - Every student-role restriction listed above.
- **`BillingTest`:**
  - Per-school invoice numbering and validation.
  - Stripe webhooks with valid, invalid and missing signatures (signed in the test exactly as Stripe signs them), plus idempotent retries.
- **`PostgresMigrationTest`:** boots the `prod` profile against a real PostgreSQL 16 using Testcontainers. Flyway applies the migrations and Hibernate validates every entity against them, so schema drift fails the build, not the deploy. It's skipped automatically when Docker isn't available.
- **`SecurityUnitTest`:** JWT secret rules and lockout counting.

CI (GitHub Actions) runs the backend tests, a frontend typecheck and build, and a Docker image build on every push and pull request.

## Project structure

```
backend/
  src/main/java/com/edumanage/
    config/        security filter chain, CORS, typed app properties
    security/      JWT issuing/validation, auth filter, login lockout
    tenant/        TenantContext (request-scoped tenant binding)
    domain/        JPA entities; TenantAwareEntity stamps tenant_id and timestamps
    repository/    tenant-scoped Spring Data repositories
    service/       business rules (students, courses, attendance, invoices, Stripe)
    web/           REST controllers, DTOs, RFC 7807 error handling
    bootstrap/     dev-only demo data
  src/main/resources/db/migration/   Flyway SQL migrations (PostgreSQL)
frontend/
  src/pages/       Login, Register, Dashboard, Students, Courses, Attendance, Billing
  src/lib/         API client (token refresh) and auth context
```

## Author

**Sizwe Ramokhali**, Software Developer
