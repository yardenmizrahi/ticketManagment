# IssueFlow AI and Project Instructions

These instructions describe how this repository should be maintained and
reviewed. The goal is a simple, explainable Spring Boot backend that matches the
TDP 2026 IssueFlow assignment.

## Source of Truth

- `TDP_issueflow_requirements.pdf` is the assignment source.
- `README.md` is the public API and behavior contract for this implementation.
- `design.md` explains how the current code works.
- `run.md` explains how to run, test, and smoke-test the app.
- `testing.md` records test coverage and latest verification.
- `prompts.md` records AI usage and important prompts.

## Implementation Rules

- Keep the app a modular monolith.
- Keep controllers thin: validate input, read path/query/body values, delegate to
  services, return DTOs.
- Keep business rules in services.
- Return DTOs, not JPA entities.
- Preserve the README endpoint paths and response shapes.
- Return `200 OK` for create/update/delete operations because the assignment
  contract uses `200 OK`.
- Use `ApiException` and `GlobalExceptionHandler` for predictable errors.
- Do not add infrastructure that is outside assignment scope, such as Kafka,
  queues, microservices, distributed locks, or external storage.

## Domain Rules To Preserve

- All endpoints except `POST /auth/login` require JWT authentication.
- Deleted-list and restore endpoints for projects/tickets are ADMIN only.
- Projects and tickets are soft-deleted; users and comments are hard-deleted.
- Soft-deleted projects hide their tickets from standard ticket/comment/
  dependency/attachment/CSV paths.
- Ticket status can stay the same or advance exactly one step:
  `TODO -> IN_PROGRESS -> IN_REVIEW -> DONE`.
- DONE tickets cannot be updated.
- Tickets cannot move to DONE while unresolved blockers exist.
- Ticket and comment entities use optimistic locking through `@Version`.
- Auto-assignment uses all `DEVELOPER` users because no project-membership API
  exists in the contract.
- Auto-escalation changes priority or `isOverdue`, never ticket status.
- Attachment upload must enforce the 10 MB limit and allowed content types.
- Mentions are case-insensitive and recalculated on comment update.
- Manual and system state changes should be audited.

## Documentation Rules

- Update `README.md`, `design.md`, `run.md`, and `testing.md` whenever behavior,
  setup, test coverage, or assumptions change.
- Keep docs honest. If a feature is partial or a tradeoff exists, say so.
- Do not claim tests pass unless Maven has been run after the relevant changes.
- Record important AI prompts and decisions in `prompts.md`.
- Avoid stale planning language once the code exists; describe what
  the code actually does.

## Testing Rules

Run tests before considering work complete:

```powershell
.\mvnw.cmd clean test
```

Maintain focused tests for:

- authentication and protected endpoints;
- user CRUD and validation;
- admin-only authorization;
- ticket lifecycle and DONE immutability;
- dependencies blocking DONE;
- soft-delete visibility;
- CSV import/export;
- attachment validation;
- audit-log persistence;
- mention parsing and recalculation;
- auto-assignment and auto-escalation;
- optimistic locking.

## Reviewer Mindset

A reviewer, human or AI, should be able to understand:

- what the assignment required;
- how the implementation maps to those requirements;
- how to run the service locally;
- how to run tests;
- what tradeoffs were made and why.

When changing code, prefer small, boring, easy-to-explain changes over clever
abstractions.
