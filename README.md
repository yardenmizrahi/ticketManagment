# IssueFlow - Ticket Management Backend Platform

IssueFlow is a Spring Boot REST API for a lightweight project and issue tracking
system. It implements the TDP 2026 assignment contract from
`TDP_issueflow_requirements.pdf`: users, JWT authentication, projects, tickets,
comments, audit logs, dependencies, attachments, CSV import/export, soft delete,
mentions, auto-assignment, and auto-escalation.

## Stack

- Java 21
- Spring Boot 3.4.2
- Spring Web, Spring Security, Spring Data JPA, Bean Validation
- PostgreSQL for local/runtime persistence
- H2 in PostgreSQL compatibility mode for tests
- JUnit 5, Mockito, MockMvc
- Apache Commons CSV
- Lombok

## How To Run

Detailed setup and troubleshooting are in `run.md`.

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

The API runs at:

```text
http://localhost:8080
```

Seeded users are created on startup if missing:

| Username | Password | Role |
|---|---|---|
| `admin` | `issueflow123` | `ADMIN` |
| `dev` | `issueflow123` | `DEVELOPER` |

The password can be changed with `ISSUEFLOW_DEFAULT_PASSWORD`. The JWT signing
secret can be changed with `JWT_SECRET`.

## How To Test

```powershell
.\mvnw.cmd clean test
```

Current verified result:

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```

Testing details are in `testing.md`.

## Authentication

Only `POST /auth/login` is public. All other endpoints require:

```text
Authorization: Bearer <accessToken>
```

`POST /auth/logout` adds the current token to an in-memory deny-list until the
token would naturally expire. This is intentionally simple and appropriate for a
home assignment.

## Implementation Notes

- Controllers return DTOs, not JPA entities.
- Create endpoints return `200 OK` because the assignment README contract uses
  `200 OK`.
- Error responses use a shared `ApiError` shape with `timestamp`, `status`,
  `error`, `message`, and `details`.
- Projects and tickets are soft-deleted with `deletedAt`; normal reads hide
  deleted data.
- Users are hard-deleted. The assignment only requires soft delete for projects
  and tickets.
- Ticket status may stay the same or advance exactly one lifecycle step:
  `TODO -> IN_PROGRESS -> IN_REVIEW -> DONE`.
- DONE tickets cannot be updated.
- A ticket cannot move to DONE while it has unresolved, non-deleted blockers.
- Ticket and comment entities use JPA `@Version` for optimistic locking.
- Attachments are stored in PostgreSQL as metadata plus `bytea` content.
- Auto-assignment uses all users with role `DEVELOPER` because the assignment
  does not define a project-membership API.
- Workload counts non-DONE, non-deleted tickets in the requested project. The
  workload endpoint returns developers who currently have at least one
  non-deleted ticket in that project.
- Auto-escalation runs every 60 seconds and promotes overdue unresolved tickets
  one priority level per run. CRITICAL overdue tickets are marked `isOverdue`.

## Postman

A Postman collection is included:

```text
postman/IssueFlow.postman_collection.json
```

It covers login, users, projects, tickets, comments, mentions, dependencies,
audit logs, attachments, CSV import/export, soft delete, restore, and workload.

## API Contract

### Auth

| Description | Method and path | Body | Response |
|---|---|---|---|
| Login | `POST /auth/login` | `{ "username": "admin", "password": "issueflow123" }` | `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600 }` |
| Logout | `POST /auth/logout` | none | `200 OK` |
| Current user | `GET /auth/me` | none | `{ "id": 1, "username": "admin", "email": "...", "fullName": "...", "role": "ADMIN" }` |

### Users

| Description | Method and path | Notes |
|---|---|---|
| List users | `GET /users` | Returns public user fields only. |
| Get user | `GET /users/{userId}` | Returns `404` if missing. |
| Create user | `POST /users` | Requires `username`, `email`, `fullName`, `role`; optional `password`. |
| Update user | `POST /users/update/{userId}` | Updates `fullName` and/or `role`. |
| Delete user | `DELETE /users/{userId}` | Hard delete. |
| User mentions | `GET /users/{userId}/mentions?page=1&pageSize=20` | Newest mentioned comments first. |

### Projects

| Description | Method and path | Notes |
|---|---|---|
| List projects | `GET /projects` | Hides soft-deleted projects. |
| Get project | `GET /projects/{projectId}` | Hides soft-deleted projects. |
| Create project | `POST /projects` | Requires `name`, `description`, `ownerId`. |
| Update project | `PATCH /projects/{projectId}` | Updates `name` and/or `description`; blank name is rejected. |
| Soft delete project | `DELETE /projects/{projectId}` | Sets `deletedAt`. |
| List deleted projects | `GET /projects/deleted` | ADMIN only. |
| Restore project | `POST /projects/{projectId}/restore` | ADMIN only. |
| Workload | `GET /projects/{projectId}/workload` | Developers with tickets in the project, sorted by open ticket count. |

### Tickets

| Description | Method and path | Notes |
|---|---|---|
| List by project | `GET /tickets?projectId={projectId}` | Hides deleted tickets and deleted projects. |
| Get ticket | `GET /tickets/{ticketId}` | Hides deleted tickets and tickets in deleted projects. |
| Create ticket | `POST /tickets` | Requires `title`, `description`, `status`, `priority`, `type`, `projectId`; optional `assigneeId`, `dueDate`. |
| Update ticket | `PATCH /tickets/{ticketId}` | One-step status transitions only; DONE is immutable. |
| Soft delete ticket | `DELETE /tickets/{ticketId}` | Sets `deletedAt`. |
| List deleted tickets | `GET /tickets/deleted?projectId={projectId}` | ADMIN only. |
| Restore ticket | `POST /tickets/{ticketId}/restore` | ADMIN only; rejected if parent project is deleted. |
| Export CSV | `GET /tickets/export?projectId={projectId}` | `id,title,description,status,priority,type,assigneeId`. |
| Import CSV | `POST /tickets/import` | Multipart `file` plus `projectId`; returns `{ "created": n, "failed": n, "errors": [...] }`. |

### Comments

| Description | Method and path | Notes |
|---|---|---|
| List comments | `GET /tickets/{ticketId}/comments` | Ticket must be active. |
| Add comment | `POST /tickets/{ticketId}/comments` | Requires `authorId`, `content`. |
| Update comment | `PATCH /tickets/{ticketId}/comments/{commentId}` | Recalculates mentions. |
| Delete comment | `DELETE /tickets/{ticketId}/comments/{commentId}` | Hard delete. |

### Dependencies

| Description | Method and path | Notes |
|---|---|---|
| Add dependency | `POST /tickets/{ticketId}/dependencies` | Body `{ "blockedBy": 42 }`; both tickets must be active and in the same project. |
| List dependencies | `GET /tickets/{ticketId}/dependencies` | Deleted blockers are hidden. |
| Remove dependency | `DELETE /tickets/{ticketId}/dependencies/{blockerId}` | Deletes the dependency row. |

### Attachments

| Description | Method and path | Notes |
|---|---|---|
| Upload attachment | `POST /tickets/{ticketId}/attachments` | Multipart `file`; max 10 MB. |
| Delete attachment | `DELETE /tickets/{ticketId}/attachments/{attachmentId}` | Deletes the attachment row. |

Allowed attachment content types:

- `image/png`
- `image/jpeg`
- `application/pdf`
- `text/plain`

### Audit Logs

| Description | Method and path | Notes |
|---|---|---|
| Query audit logs | `GET /audit-logs` | Optional filters: `entityType`, `entityId`, `action`, `actor`. |

Manual changes are logged with `actor = USER`; auto-assignment and
auto-escalation are logged with `actor = SYSTEM`.

## AI Usage

AI-assisted planning, implementation, review prompts, and decisions are recorded
in `prompts.md`. Project working instructions are in `Instructions.md`.
