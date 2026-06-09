# Phase 4: Security and Operational Hardening

## T4.1 Implement auth-safe logging

Add structured logging for:

- registration success/failure
- login success/failure
- token validation failure
- logout success
- user soft delete
- session revocation

Done when:

- logs exclude raw passwords, password hashes, raw tokens, and full sensitive headers
- security-relevant failures remain observable

## T4.2 Add configuration properties and startup validation

Add strongly typed configuration for:

- JWT issuer
- JWT audience
- JWT TTL
- JWT secret/private key
- password hash algorithm
- GraphQL security limits
- CORS origins
- optional rate limiting settings

Done when:

- invalid required configuration fails fast at startup
- environment variables from the spec are documented in config bindings

## T4.3 Configure GraphQL abuse protection

Implement the protections required by the spec for:

- introspection toggle
- query depth limit
- query complexity limit
- request body size limit
- playground/sandbox toggle
- batch request policy

Done when:

- protections are configurable
- disabled features are enforced in the runtime configuration

## T4.4 Evaluate and implement login throttling approach

Add rate limiting only if infrastructure support is available and approved for MVP.

Done when:

- the chosen approach is documented
- if deferred, the backlog records the gap explicitly instead of silently omitting it
