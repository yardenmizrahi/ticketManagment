# IssueFlow AI Prompt History

Model used: Codex based on GPT-5.

## Initial Planning Prompt

The initial prompt asked for a senior-backend design review for the IssueFlow
assignment, with a documentation-first workflow, practical architecture, and
explicit diagrams. The response produced the first versions of `design.md`,
`status.md`, and `Instructions.md`.

## Stack Correction Prompt

The stack was corrected from the default TypeScript/NestJS assumption to the
existing repository stack: Java 21, Spring Boot 3.4, PostgreSQL, Spring Data
JPA, JUnit, and Mockito.

## Design Review Prompts

Follow-up prompts challenged missing model fields:

- `Project.ownerId` from the PDF/README contract.
- `Ticket.description`.
- `Ticket.projectId`.
- `Comment.authorId`.
- The reason for `User.passwordHash` and `User.createdAt`.

Those reviews clarified the distinction between JPA relationships in entities
and scalar IDs in request/response DTOs.

## Implementation Prompt

The implementation prompt asked to build the planned project. The resulting
work added JWT authentication, users, projects, tickets, comments, mentions,
audit logs, dependencies, attachments, CSV import/export, soft delete,
auto-assignment, auto-escalation, validation, error handling, and focused tests.

## Important AI Instructions Used

- Keep the backend a modular monolith.
- Keep business rules in service/domain classes.
- Do not expose JPA entities directly from controllers.
- Use optimistic locking for ticket/comment updates.
- Update `status.md` after major progress.
- Keep the solution understandable enough for the candidate to explain.
