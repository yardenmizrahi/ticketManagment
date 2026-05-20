# IssueFlow AI and Project Instructions

These rules guide all AI-assisted work on this repository. The goal is a simple,
testable, maintainable Spring Boot backend that matches the README API contract.

## Core Rules

- Read `README.md`, `TDP_issueflow_requirements.pdf`, `design.md`, and
  `status.md` before making major implementation changes.
- Explain the design choice before generating or changing code.
- Keep the implementation practical for a home assignment. Do not introduce
  microservices, queues, Kafka, distributed locks, or unrelated infrastructure.
- Prefer simple, testable Spring services over clever abstractions.
- Keep business rules inside the service/domain layer, not controllers.
- Controllers should validate input shape, delegate to services, and return DTOs.
- Use PostgreSQL-compatible persistence through Spring Data JPA.
- Keep API behavior aligned with the README tables unless `design.md` explicitly
  documents a small extension.

## Documentation Rules

- Update `status.md` after each major progress milestone.
- Add meaningful AI prompts and decisions to `prompts.md`.
- Keep `run.md` accurate whenever setup, build, run, or test commands change.
- Keep docs honest: if a feature is partial, mark it partial.
- Document the model and AI usage because the assignment explicitly requires it.

## Code Rules

- Use package boundaries that match the domain: `auth`, `users`, `projects`,
  `tickets`, `comments`, `audit`, `attachments`, and `common`.
- Use DTOs for request and response payloads. Do not expose JPA entities directly.
- Use validation annotations for request DTOs.
- Use a global exception handler for predictable error responses.
- Use optimistic locking for ticket and comment updates.
- Record state-changing operations in the audit log.
- Treat soft-deleted projects and tickets as hidden from standard read APIs.
- Do not hard-delete tickets or projects through public APIs.

## Testing Rules

- Write tests for core business rules, especially:
  - ticket lifecycle transitions;
  - DONE ticket immutability;
  - dependency blocking;
  - auto-assignment;
  - auto-escalation;
  - mention parsing and recalculation;
  - CSV import validation;
  - attachment validation;
  - authorization for admin-only operations.
- Prefer focused service tests for business rules.
- Add controller/integration tests for authentication, request validation, and API
  contract behavior.
- Run the test suite before marking implementation phases complete.

## Reviewer Mindset

- A skeptical reviewer will look for contract mismatches, missing validation,
  weak tests, hidden concurrency bugs, and undocumented AI usage.
- If a shortcut is taken, document it clearly and make sure it is reasonable for
  the assignment scope.
