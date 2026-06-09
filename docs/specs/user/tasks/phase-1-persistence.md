# Phase 1: Persistence and Data Model

Follow the foundation decisions in [../foundation.md](../foundation.md), especially the case-insensitive nickname rule and reserved-email soft delete rule.

## T1.1 Create `users` persistence model

Implement the persistence representation for users with fields required by the spec:

- `id`
- `email`
- `name`
- `nickname`
- `passwordHash`
- `createdAt`
- `updatedAt`
- `deletedAt`

Done when:

- the persistence model matches the spec field set
- soft delete is represented with nullable `deletedAt`
- sensitive fields are not exposed outside persistence/service boundaries

## T1.2 Create `user_login_sessions` persistence model

Implement the persistence representation for login attempts and active sessions with the required fields and status values.

Done when:

- successful and failed login attempts can both be represented
- token metadata is stored without storing the raw token
- session status transitions required by login/logout/delete are representable

## T1.3 Create database migrations

Create migrations for:

- `users`
- `user_login_sessions`
- indexes and constraints from the spec

Done when:

- schema matches the spec
- unique constraints exist for email, nickname, and `token_jti`
- timestamps are present on both tables

## T1.4 Implement user repository layer

Repository responsibilities:

- find active user by id
- find active user by normalized email
- check email uniqueness
- check nickname uniqueness
- save user
- update user

Done when:

- repository methods cover the service use cases in the spec
- soft-deleted users are excluded where required

## T1.5 Implement login session repository layer

Repository responsibilities:

- save successful session
- save failed login attempt
- find session by `jti`
- revoke session by `jti`
- revoke active sessions by user id
- update `last_used_at`

Done when:

- repository supports login, token validation, logout, and soft-delete flows
- revoked and expired session handling is explicit
