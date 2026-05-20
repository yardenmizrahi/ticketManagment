# IssueFlow Run Guide

## Prerequisites

- Java 21
- Docker Desktop
- Maven wrapper included in this repo

## Start PostgreSQL

```powershell
docker compose up -d
```

The database uses:

- Host: `localhost`
- Port: `5432`
- Database: `issueflow`
- Username: `issueflow`
- Password: `issueflow`

## Run the Application

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs at:

```text
http://localhost:8080
```

## Seeded Login Users

The app creates two users on startup if they do not already exist:

- `admin` / `issueflow123`
- `dev` / `issueflow123`

Login:

```powershell
curl -X POST http://localhost:8080/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"issueflow123\"}"
```

Use the returned token:

```powershell
curl http://localhost:8080/auth/me `
  -H "Authorization: Bearer <token>"
```

## Run Tests

```powershell
.\mvnw.cmd test
```

The tests run against an in-memory H2 database in PostgreSQL compatibility mode.

## Notes

- All endpoints except `POST /auth/login` require a JWT.
- `POST /users` accepts an optional `password`; if omitted, the default is
  `issueflow123`.
- Soft delete is implemented for projects and tickets.
- Attachments are stored in PostgreSQL as metadata plus `bytea` content.
- Ticket auto-assignment considers all users with role `DEVELOPER` because the
  README does not define a project-membership API.
