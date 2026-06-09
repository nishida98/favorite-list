# Phase 0: Decisions and Foundation

## T0.1 Resolve open product decisions

Confirm the open questions that affect implementation behavior before coding:

- registration does not auto-login the user
- refresh tokens are out of scope for MVP
- nicknames do not need to be unique
- user ids use UUIDv7
- password hashes must include a unique salt
- email change is out of scope for MVP
- soft-deleted emails remain reserved
- failed login retention policy
- whether `deletedAt` should remain exposed in the GraphQL `User` type

Done when:

- each open question has an explicit decision recorded in the spec or a follow-up decision document
- blocked implementation tasks reference those decisions

## T0.2 Define package/module layout

Create the implementation structure consistent with the spec and current project conventions:

- `graphql/`
- `auth/`
- `users/`
- `sessions/`
- `shared/`

Done when:

- package structure is documented before the first feature classes are added
- ownership of services, repositories, resolvers, and shared infrastructure is clear

## T0.3 Add missing infrastructure dependencies

Add the minimum required dependencies and configuration support for:

- database migrations
- password hashing
- JWT generation and validation
- bean validation if not already configured
- test support needed by repository, GraphQL, and security tests

Done when:

- dependency choices are documented
- no dependency is added without a clear use in the MVP
