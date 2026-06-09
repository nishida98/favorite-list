# User Auth MVP Foundation

This document records the phase 0 foundation decisions for the user auth GraphQL MVP and is the source of truth for the downstream task phases.

## 1. Resolved Product Decisions

| Decision | Outcome | Implementation impact |
|---|---|---|
| Registration auto-login | Registration does not auto-login the user. `registerUser` returns only the created user payload. | `login` remains the only token-issuing mutation. |
| Refresh tokens in MVP | Refresh tokens are out of scope for MVP. | `user_login_sessions` is designed so refresh token support can be added later without schema replacement. |
| Nickname uniqueness | Nicknames do not need to be unique. Preserve the submitted casing for display and validate only format/length rules. | Persistence must not enforce nickname uniqueness. |
| User identifier format | Application-generated user ids use UUIDv7. | User creation must use a UUIDv7 generator; database storage remains `UUID`. |
| Password hashing salt | Every password hash must include a unique random salt. | Hashing adapters must use salted password hashing algorithms and tests must verify identical passwords hash differently. |
| Email change | Email change is out of scope for MVP. | `updateMe` only supports `name` and `nickname`. |
| Re-registration after delete | Soft-deleted emails remain reserved. | Soft delete does not release the unique email constraint. |
| Failed login retention | Retain failed login attempts for 90 days. | Phase 4 cleanup work should remove or archive failed attempts older than 90 days. |
| `deletedAt` in GraphQL `User` | Do not expose `deletedAt` in the MVP GraphQL `User` type. | `deletedAt` stays internal to persistence and service layers. |

## 2. Package and Module Layout

Create feature-oriented packages under `com.lhn.favs_list`:

| Package | Ownership |
|---|---|
| `graphql` | GraphQL schema resources, scalars, resolver classes, context/auth guards, and GraphQL-specific error mapping. |
| `auth` | Registration, login, logout orchestration, password hashing abstraction, token service, and auth-specific DTOs/errors. |
| `users` | User profile application service, persistence model, repository contract/adapter, DTOs, and user-specific errors. |
| `sessions` | Login attempt/session persistence model, repository contract/adapter, session status handling, and audit/revocation helpers. |
| `shared` | Cross-cutting configuration, validation helpers, clocks, ID generation, request context, and infrastructure utilities used by more than one feature package. |

Guidelines:

- Resolvers stay in `graphql`; they delegate to `auth` and `users`.
- Repository implementations stay with the owning feature package, not in `graphql`.
- Cross-feature configuration properties and reusable infrastructure belong in `shared`.
- Phase 1 and later work should add subpackages only when they clarify ownership, for example `users.persistence` or `graphql.resolvers`.

## 3. Foundation Dependencies

The MVP foundation uses the following additional dependencies:

| Dependency | Why it is included now |
|---|---|
| Flyway | Versioned database migrations for `users` and `user_login_sessions` starting in phase 1. |
| Spring Validation starter | Bean validation for GraphQL inputs and strongly typed configuration properties. |
| Spring Security Crypto | Password hashing support with Argon2id as the preferred algorithm and bcrypt as the fallback option. |
| Spring Security OAuth2 JOSE | JWT creation and validation support built on Spring Security's JOSE abstractions. |
| Spring Security Test | Test helpers for token/security validation in GraphQL and service tests. |
| Spring Boot Testcontainers + Testcontainers PostgreSQL | Deterministic repository and end-to-end tests against PostgreSQL instead of an in-memory substitute. |

Deliberately excluded in phase 0:

- Refresh token libraries or cookie-specific auth infrastructure, because refresh tokens are out of scope for MVP.
- Additional ORM/query libraries, because JPA already covers the persistence scope in the spec.
- Rate limiting libraries, because login throttling remains conditional and infrastructure-dependent.

## 4. Configuration Baseline

Phase 0 adds configuration support for:

- `app.auth.jwt.*` for issuer, audience, TTL, and signing secret.
- `app.auth.password.algorithm` for password hashing strategy selection.
- `spring.flyway.*` for migration discovery under `classpath:db/migration`.

Downstream tasks must reference this document before changing the GraphQL contract, persistence rules, or security behavior.
