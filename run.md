# IssueFlow Run Guide

## Prerequisites

- Java 21
- Docker Desktop
- VS Code with the Java extensions, or another Java IDE
- Maven wrapper included in this repository

For VS Code, install:

- Extension Pack for Java
- Spring Boot Extension Pack
- Lombok Annotations Support for VS Code

If VS Code shows false Java/Lombok errors, run:

1. `Ctrl+Shift+P`
2. `Java: Configure Java Runtime`
3. Select Java 21
4. `Java: Clean Java Language Server Workspace`
5. Reload the window

Maven is the source of truth. If `.\mvnw.cmd clean test` passes, the project
compiles.

## Configuration

Runtime configuration is in `src/main/resources/application.yaml`.

Default local values:

| Setting | Value |
|---|---|
| API port | `8080` |
| Database URL | `jdbc:postgresql://localhost:5432/issueflow` |
| Database username | `issueflow` |
| Database password | `issueflow` |
| JWT expiration | `3600` seconds |
| Default seeded-user password | `issueflow123` |

Optional environment variables:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Overrides the development JWT signing secret. |
| `ISSUEFLOW_DEFAULT_PASSWORD` | Overrides the default password used for seeded users and users created without a password. |

PowerShell example:

```powershell
$env:JWT_SECRET = "replace-with-a-long-local-development-secret"
$env:ISSUEFLOW_DEFAULT_PASSWORD = "issueflow123"
```

## Start PostgreSQL

```powershell
docker compose up -d
```

Check the container:

```powershell
docker compose ps
```

## Run the Application

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs at:

```text
http://localhost:8080
```

If port `8080` is already in use, stop the existing app process or temporarily
change `server.port` in `application.yaml`.

## Seeded Login Users

The app creates these users on startup if they do not already exist:

| Username | Password | Role |
|---|---|---|
| `admin` | `issueflow123` | `ADMIN` |
| `dev` | `issueflow123` | `DEVELOPER` |

Login:

```powershell
curl -X POST http://localhost:8080/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"issueflow123\"}"
```

Use the returned JWT:

```powershell
curl http://localhost:8080/auth/me `
  -H "Authorization: Bearer <token>"
```

## Run Tests

Fast run:

```powershell
.\mvnw.cmd test
```

Final verification run:

```powershell
.\mvnw.cmd clean test
```

Tests use H2 in PostgreSQL compatibility mode and do not require Docker.

## Postman Collection

The collection is here:

```text
postman/IssueFlow.postman_collection.json
```

Recommended smoke-test order:

1. Start PostgreSQL with `docker compose up -d`.
2. Start the app with `.\mvnw.cmd spring-boot:run`.
3. Import the Postman collection.
4. Run `Auth / Login admin`.
5. Run `Auth / Login developer`.
6. Run `Users / List users`.
7. Continue through projects, tickets, comments, mentions, dependencies,
   attachments, CSV import/export, audit logs, soft delete, restore, and
   workload.

The collection uses variables such as `baseUrl`, `token`, `adminToken`,
`devToken`, `userId`, `projectId`, `ticketId`, and `commentId`. Several
requests save tokens and IDs automatically.

## Build a Jar

```powershell
.\mvnw.cmd clean package
```

Run the packaged jar:

```powershell
java -jar target\issueflow-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

- **VS Code shows Lombok getter/setter errors:** install Lombok support, clean
  the Java language server workspace, and reload.
- **Maven cannot download dependencies:** check internet access and try the
  command again.
- **Docker permission error:** make sure Docker Desktop is running.
- **Port 8080 in use:** stop the existing app or change `server.port`.
- **Login fails after changing default password:** seeded users keep their
  existing password hash. Either use the old password or recreate the database
  volume for a clean seed.
