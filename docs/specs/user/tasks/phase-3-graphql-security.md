# Phase 3: GraphQL Contract and Request Security

Follow the foundation decisions in [../foundation.md](../foundation.md), especially the public `User` contract without `deletedAt`.

## T3.1 Create GraphQL schema

Add the schema contract for:

- `DateTime` scalar
- `UUID` scalar
- `Query.me`
- `Mutation.registerUser`
- `Mutation.login`
- `Mutation.logout`
- `Mutation.updateMe`
- `Mutation.deleteMe`
- related input and payload types

Done when:

- schema matches the spec contract
- sensitive fields are absent from the public API

## T3.2 Implement custom GraphQL scalars

Implement `DateTime` and `UUID` scalar support.

Done when:

- timestamps serialize as ISO-8601 UTC strings
- invalid scalar input fails with safe GraphQL errors

## T3.3 Implement GraphQL authentication context

Implement request authentication extraction from the HTTP `Authorization` header and attach authenticated user/session context for protected operations.

Done when:

- bearer token extraction is centralized
- protected resolvers can read authenticated context without trusting client input
- missing or malformed auth headers are rejected consistently

## T3.4 Implement auth guard/directive strategy

Choose and implement one authentication enforcement approach:

- GraphQL directive
- resolver wrapper
- context guard

Done when:

- all protected operations are enforced centrally
- public operations remain accessible without a token

## T3.5 Implement GraphQL resolvers

Implement resolvers for:

- `registerUser`
- `login`
- `logout`
- `me`
- `updateMe`
- `deleteMe`

Done when:

- resolvers delegate to services
- resolvers do not contain password hashing, token generation, or repository logic
- protected resolvers read user identity from the auth context only

## T3.6 Implement GraphQL error mapping

Map domain and validation errors into the spec error shape and codes:

- `BAD_USER_INPUT`
- `UNAUTHENTICATED`
- `FORBIDDEN`
- `NOT_FOUND`
- `CONFLICT`
- `RATE_LIMITED`
- `INTERNAL_SERVER_ERROR`

Done when:

- GraphQL errors do not leak stack traces or internal exception names
- validation errors can include safe field-level messages
