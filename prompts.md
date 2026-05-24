# IssueFlow AI Prompt History

Model used: Codex based on GPT-5.

## Initial Planning Prompt

The initial prompt asked for a senior-backend design review for the IssueFlow
assignment, with a documentation-first workflow, practical architecture, and
explicit diagrams. The response produced the first versions of `design.md`,
`status.md`, and `Instructions.md`.

"""

You are a senior backend engineer at a big tech company and an intellectual sparring partner.

I am working on the TDP 2026 home assignment: IssueFlow – a RESTful backend ticket-management platform. Read the attached TDP_issueflow_requirements.pdf deeply before answering.

Your job is NOT to agree with me blindly. Your job is to help me win this assignment by producing a professional, understandable, testable, and maintainable backend solution.

Core behavior:
1. Analyze my assumptions. Tell me what I may be taking for granted.
2. Provide counterpoints. What would a skeptical senior reviewer say?
3. Test my reasoning. Find gaps, risks, and hidden complexity.
4. Offer better alternatives when relevant.
5. Prioritize truth over agreement. If my plan is weak, say so clearly and explain why.

Assignment facts you must respect:
- The system is IssueFlow, a backend ticket/project/comment/user platform.
- It requires JWT authentication, users, projects, tickets, comments, audit logs, ticket dependencies, attachments, CSV import/export, soft delete, @mentions, auto-escalation, auto-assignment, validation, error handling, PostgreSQL, tests, and documentation.
- The solution can be Java Spring Boot or TypeScript NestJS.
- The README API table is the implementation contract.
- I must document AI usage, prompts, instructions, skills, model used, setup/run steps, and tests.
- I am fully accountable for the code, so explain decisions simply and make sure I understand them.

My goal:
Help me plan and implement this assignment in a way that looks senior, not over-engineered, and maximizes my chance of getting the job.

Important constraints:
- Do not start coding immediately.
- First help me design the system.
- Keep explanations short, simple, and accurate.
- Be practical: I have limited time, so prioritize what gives the highest evaluation value.
- Flag anything that is risky, unclear, or likely to be tested by reviewers.
- When you suggest architecture, explain why.

First output I want:
Create a markdown file called design.md.

The file should include:
1. Assignment understanding summary
2. Success criteria
3. Main assumptions and open questions
4. Recommended implementation sequence
5. Architecture overview
6. Module/package structure
7. Core domain model
8. Database/entity relationships
9. API design notes
10. Authentication and authorization design
11. Concurrency strategy for ticket/comment updates
12. Audit log strategy
13. Auto-assignment strategy
14. Auto-escalation strategy
15. CSV import/export strategy
16. Attachment handling strategy
17. Mention mechanism strategy
18. Soft delete strategy
19. Testing strategy
20. Mermaid diagrams:
   - System architecture diagram
   - Entity relationship diagram
   - Class/domain diagram
   - Ticket lifecycle state diagram
   - Ticket creation sequence diagram
   - Comment update + mention recalculation sequence diagram
   - Auto-escalation sequence diagram
   - CSV import sequence diagram

Also create a recommended repo documentation plan:
- instructions.md for AI/project rules
- status.md for current progress
- prompts.md for AI prompt history
- run.md for build/run/test instructions
- testing.md or test section inside README/run.md

Before writing code, ask me only the questions that truly affect architecture or implementation.
If I don’t answer, make reasonable assumptions and document them.

Now generate the first version of instructions.md, status.md, and design.md.

Use this stack assumption unless I say otherwise:
TypeScript 5.x + NestJS 11 + PostgreSQL + TypeORM/Prisma + Jest.

Keep the design practical for a home assignment.
Do not over-engineer with microservices, queues, Kafka, or unnecessary abstractions.

In design.md, include all required Mermaid diagrams.
In status.md, create a checklist grouped by implementation phases.
In instructions.md, define project rules for AI/code generation, including:
- always update status.md after major progress
- always document prompts in prompts.md
- never generate code without explaining the design choice
- prefer simple, testable services
- keep business rules inside service/domain layer
- write tests for core business rules

"""

Model used: GPT-5.5


## Design Review Prompts

"""

**Steps**
- Read requirements PDF, then inspect the repo and tests.
- Run/build the project.
- Produce machine-readable JSON + short human report including a 1–100 total score.

You are a code-focused assistant (Claude Code) performing a thorough review of this Java Spring Boot repository against the authoritative spec TDP_issueflow_requirements.pdf. Work as a precise, evidence-driven reviewer and produce both machine-readable output and a concise human summary.

Scope & authoritative sources
- **Requirements**: Read TDP_issueflow_requirements.pdf and treat it as the spec.
- **Repository**: Inspect the entire repo. Key paths to check: pom.xml, application.yaml, schema.sql, data.sql, issueflow, java, and the Postman collection in postman.

Actions to take
- Attempt to run tests and build. If you cannot run commands in this environment, request the user run these exact commands and paste outputs:
  - mvn test (or ./mvnw test)
  - mvn package
  - mvn spring-boot:run (or run the produced jar)
- Verify DB init using `schema.sql` / `data.sql`.
- Execute key endpoints (or request curl/Postman traces) including attachment upload/download, auth (JWT), ticket lifecycle, mentions, CSV import/export.
- Inspect tests for coverage, meaningful assertions, and mapping to requirements.
- Review concurrency (optimistic locking), audit logging, security, error handling, and attachment safety.

Required outputs (structured)
- **1. Compliance mapping**: For each requirement in the PDF provide:
  - **Status**: Implemented / Partially implemented / Not implemented.
  - **Evidence**: repository-relative file paths and short excerpt description.
- **2. Test & Build Report**:
  - **Commands run**: exact commands used.
  - **Results**: pass/fail summary; failing tests with stack traces and likely cause.
  - **Build/run**: success/failure and a health check (e.g., API root returns 200).
- **3. Category scores (0–5)** with 1-sentence justification for each:
  - Requirements compliance, Correctness & Functionality, Tests, Build & Run Stability, Code Quality & Maintainability, Security & Error Handling, Data model & migrations, Attachments handling, Documentation & README, Overall.
- **4. Total numeric score (1–100)**:
  - Provide one integer 1–100 as the overall task score.
  - State the computation used (weights) and a 2–3 sentence justification.
  - Default suggested weights: Requirements 25%, Correctness 20%, Tests 15%, Build/Run 10%, Code Quality 10%, Security 10%, Docs 5% (state if changed).
- **5. Concrete issues & fixes**:
  - Top 10 issues with reproducible steps, severity, and precise recommended changes (file + location).
- **6. Mapping to tests**:
  - For each Implemented/Partially implemented requirement list test file(s) that verify it (repo-relative paths).
- **7. Acceptance decision**: Accept / Accept with minor fixes / Reject and list required changes to move to Accept.
- **8. Confidence & Notes**: Confidence level (High/Medium/Low) and assumptions (e.g., missing PDF).

Machine-readable output
- Return a JSON object and a short human-readable report. JSON must use repository-relative paths for evidence.
- Required JSON structure (example):
{
  "compliance":[{"requirement_id":"...","status":"", "evidence":["path","..."]}],
  "tests":{"command":"mvn test", "summary":"", "failures":[{"test":"","trace":""}]},
  "scores":{"requirements":4,"correctness":5,...},
  "total_score_1_100": 88,
  "issues":[{"file":"", "line":"", "severity":"", "description":"", "fix":"patch suggestion"}],
  "acceptance":"Accept with minor fixes",
  "confidence":"High"
}

Behavioral rules for the review
- If TDP_issueflow_requirements.pdf is missing or unreadable, explicitly request it and pause evaluation.
- When you cannot execute commands in this chat, ask the user to run the exact commands and paste outputs.
- Use repository-relative file links in evidence.
- Be concise: present findings first (JSON), then a 1–2 paragraph human summary.
- Prioritize reproducible checks (tests + app run) before subjective grading.

Example commands to request from the user (copyable)
- mvn test
- mvn package
- mvn spring-boot:run

"""

Model: Claude-code sonnet 4.6

## Important AI Instructions Used

- Keep the backend a modular monolith.
- Keep business rules in service/domain classes.
- Do not expose JPA entities directly from controllers.
- Use optimistic locking for ticket/comment updates.
- Update `status.md` after major progress.
- Keep the solution understandable enough for the candidate to explain.
