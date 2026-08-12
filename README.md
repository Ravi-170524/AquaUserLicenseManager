# Aqua User & License Manager

A small standalone tool for managing who can log in with project credentials and
what they're allowed to do: **Access**, **Modify**, and **Approve** permissions,
each gated by a **license** (type + expiry) that must be active for the account
to log in at all.

Two modules:

- `backend/` — Spring Boot 3 REST API (Java 17). Owns users, permissions and
  licenses in an embedded H2 database file, issues JWTs on login, and enforces
  that only accounts marked `admin` can manage other users.
- `desktop-ui/` — JavaFX desktop app. Log in, then add/edit/delete users, grant
  Access/Modify/Approve, and renew or revoke licenses from a table view.

## Prerequisites

- Java 17 (`JAVA_HOME` should point at a JDK 17 install)
- Maven 3.6+
- Linux (the desktop UI's `pom.xml` pins the `linux` classifier for JavaFX's
  native libraries — see "Running on Windows/Mac" below to change it)

## Run the backend

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8181`. On first run it creates a default admin
account and logs the password once at startup:

```
Created default admin account (username='admin', password='admin123').
Log in and change this password immediately.
```

Data is stored in `~/.aulm/aulm-db.mv.db` (H2 file database), so it persists
across restarts. Delete that file to reset everything.

To use a different JWT signing secret in production, set `AULM_JWT_SECRET`.

## Run the desktop UI

In a second terminal, once the backend is up:

```bash
cd desktop-ui
mvn javafx:run
```

Log in with the admin account above. From the main window you can:

- **Add User** — set username/password, full name/email, tick any of
  Access/Modify/Approve, optionally mark them `admin` (able to manage other
  users), and issue their initial license (type + expiry, or "never expires").
- **Edit** — change name/email/permissions/admin flag, reset the password, or
  enable/disable the account.
- **Renew License** — change license type/expiry or revoke it outright. A
  revoked or expired license blocks that user's next login immediately.
- **Delete** — removes the user and their license.

Only accounts with `admin=true` can sign in to this tool itself; regular users
just hold a license + permissions that other systems can check against.

### Pointing the UI at a different backend

```bash
mvn javafx:run -Djavafx.args="" -Daulm.backend.url=http://some-host:8181
```

(or pass `-Daulm.backend.url=...` via `MAVEN_OPTS`/IDE run config — the app
reads it as a plain JVM system property).

## Running on Windows/Mac

`desktop-ui/pom.xml` pins the JavaFX dependencies to the `linux` classifier so
the build is self-contained on this dev machine. To build on another OS,
change the `<classifier>` on the `javafx-controls`/`javafx-fxml` dependencies
to `win` or `mac` respectively.

## API surface (backend)

| Method | Path                          | Auth        | Purpose                        |
|--------|-------------------------------|-------------|---------------------------------|
| POST   | `/api/auth/login`             | none        | Login, returns JWT + profile   |
| GET    | `/api/users`                  | admin JWT   | List all users                 |
| GET    | `/api/users/{id}`             | admin JWT   | Get one user                    |
| POST   | `/api/users`                  | admin JWT   | Create user + license           |
| PUT    | `/api/users/{id}`             | admin JWT   | Update profile/permissions/admin/enabled/password |
| DELETE | `/api/users/{id}`             | admin JWT   | Delete user                     |
| POST   | `/api/users/{id}/license/renew` | admin JWT | Change license type/expiry, or revoke |

Login fails with 403 if the account is disabled or its license is missing,
revoked, or past its expiry date.
