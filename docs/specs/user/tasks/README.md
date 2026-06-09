# User Auth GraphQL MVP Tasks

Source spec: [../user-spec.md](../user-spec.md)
Foundation baseline: [../foundation.md](../foundation.md)

## Scope

This backlog covers the MVP defined in `user-spec.md` for:

- user registration
- user login with JWT access tokens
- session tracking and revocation
- authenticated GraphQL operations: `me`, `logout`, `updateMe`, `deleteMe`
- validation, security hardening, observability, and tests required by the spec

## Tasking Rules

- Keep changes small and reviewable.
- Preserve the current Spring Boot + Kotlin + Spring GraphQL architecture.
- Do not start implementation tasks that depend on unresolved product decisions.
- Add tests with each behavior change when practical.

## Task Files

- [phase-0-foundation.md](./phase-0-foundation.md)
- [phase-1-persistence.md](./phase-1-persistence.md)
- [phase-2-services-security-core.md](./phase-2-services-security-core.md)
- [phase-3-graphql-security.md](./phase-3-graphql-security.md)
- [phase-4-hardening.md](./phase-4-hardening.md)
- [phase-5-testing.md](./phase-5-testing.md)
- [phase-6-delivery.md](./phase-6-delivery.md)

## Recommended Delivery Order

1. T0.1-T0.3
2. T1.1-T1.5
3. T2.1-T2.5
4. T3.1-T3.6
5. T4.1-T4.4
6. T5.1-T5.6
7. T6.1-T6.3

## Explicit Risks and Blockers

- Downstream phases must follow the resolved decisions in [../foundation.md](../foundation.md).
- The current scaffold does not yet show migration, JWT, or password-hashing choices, so those need explicit selection first.
- Login throttling is specified conditionally and may require infrastructure decisions outside application code.
- MVP decision for phase 4: application-level login throttling remains deferred until shared infrastructure support is approved. `LOGIN_RATE_LIMIT_PER_MINUTE` is bound for future rollout, but request throttling is not enforced in-process yet.
