# Phase 5: Testing

Test expectations must align with the resolved foundation decisions in [../foundation.md](../foundation.md).

## T5.1 Unit tests for auth services

Cover:

- valid registration
- duplicate email
- duplicate nickname
- password hashing before persistence
- valid login
- invalid password
- unknown email
- deleted user login rejection
- successful session creation
- failed login record creation
- logout revocation

## T5.2 Unit tests for token service

Cover:

- token generation with expected claims
- valid token acceptance
- expired token rejection
- invalid signature rejection
- invalid issuer rejection
- invalid audience rejection
- unexpected algorithm rejection

## T5.3 Unit tests for user service

Cover:

- current user retrieval
- valid profile update
- duplicate nickname rejection
- soft delete
- active session revocation after delete

## T5.4 Resolver and GraphQL layer tests

Cover:

- resolver delegation
- auth context usage
- protected operation enforcement
- public operation accessibility
- GraphQL validation error mapping

## T5.5 Integration tests

Cover the end-to-end flows for:

- register user
- login user
- call `me` with returned token
- logout user
- verify revoked token rejection
- soft-delete user
- verify deleted user cannot log in
- verify protected operations fail without auth header

## T5.6 Security-focused negative tests

Cover:

- password and password hash never appear in GraphQL responses
- login error does not reveal account existence
- token without `Bearer` prefix is rejected
- manipulated token is rejected
- revoked token is rejected
- excessive query depth is rejected
- excessive query complexity is rejected
- introspection is disabled when configured
