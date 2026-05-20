# IssueFlow Status

This file tracks assignment progress. Update it after each major milestone.

## Phase 0 - Documentation and Planning

- [x] Read README API contract.
- [x] Read assignment PDF.
- [x] Choose implementation stack: Java 21, Spring Boot 3.4, PostgreSQL, Spring Data JPA.
- [x] Create `Instructions.md`.
- [x] Create `design.md`.
- [x] Create `status.md`.
- [x] Create `prompts.md` with initial prompt history and model usage.
- [x] Create `run.md` with exact setup, build, run, and test steps.

## Phase 1 - Project Setup

- [x] Clean starter placeholder SQL/files that conflict with the real domain.
- [x] Add required dependencies for security, JWT, testing, and CSV if missing.
- [x] Configure profiles for local PostgreSQL and tests.
- [x] Add global error response shape and exception handler.
- [x] Add shared DTO, validation, and pagination helpers where needed.

## Phase 2 - Authentication and Users

- [x] Implement user entity, repository, DTOs, service, and controller.
- [x] Store password hashes for login support.
- [x] Implement JWT login.
- [x] Implement logout strategy.
- [x] Implement `/auth/me`.
- [x] Protect all non-login endpoints.
- [x] Add user validation and tests.

## Phase 3 - Projects and Tickets

- [x] Implement project CRUD with soft delete and restore.
- [x] Implement ticket CRUD with soft delete and restore.
- [x] Enforce ticket status lifecycle.
- [x] Prevent updates to DONE tickets.
- [x] Add optimistic locking for ticket updates.
- [x] Add `isOverdue` to ticket responses.
- [x] Add tests for project and ticket behavior.

## Phase 4 - Dependencies and Workload

- [x] Implement ticket dependency add/list/remove APIs.
- [x] Enforce same-project dependency rule.
- [x] Block transition to DONE while blockers are unresolved.
- [x] Implement workload endpoint.
- [x] Implement auto-assignment on ticket creation.
- [x] Audit auto-assignment as a system action.
- [x] Add dependency and workload tests.

## Phase 5 - Comments and Mentions

- [x] Implement comment add/list/update/delete APIs.
- [x] Add optimistic locking for comment updates.
- [x] Parse case-insensitive `@username` mentions.
- [x] Persist mention associations.
- [x] Recalculate mentions on comment update.
- [x] Implement mentions-by-user API.
- [x] Add comment and mention tests.

## Phase 6 - Audit Logs

- [x] Implement append-only audit log entity and repository.
- [x] Record manual state-changing actions.
- [x] Record system actions.
- [x] Implement filterable audit log endpoint.
- [ ] Add audit log tests.

## Phase 7 - Attachments

- [x] Implement attachment upload.
- [x] Enforce 10 MB limit.
- [x] Enforce allowed content types.
- [x] Store attachment metadata and content.
- [x] Implement attachment delete.
- [ ] Add attachment tests.

## Phase 8 - CSV Import and Export

- [x] Implement ticket CSV export.
- [x] Implement ticket CSV import.
- [x] Correctly handle commas and quotes in CSV fields.
- [x] Return import summary with row-level errors.
- [ ] Add CSV tests.

## Phase 9 - Auto-Escalation

- [x] Implement scheduled escalation job.
- [x] Promote overdue unresolved tickets by one priority level.
- [x] Set `isOverdue` only when overdue CRITICAL tickets remain unresolved.
- [x] Reset escalation state on manual priority change.
- [x] Audit scheduler changes as system actions.
- [x] Add escalation tests.

## Phase 10 - Finalization

- [ ] Align README with implementation notes if needed.
- [x] Complete `run.md`.
- [x] Complete `prompts.md`.
- [x] Add testing documentation.
- [x] Run full test suite.
- [x] Review API contract against README table.
- [ ] Confirm repository contains no generated secrets or local-only artifacts.
