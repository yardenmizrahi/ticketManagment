# IssueFlow Testing Notes

## Test Command

```powershell
.\mvnw.cmd test
```

## Current Coverage

- Spring context loads with security, JPA, repositories, and seeded users.
- Ticket lifecycle rejects backward status transitions.
- DONE tickets reject further updates.
- Tickets with unresolved blockers cannot move to DONE.
- Auto-assignment chooses the least-loaded developer.
- Auto-escalation promotes overdue tickets and marks overdue CRITICAL tickets.
- Mention parsing is case-insensitive and rejects unknown users.

## Remaining High-Value Tests

The current implementation is functional, but these tests would further improve
review confidence:

- Controller tests for JWT login and protected endpoint rejection.
- Admin-only authorization tests for deleted/restore endpoints.
- CSV import/export integration tests.
- Attachment upload validation tests.
- Audit log integration tests proving manual and system actions are persisted.
- Optimistic-locking integration test with two concurrent updates.
