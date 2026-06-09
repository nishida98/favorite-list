# Phase 2: Domain Services and Security Core

Follow the foundation decisions in [../foundation.md](../foundation.md), especially the no auto-login, no refresh-token MVP, and 90-day failed-login retention rules.

## T2.1 Implement input normalization and validation rules

Implement reusable validation/normalization for:

- email
- name
- nickname
- password

Done when:

- validation rules match the spec limits and formats
- normalization is applied before uniqueness checks and persistence
- field-level validation failures can be mapped to GraphQL errors

## T2.2 Implement password hashing adapter

Implement a password hashing abstraction and adapter using the approved algorithm.

Done when:

- raw passwords can be hashed and verified
- identical raw passwords produce different hashes because each hash includes a unique random salt
- hashing details remain outside resolvers and domain services
- raw passwords are never logged or persisted

## T2.3 Implement token service

Implement JWT creation and validation with support for:

- `sub`
- `jti`
- `iat`
- `exp`
- `iss`
- `aud`

Done when:

- token TTL is configurable
- validation checks signature, algorithm, issuer, audience, expiration, subject, and `jti`
- invalid token cases map cleanly to auth errors

## T2.4 Implement authentication/session domain logic

Implement the service logic to:

- register users
- authenticate credentials
- record failed login attempts
- create successful sessions
- revoke sessions on logout
- revoke all active sessions on soft delete

Done when:

- auth flows follow the spec sequence rules
- login returns a token only after a successful session record is created
- invalid credentials return generic auth failures

## T2.5 Implement user profile service logic

Implement the service logic to:

- fetch current authenticated user
- update allowed profile fields
- soft-delete the current user

Done when:

- email and password updates remain out of scope
- `updatedAt` changes on successful updates
- delete revokes all active sessions for the user
