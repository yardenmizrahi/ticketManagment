# IssueFlow Testing Notes

## Commands

Fast run:

```powershell
.\mvnw.cmd test
```

Final verification:

```powershell
.\mvnw.cmd clean test
```

Tests use H2 in PostgreSQL compatibility mode and do not require Docker.

## Current Coverage

- `IssueFlowApplicationTests`: verifies the Spring Boot context loads with
  security, JPA, repositories, and seeded users.
- `UserManagementIntegrationTest`: covers user create/list/get/update/delete,
  duplicate username/email validation, invalid email validation, missing role
  validation, and verifies password hashes are not exposed in user responses.
- `TicketServiceTest`: covers one-step ticket lifecycle validation, DONE-ticket
  immutability, unresolved dependency blocking, auto-assignment by least
  workload, and auto-escalation behavior.
- `MentionServiceTest`: covers case-insensitive mention parsing, deduplication,
  and unknown mentioned-user validation.
- `IssueFlowApiIntegrationTest`: covers JWT login, `/auth/me`, logout deny-list
  behavior, protected endpoint rejection, admin-only restore/deleted endpoints,
  soft-delete visibility, audit-log persistence, CSV import/export, attachment
  validation, mention recalculation, and system audit logs.
- `OptimisticLockingIntegrationTest`: verifies stale ticket updates are rejected
  by JPA optimistic locking through the `@Version` field.

## Latest Verification

Latest clean verification:

```powershell
.\mvnw.cmd clean test
```

Result:

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```

The clean run recompiled 56 main source files and 6 test source files.

## PostgreSQL Smoke Test

Docker PostgreSQL can be started with:

```powershell
docker compose up -d
```

The previous PostgreSQL smoke test covered:

- `POST /auth/login`
- `GET /auth/me`
- `POST /projects`
- `POST /tickets`
- `POST /tickets/{ticketId}/comments`
- `GET /audit-logs?entityType=TICKET&entityId={ticketId}`
- `DELETE /tickets/{ticketId}`
- `POST /tickets/{ticketId}/restore`

Smoke result at the time: login user `admin`, project created, ticket created,
mention comment created, two ticket audit records found, and soft-delete/restore
completed.

## Remaining Risks and Tradeoffs

- PostgreSQL smoke testing depends on Docker being available locally.
- There is no project-membership API in the README, so auto-assignment uses all
  `DEVELOPER` users and workload is counted within the target project.
- Optimistic locking is verified at the persistence layer because the public API
  does not expose a version field.
- Logout deny-list is in-memory and resets on application restart.
