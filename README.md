# Aqua User & License Manager

A standalone user/license management tool, shared across multiple projects from
one instance. Each user belongs to a project and holds:

- **Permissions** — any combination of **Access**, **Modify**, **Approve**.
- **A license** — type (`Trial` / `Standard` / `Premium` / `Admin` / `Custom`) with
  an issued/expiry date, that must be active for the account to be usable.
- An **enabled** flag — an independent kill switch, separate from role or license.

New users self-register and existing users request renewals/permission changes;
both go through an **approval workflow** rather than taking effect immediately.
Other systems can check a user's access via a small read-only API
(`GET /api/auth/access-check`).

## Modules

- **`backend/`** — Spring Boot 3 REST API (Java 21). Owns projects, users,
  permissions, licenses and access requests in PostgreSQL, issues JWTs on
  login, sends email for password resets and new-request notifications, and
  also serves the web UI (`backend/src/main/resources/static/index.html`) —
  this is the active front-end.
- **`desktop-ui/`** — JavaFX desktop client from an earlier iteration of this
  app. **Currently disabled** in the root `pom.xml` (commented out of
  `<modules>`) since the web UI covers the same workflows and has continued to
  receive the newer features (projects, access requests, superadmin, etc.);
  the desktop module is kept for reference but isn't built by default.
- **`sql/`** — PostgreSQL schema, applied by hand
  (`spring.jpa.hibernate.ddl-auto=validate`, so Hibernate checks the schema
  matches but never generates it): `01_create_role_and_db.sql` creates the DB
  role/database, `02_create_tables.sql` creates all tables.

## Key concepts

- **Projects** — multiple projects share one AULM instance; a username is
  unique per project, not globally. `aulm` is the app's own bootstrap project
  and is treated as the org-wide admin project: admins there are automatically
  eligible approvers for every other project's access requests.
- **Roles** — `admin` (manages users within their own project), `superAdmin`
  (global; manages the project list only — superadmins don't manage users
  directly), and plain users (hold permissions/license only).
- **Permissions** — `ACCESS`, `MODIFY`, `APPROVE`; independent booleans other
  systems can check via the access-check API.
- **License** — `TRIAL` (1 week), `STANDARD` (1 month), `PREMIUM` (1 year),
  `ADMIN` (never expires), or `CUSTOM` (exact start/end date chosen by the
  requester). An expired or revoked license (other than `ADMIN`) blocks
  license-gated access, independent of the `enabled` flag.
- **`enabled`** — an account-level kill switch. Checked at login, on every
  authenticated request (so disabling someone invalidates their session
  immediately, not just at next login), and by the access-check API.
- **Access requests** — self-registration and later renewal/permission-change
  requests go into a `PENDING` queue instead of applying immediately. A
  request can target one specific admin or "any admin" in the project; the
  requesting user gets a JWT right away so they can log in and watch their
  request's status even before it's approved. Approving/rejecting emails
  aren't sent back to the requester, but a new request emails the target
  admin(s) (best-effort — a mail failure is logged, not fatal).

## Prerequisites

- Java 21 (`JAVA_HOME` should point at a JDK 21 install)
- Maven 3.6+
- PostgreSQL
- (optional) SMTP credentials, for password-reset emails and new-request
  admin notifications — the app works without them, those emails just won't
  send (failures are logged, not fatal)

## Database setup

```bash
psql -U postgres -f sql/01_create_role_and_db.sql   # edit the password in this file first
psql -U aulm_user -d aulm_db -f sql/02_create_tables.sql
```

## Configuration

All of these are optional — `backend/src/main/resources/application.properties`
bakes in localhost-friendly defaults via `${VAR:default}`.

| Variable                                | Default                              | Purpose                                  |
|------------------------------------------|---------------------------------------|-------------------------------------------|
| `AULM_DB_HOST` / `AULM_DB_PORT` / `AULM_DB_NAME` | `localhost` / `5432` / `aulm_db` | PostgreSQL connection                     |
| `AULM_DB_USER` / `AULM_DB_PASSWORD`       | `postgres` / `postgres`               | PostgreSQL credentials                    |
| `AULM_JWT_SECRET`                         | a placeholder — **override in production** | JWT signing secret                  |
| `AULM_SMTP_HOST` / `AULM_SMTP_PORT`       | `smtp.gmail.com` / `587`               | Outgoing mail server                      |
| `AULM_SMTP_USERNAME` / `AULM_SMTP_PASSWORD` | — (required for mail to send)       | SMTP credentials                          |
| `AULM_MAIL_FROM`                          | falls back to `AULM_SMTP_USERNAME`     | From address on outgoing mail             |

## Run the backend

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8181`, serving both the REST API and the web UI at
that same URL.

On an empty database, a seeder creates the `aulm` project and a default
superadmin account on startup, logging the password once:

```
Created default admin account (username='admin', project='aulm', password='admin123').
Log in and change this password immediately.
```

## Using the web UI

Open `http://localhost:8181`. Log in with the seeded account above, or
**Register** as a new user (lands in the Pending Requests queue for an admin
to approve).

- **Login / Register / Forgot Password / Reset Password** — self-service auth
  screens; a forgotten-password link is emailed and expires after 30 minutes,
  single-use.
- **Dashboard** (admin) — Add/Edit/Delete users, Renew License, **Pending
  Requests** (a badge on the button shows the live count and auto-refreshes
  every 30s; approve/reject inline), **Manage Projects** (creating a project
  requires superadmin; any admin can view the list).
- **My Access** (non-admin user) — view current license/permissions, submit a
  renewal or permission-change request if none is already pending.
- **Superadmin landing** — manage the project list only.

## API surface (backend)

| Method | Path                                  | Auth                | Purpose                                          |
|--------|----------------------------------------|----------------------|----------------------------------------------------|
| POST   | `/api/auth/login`                      | none                 | Login, returns JWT + profile                       |
| POST   | `/api/auth/register`                   | none                 | Self-register: creates the account + a `PENDING` registration request, returns a JWT immediately |
| GET    | `/api/auth/me`                         | JWT                  | Current user's profile                             |
| GET    | `/api/auth/admins?projectName=`        | none                 | List eligible admins for a project (populates the "send request to" picker) |
| GET    | `/api/auth/access-check`               | none                 | External check: `userName`/`projectName`/`permissionType` → boolean (enabled + permission + license validity) |
| POST   | `/api/auth/forgot-password`            | none                 | Email a password-reset link if the username/project/email match |
| POST   | `/api/auth/reset-password`             | none                 | Consume a reset token, set a new password           |
| GET    | `/api/projects`                        | none                 | List all projects                                   |
| POST   | `/api/projects`                        | superadmin JWT       | Create a project                                    |
| GET    | `/api/users`                           | admin JWT            | List users (own project, or all projects for a superadmin) |
| GET    | `/api/users/{id}`                      | admin JWT            | Get one user                                        |
| POST   | `/api/users`                           | admin JWT            | Create a user + license directly (bypasses the request/approval flow) |
| PUT    | `/api/users/{id}`                      | admin JWT            | Update profile/permissions/admin/enabled/password   |
| DELETE | `/api/users/{id}`                      | admin JWT            | Delete a user (blocked if they're the last remaining admin) |
| POST   | `/api/users/{id}/license/renew`        | admin JWT            | Change license type/expiry, or revoke               |
| POST   | `/api/access-requests/mine`            | any authenticated JWT| Submit a renewal or permission-change request       |
| GET    | `/api/access-requests/mine`            | any authenticated JWT| List the caller's own requests                      |
| GET    | `/api/access-requests`                 | admin JWT            | List pending requests visible to this admin         |
| POST   | `/api/access-requests/{id}/approve`    | admin JWT            | Approve (sets license/permissions)                  |
| POST   | `/api/access-requests/{id}/reject`     | admin JWT            | Reject with a reason                                |

Login returns 401 for a bad username/project/password combination, or 403 if
the account has been disabled.
