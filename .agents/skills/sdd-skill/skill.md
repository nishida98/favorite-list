# SKILLS.md — SDD and Task Creation

## Purpose

This skill guides an AI agent or software engineer in creating high-quality Software Design Documents (SDDs) and implementation tasks. The goal is to transform unclear product or technical ideas into actionable, reviewable, testable, and maintainable engineering plans.

Use this skill whenever the user asks to create, refine, review, or split a feature, backend service, frontend flow, API, database change, integration, migration, technical challenge, or product MVP into a design document and execution plan.

---

## Core Principles

1. **Clarify intent before design**
   - Understand the business goal, user problem, system boundary, expected users, and MVP scope.
   - Avoid designing for imaginary future requirements unless explicitly requested.

2. **Prefer explicit trade-offs over hidden assumptions**
   - State assumptions clearly.
   - Document rejected alternatives and why they were not selected.
   - Call out risks, constraints, and dependencies.

3. **Design for implementation, not only architecture**
   - The SDD must be detailed enough for an engineer or AI coding agent to implement without repeatedly asking basic questions.
   - Every major design decision should map to one or more implementation tasks.

4. **Tasks must be small, testable, and independently reviewable**
   - Avoid vague tasks such as “implement backend” or “create frontend.”
   - Each task should have clear inputs, outputs, acceptance criteria, and test expectations.

5. **Security, observability, reliability, and maintainability are default requirements**
   - Authentication, authorization, validation, logging, metrics, error handling, and testing should not be treated as optional afterthoughts.

6. **Keep scope controlled**
   - Separate MVP, post-MVP, and future ideas.
   - Do not mix product discovery, architecture, and implementation details without structure.

---

## Required Inputs

Before creating an SDD or tasks, gather as much of the following as possible. If information is missing, make reasonable assumptions and list them explicitly.

### Product Context

- Product or feature name
- Problem being solved
- Target users
- Business goal
- MVP scope
- Out of scope items
- Success metrics, if available

### Functional Requirements

- User flows
- Main use cases
- Edge cases
- Permissions and roles
- Data to be created, read, updated, or deleted
- External integrations
- Notifications, emails, webhooks, or background jobs

### Technical Context

- Frontend framework
- Backend language and framework
- API style: REST, GraphQL, gRPC, event-driven, etc.
- Database type
- Authentication approach
- Infrastructure/cloud provider
- Existing system constraints
- Deployment expectations

### Non-Functional Requirements

- Security requirements
- Performance expectations
- Availability/reliability expectations
- Scalability assumptions
- Observability requirements
- Compliance/privacy requirements
- Internationalization/accessibility requirements, if applicable

---

## SDD Creation Workflow

Follow this process when creating an SDD.

### 1. Understand the Problem

Summarize the feature in plain language.

Include:

- What is being built
- Why it matters
- Who will use it
- What the first usable version must support

### 2. Define Scope

Split scope into:

- **MVP**: must be implemented now
- **Post-MVP**: valuable but not required for first release
- **Out of Scope**: explicitly excluded

This prevents scope creep and helps prioritize implementation tasks.

### 3. Identify Actors and Use Cases

Define system actors, such as:

- Anonymous user
- Authenticated user
- Admin
- External system
- Background worker

For each actor, describe the main use cases and expected outcomes.

### 4. Model the Domain

Identify the core entities and their relationships.

For each entity, define:

- Purpose
- Required fields
- Optional fields
- Status/lifecycle fields
- Timestamps
- Ownership rules
- Soft delete rules, if applicable
- Uniqueness constraints

### 5. Design the API Contract

The API section must be precise enough for implementation.

For REST APIs, include:

- Method and path
- Request body
- Response body
- Status codes
- Validation rules
- Authorization rules
- Error responses

For GraphQL APIs, include:

- Types
- Inputs
- Queries
- Mutations
- Resolvers
- Authorization rules per resolver
- Validation rules
- Error model

For event-driven APIs, include:

- Event name
- Producer
- Consumer
- Payload schema
- Retry behavior
- Idempotency strategy
- Dead-letter handling

### 6. Design Persistence

Define the data model clearly.

Include:

- Tables or collections
- Columns or fields
- Types
- Indexes
- Foreign keys or references
- Unique constraints
- Soft delete strategy
- Audit fields
- Migration notes

Always include created, updated, and deleted timestamps when relevant.

### 7. Define Business Rules

Business rules must be explicit and testable.

Examples:

- A user email must be unique.
- A deleted user cannot log in.
- A token must expire after a configured duration.
- A nickname must be unique only among active users.
- A login attempt must be recorded regardless of success or failure.

### 8. Address Security

Every SDD must include a security section.

Cover:

- Authentication
- Authorization
- Password hashing
- Token expiration
- Input validation
- Rate limiting
- Sensitive data exposure
- Secrets management
- Audit logging
- Abuse cases

For authentication systems, explicitly define:

- Password storage strategy
- Token type
- Token claims
- Refresh behavior, if any
- Revocation strategy, if any
- Session/login tracking

### 9. Address Observability

Define how the feature will be monitored and debugged.

Include:

- Structured logs
- Metrics
- Traces, if applicable
- Error tracking
- Audit events
- Alerts
- Dashboard ideas

### 10. Address Reliability and Failure Modes

Document how the system behaves when something fails.

Include:

- Database failures
- External service failures
- Retry behavior
- Timeout behavior
- Partial failure handling
- Idempotency
- Data consistency expectations

### 11. Define Testing Strategy

The SDD must include tests at multiple levels.

Cover:

- Unit tests
- Integration tests
- API/contract tests
- Authorization tests
- Validation tests
- Error case tests
- Migration tests, when applicable
- End-to-end tests, when applicable

Each important business rule must have at least one corresponding test scenario.

### 12. Create Implementation Tasks

After the design is clear, split the work into tasks.

Tasks should be grouped by implementation layer:

- Project setup/configuration
- Database/migrations
- Domain/model
- Repository/data access
- Service/business logic
- API/resolvers/controllers
- Security/authentication
- Observability
- Tests
- Documentation
- Deployment/configuration

### 13. Validate Traceability

Every requirement should map to:

- A design decision
- One or more implementation tasks
- One or more acceptance criteria
- One or more tests

If a requirement does not map to a task, the plan is incomplete.

---

## Recommended SDD Structure

Use this structure by default.

```markdown
# Software Design Document: <Feature Name>

## 1. Overview

### 1.1 Purpose
### 1.2 Background
### 1.3 Goals
### 1.4 Non-Goals
### 1.5 Assumptions

## 2. Scope

### 2.1 MVP Scope
### 2.2 Post-MVP Scope
### 2.3 Out of Scope

## 3. Users and Use Cases

### 3.1 Actors
### 3.2 Main User Flows
### 3.3 Edge Cases

## 4. Functional Requirements

## 5. Non-Functional Requirements

### 5.1 Security
### 5.2 Performance
### 5.3 Reliability
### 5.4 Observability
### 5.5 Accessibility / Internationalization, if applicable

## 6. Domain Model

### 6.1 Entities
### 6.2 Relationships
### 6.3 State Transitions

## 7. API Design

### 7.1 API Style
### 7.2 Contracts
### 7.3 Validation Rules
### 7.4 Error Model
### 7.5 Authorization Rules

## 8. Data Design

### 8.1 Tables / Collections
### 8.2 Indexes
### 8.3 Constraints
### 8.4 Migrations
### 8.5 Soft Delete / Audit Strategy

## 9. Business Rules

## 10. Security Design

## 11. Observability Design

## 12. Failure Modes and Resilience

## 13. Testing Strategy

## 14. Implementation Plan

## 15. Task Breakdown

## 16. Open Questions

## 17. Alternatives Considered

## 18. Risks and Mitigations

## 19. Definition of Done
```

---

## Task Creation Rules

A good task must be:

- **Atomic**: focused on one clear change
- **Actionable**: an engineer knows exactly what to do
- **Testable**: acceptance criteria can be verified
- **Reviewable**: small enough for a meaningful code review
- **Traceable**: linked to a requirement or design section
- **Ordered**: dependencies are clear

Avoid tasks that are too broad:

```markdown
- Implement authentication
- Create backend
- Add tests
- Build UI
```

Prefer tasks like:

```markdown
- Create users table migration with email uniqueness and audit timestamps
- Implement password hashing service using bcrypt or Argon2
- Add login mutation that validates credentials and returns a signed access token
- Add integration tests for successful login, invalid password, deleted user, and unknown email
```

---

## Recommended Task Template

Use this format for each task.

```markdown
## Task <number>: <Task Title>

### Objective
Describe the specific outcome of this task.

### Context
Explain why this task exists and what requirement/design section it supports.

### Scope
- Include:
  - <What must be implemented>
- Exclude:
  - <What must not be implemented in this task>

### Implementation Notes
- <Technical guidance>
- <Relevant files/modules/layers>
- <Important design constraints>

### Acceptance Criteria
- [ ] <Observable behavior or result>
- [ ] <Validation/error case>
- [ ] <Security/authorization requirement, if applicable>
- [ ] <Observability/logging requirement, if applicable>
- [ ] <Tests are added or updated>

### Test Requirements
- Unit tests:
  - <Scenario>
- Integration tests:
  - <Scenario>
- Contract/API tests:
  - <Scenario>

### Dependencies
- Depends on: <Task IDs or none>
- Blocks: <Task IDs or none>

### Definition of Done
- [ ] Code implemented
- [ ] Tests passing
- [ ] Documentation updated, if needed
- [ ] No known regressions
- [ ] Reviewed against SDD requirements
```

---

## Task Breakdown Best Practices

### Backend Task Order

1. Project configuration and dependencies
2. Database migrations
3. Domain entities/models
4. Repository/data access layer
5. Service/business logic layer
6. API layer: controllers, resolvers, routes, or handlers
7. Authentication and authorization
8. Validation and error handling
9. Observability: logs, metrics, traces
10. Tests
11. Documentation
12. Deployment/configuration

### Frontend Task Order

1. Project setup and routing
2. Design system or layout foundation
3. API client setup
4. State management
5. Main screens/pages
6. Forms and validation
7. Error and loading states
8. Authentication/session handling
9. Responsiveness
10. Accessibility
11. Tests
12. Documentation

### Full-Stack Task Order

1. Define API contract
2. Implement backend persistence
3. Implement backend business logic
4. Implement backend API
5. Add backend tests
6. Implement frontend API integration
7. Implement frontend screens and forms
8. Add frontend validation and states
9. Add end-to-end tests
10. Update documentation

---

## Quality Checklist for SDDs

Before considering an SDD complete, verify:

- [ ] The problem and goal are clear
- [ ] MVP and non-goals are explicit
- [ ] Assumptions are listed
- [ ] Actors and use cases are described
- [ ] Domain entities are defined
- [ ] API contracts are specific
- [ ] Data model includes constraints and indexes
- [ ] Business rules are testable
- [ ] Security is addressed
- [ ] Observability is addressed
- [ ] Failure modes are documented
- [ ] Testing strategy covers happy paths and edge cases
- [ ] Tasks are included or referenced
- [ ] Open questions are listed separately
- [ ] Risks and mitigations are included
- [ ] Definition of Done is clear

---

## Quality Checklist for Tasks

Before considering tasks complete, verify:

- [ ] Each task has a clear objective
- [ ] Each task is small enough to implement independently
- [ ] Each task has acceptance criteria
- [ ] Each task has test requirements
- [ ] Dependencies are explicit
- [ ] Tasks are ordered logically
- [ ] No task mixes unrelated concerns
- [ ] Security tasks are not skipped
- [ ] Observability tasks are not skipped
- [ ] Documentation tasks are included
- [ ] Every major requirement from the SDD maps to at least one task

---

## Acceptance Criteria Guidelines

Acceptance criteria should describe observable behavior, not internal intention.

Weak:

```markdown
- The login should work.
```

Strong:

```markdown
- Given an active user with valid credentials, when the login mutation is executed, then the API returns a signed access token and records a successful login event.
- Given an active user with an invalid password, when the login mutation is executed, then the API returns an authentication error and records a failed login event.
```

Use Given/When/Then format when possible.

---

## Error Model Guidelines

Every API design should define consistent errors.

Include:

- Error code
- Human-readable message
- Field-level validation errors, if applicable
- Correlation/request ID, if applicable
- HTTP status code for REST, if applicable
- GraphQL error extension fields, if applicable

Example GraphQL error extension:

```json
{
  "errors": [
    {
      "message": "Invalid credentials.",
      "extensions": {
        "code": "AUTH_INVALID_CREDENTIALS",
        "requestId": "req_123"
      }
    }
  ]
}
```

---

## Security Checklist

For every SDD, evaluate:

- [ ] Authentication strategy
- [ ] Authorization rules
- [ ] Input validation
- [ ] Password hashing, if applicable
- [ ] Token expiration, if applicable
- [ ] Token revocation/session invalidation, if applicable
- [ ] Rate limiting for sensitive actions
- [ ] Protection against enumeration attacks
- [ ] Sensitive data redaction in logs
- [ ] Secrets management
- [ ] Audit logging
- [ ] Least privilege database/cloud permissions
- [ ] CORS/CSRF considerations, if applicable

---

## Observability Checklist

For every SDD, define:

- [ ] What should be logged
- [ ] What must never be logged
- [ ] Metrics to collect
- [ ] Error events to track
- [ ] Useful dashboards
- [ ] Alerts for critical failures
- [ ] Correlation/request ID propagation

Example metrics:

- Request count
- Error count
- Latency percentiles
- Login success/failure count
- Token validation failures
- Database operation failures

---

## Testing Checklist

Every SDD and task plan should include tests for:

- [ ] Happy path
- [ ] Invalid input
- [ ] Unauthorized access
- [ ] Forbidden access
- [ ] Missing resources
- [ ] Duplicate resources
- [ ] Soft-deleted resources
- [ ] External dependency failure
- [ ] Database constraint violations
- [ ] Concurrency/idempotency, if applicable

---

## Common Mistakes to Avoid

- Writing architecture without implementation tasks
- Creating tasks that are too large
- Omitting security until the end
- Forgetting observability and failure modes
- Not defining validation rules
- Not defining error responses
- Mixing MVP and future scope
- Creating API contracts without examples
- Ignoring database indexes and constraints
- Writing acceptance criteria that cannot be tested
- Leaving important decisions as implicit assumptions

---

## Output Expectations

When asked to create an SDD, produce:

1. A complete SDD using the recommended structure
2. Clear assumptions and open questions
3. API/data/security/testing details
4. A task breakdown
5. A final Definition of Done

When asked to create tasks only, produce:

1. Ordered implementation tasks
2. Dependencies between tasks
3. Acceptance criteria for each task
4. Test requirements for each task
5. Definition of Done

When asked to create both SDD and tasks, ensure tasks directly map to sections in the SDD.

---

## Preferred Writing Style

- Use clear, direct English
- Prefer concrete examples over abstract statements
- Use tables when comparing alternatives or mapping requirements to tasks
- Use bullet points for requirements and checklists
- Keep each section implementation-oriented
- Avoid unnecessary buzzwords
- Be explicit about trade-offs

---

## Final Review Prompt

Before finalizing an SDD or task plan, review it using this prompt:

```markdown
Review this SDD and task breakdown as a senior software engineer.
Check for missing requirements, vague tasks, unclear acceptance criteria, security gaps, observability gaps, testing gaps, and implementation risks.
Suggest concrete improvements and identify anything that would block implementation by an AI coding agent or human engineer.
```
