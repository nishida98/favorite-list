# Software Design Document — GraphQL User Registration, Login, and Token Authentication

## 1. Document Purpose

This document defines the backend specification for implementing user registration, user login, login/session tracking, and token-based authentication in a GraphQL API.

The goal is to provide enough context for an engineering team or an AI coding agent to implement the feature safely, consistently, and with testable acceptance criteria.

---

## 2. Scope

### 2.1 In Scope

The backend must support:

1. User registration with:
   - Email
   - Name
   - Nickname
   - Password
   - Creation timestamp
   - Update timestamp
   - Delete timestamp for soft delete

2. User login with:
   - Email and password validation
   - Login/session tracking in a dedicated table
   - Token generation after successful authentication
   - Token validation for protected GraphQL operations

3. GraphQL API with:
   - A single GraphQL endpoint
   - Public mutations for registration and login
   - Protected queries/mutations for the authenticated user
   - Typed GraphQL inputs and payloads
   - Consistent GraphQL error format

4. Authentication layer that:
   - Extracts the token from incoming GraphQL HTTP requests
   - Validates the token signature and claims
   - Rejects unauthenticated or invalid protected operations
   - Makes the authenticated user context available to resolvers

5. Logout/revocation behavior:
   - Invalidate the current user session/token server-side
   - Record revocation timestamp

6. Basic authenticated user profile retrieval.

### 2.2 Out of Scope for MVP

The following features are intentionally out of scope unless explicitly requested later:

- Password reset flow
- Email verification
- Multi-factor authentication
- Social login
- Admin user management
- Role-based authorization
- Device management UI
- Account lockout UI
- Refresh token rotation UI
- OAuth2/OpenID Connect provider integration
- GraphQL subscriptions
- File uploads

---

## 3. Recommended Product and Technical Decisions

### 3.1 API Style

This project must expose the authentication and user profile features through GraphQL.

Recommended MVP behavior:

- Use one GraphQL HTTP endpoint: `POST /graphql`.
- Use GraphQL queries for reads.
- Use GraphQL mutations for state changes.
- Keep authentication transport through the HTTP `Authorization` header.
- Use GraphQL error extensions for application-level error codes.
- Avoid returning sensitive fields such as `passwordHash`, token hash, raw token metadata, or internal session records.

### 3.2 Token Strategy

Use JWT access tokens for backend authentication.

Recommended MVP behavior:

- Generate an access token on successful login.
- Access token should be short-lived.
- Store a unique token identifier, `jti`, in the login/session table.
- Do not store the raw token in the database.
- Store only token metadata and, if needed, a hash of the token.
- Validate token signature, expiration, issuer, audience, subject, and token id.
- Reject tokens whose `jti` is revoked, expired, or not found in the session table.

Recommended future improvement:

- Add refresh tokens with rotation.
- Store refresh token hashes only.
- Keep access tokens short-lived and refresh tokens revocable.

### 3.3 Password Storage

Passwords must never be stored in plain text.

Use a strong password hashing algorithm:

- Preferred: Argon2id
- Acceptable: bcrypt with a strong work factor
- Each password hash must include a unique random salt
- The salt may be embedded in the encoded password hash; a separate salt column is not required

The user table must store only `password_hash`, never `password`.

### 3.4 Login Table Semantics

The login table should represent authentication attempts and/or active sessions.

Recommended MVP approach:

- Store successful logins as active sessions.
- Store failed login attempts for audit and abuse detection.
- For successful login rows, include token/session metadata.
- For failed login rows, include attempted email, status, failure reason, IP address, and user agent when available.
- Retain failed login attempts for 90 days before cleanup or archival.

### 3.5 Soft Delete

Users should not be physically deleted in the MVP.

Deleting a user should set `deleted_at`.

A soft-deleted user:

- Cannot log in.
- Cannot access protected GraphQL operations.
- Should have all active sessions revoked.

Required MVP rule: keep email unique even after soft delete to avoid account takeover or identity confusion.

### 3.6 Timezone

All timestamps should be stored in UTC.

GraphQL should expose timestamps as ISO-8601 strings through a custom `DateTime` scalar.

---

## 4. Main Assumptions

1. The backend exposes a GraphQL API using JSON over HTTP.
2. The main endpoint is `POST /graphql`.
3. The frontend will send credentials over HTTPS.
4. The API will use bearer tokens in the `Authorization` header.
5. The authentication token will be validated before protected resolvers are executed.
6. The database can enforce unique constraints.
7. The application supports soft delete through `deleted_at`.
8. The first version does not require roles or permissions.
9. The first version does not require email confirmation.
10. The first version does not use GraphQL subscriptions.

---

## 5. Domain Model

### 5.1 User

Represents a registered application user.

Fields:

| Field | Type | Required | Notes |
|---|---:|:---:|---|
| `id` | UUID | Yes | Primary key generated as UUIDv7 |
| `email` | String | Yes | Unique, normalized to lowercase |
| `name` | String | Yes | User's full or display name |
| `nickname` | String | Yes | User nickname/handle |
| `password_hash` | String | Yes | Hashed password only |
| `created_at` | Timestamp UTC | Yes | Set on creation |
| `updated_at` | Timestamp UTC | Yes | Updated on changes |
| `deleted_at` | Timestamp UTC nullable | No | Null when active |

Rules:

- `email` must be normalized before persistence.
- `email` must be unique.
- application-generated user ids must use UUIDv7.
- `password_hash` must never be returned by GraphQL.
- Users with `deleted_at != null` are considered inactive/deleted.

### 5.2 User Login Session

Represents login attempts and authenticated sessions.

Recommended table name: `user_login_sessions`.

Fields:

| Field | Type | Required | Notes |
|---|---:|:---:|---|
| `id` | UUID | Yes | Primary key |
| `user_id` | UUID nullable | No | Null for failed attempts where no user exists |
| `attempted_email` | String | Yes | Email submitted during login |
| `status` | Enum | Yes | `SUCCESS`, `FAILED`, `REVOKED`, `EXPIRED` |
| `failure_reason` | String nullable | No | Example: `INVALID_PASSWORD`, `USER_NOT_FOUND`, `USER_DELETED` |
| `token_jti` | String nullable | No | Unique JWT ID for successful sessions |
| `token_hash` | String nullable | No | Optional hash of issued token; never store raw token |
| `issued_at` | Timestamp UTC nullable | No | Token issue time |
| `expires_at` | Timestamp UTC nullable | No | Token expiration time |
| `revoked_at` | Timestamp UTC nullable | No | Set on logout or forced revocation |
| `last_used_at` | Timestamp UTC nullable | No | Updated when token is accepted |
| `ip_address` | String nullable | No | Request IP if available |
| `user_agent` | String nullable | No | Request User-Agent if available |
| `created_at` | Timestamp UTC | Yes | Record creation time |
| `updated_at` | Timestamp UTC | Yes | Last update time |

Rules:

- `token_jti` must be unique for successful sessions.
- Failed login attempts should not generate tokens.
- Logout must set `revoked_at` and status `REVOKED`.
- Expired tokens should not authenticate requests.
- Soft-deleting a user must revoke all active sessions for that user.

---

## 6. Suggested Database Schema

The SQL below is technology-agnostic and may need small adjustments depending on the selected database.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE user_login_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NULL,
    attempted_email VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(80) NULL,
    token_jti VARCHAR(255) NULL UNIQUE,
    token_hash VARCHAR(255) NULL,
    issued_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    ip_address VARCHAR(80) NULL,
    user_agent VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_login_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_nickname ON users(nickname);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_login_sessions_user_id ON user_login_sessions(user_id);
CREATE INDEX idx_login_sessions_token_jti ON user_login_sessions(token_jti);
CREATE INDEX idx_login_sessions_status ON user_login_sessions(status);
CREATE INDEX idx_login_sessions_expires_at ON user_login_sessions(expires_at);
```

---

## 7. GraphQL API Design

### 7.1 Endpoint

All GraphQL operations should be served through:

```http
POST /graphql
Content-Type: application/json
```

Authenticated operations must include:

```http
Authorization: Bearer <access_token>
```

### 7.2 Transport Request Format

Example GraphQL HTTP request:

```json
{
  "query": "mutation Register($input: RegisterUserInput!) { registerUser(input: $input) { user { id email name nickname createdAt updatedAt } } }",
  "variables": {
    "input": {
      "email": "john@example.com",
      "name": "John Doe",
      "nickname": "johnny",
      "password": "StrongPassword123!"
    }
  }
}
```

### 7.3 GraphQL Schema SDL

```graphql
scalar DateTime
scalar UUID

directive @authenticated on FIELD_DEFINITION

type Query {
  me: User! @authenticated
}

type Mutation {
  registerUser(input: RegisterUserInput!): RegisterUserPayload!
  login(input: LoginInput!): LoginPayload!
  logout: LogoutPayload! @authenticated
  updateMe(input: UpdateMeInput!): User! @authenticated
  deleteMe: DeleteMePayload! @authenticated
}

type User {
  id: UUID!
  email: String!
  name: String!
  nickname: String!
  createdAt: DateTime!
  updatedAt: DateTime!
}

input RegisterUserInput {
  email: String!
  name: String!
  nickname: String!
  password: String!
}

type RegisterUserPayload {
  user: User!
}

input LoginInput {
  email: String!
  password: String!
}

type LoginPayload {
  accessToken: String!
  tokenType: String!
  expiresIn: Int!
  user: User!
}

input UpdateMeInput {
  name: String
  nickname: String
}

type LogoutPayload {
  success: Boolean!
}

type DeleteMePayload {
  success: Boolean!
}
```

### 7.4 Public Operations

The following operations do not require an access token:

- `registerUser`
- `login`

### 7.5 Protected Operations

The following operations require a valid access token:

- `me`
- `logout`
- `updateMe`
- `deleteMe`

Authentication can be enforced through one of these approaches:

1. A schema directive such as `@authenticated`.
2. A resolver wrapper/higher-order function.
3. A GraphQL context guard.
4. Framework-specific GraphQL security configuration.

Recommended approach: centralize authentication in the GraphQL context and enforce protected operations through a resolver guard or directive.

---

## 8. GraphQL Operation Specifications

### 8.1 Register User

Mutation:

```graphql
mutation RegisterUser($input: RegisterUserInput!) {
  registerUser(input: $input) {
    user {
      id
      email
      name
      nickname
      createdAt
      updatedAt
    }
  }
}
```

Variables:

```json
{
  "input": {
    "email": "john@example.com",
    "name": "John Doe",
    "nickname": "johnny",
    "password": "StrongPassword123!"
  }
}
```

Validation:

- `email` is required.
- `email` must be valid.
- `email` must be normalized to lowercase.
- `email` must be unique.
- `name` is required.
- `nickname` is required.
- `password` is required.
- `password` must satisfy the configured password policy.

Recommended password policy for MVP:

- Minimum 8 characters.
- At least one letter.
- At least one number.
- Reject common/weak passwords if a password strength library is available.

Successful response:

```json
{
  "data": {
    "registerUser": {
      "user": {
        "id": "3af4f2de-3f2b-42ec-8b73-fd00a1cc7f8d",
        "email": "john@example.com",
        "name": "John Doe",
        "nickname": "johnny",
        "createdAt": "2026-06-08T22:00:00Z",
        "updatedAt": "2026-06-08T22:00:00Z"
      }
    }
  }
}
```

Possible errors:

| GraphQL Error Code | Scenario |
|---|---|
| `BAD_USER_INPUT` | Invalid input or invalid fields |
| `CONFLICT` | Email already exists |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

Behavior:

1. Validate input.
2. Normalize email.
3. Normalize nickname while preserving the submitted casing for display.
4. Check email uniqueness.
5. Hash password.
7. Create user.
8. Return public user fields only.

Security rule:

- The raw password and password hash must never be returned in the GraphQL response.

---

### 8.2 Login

Mutation:

```graphql
mutation Login($input: LoginInput!) {
  login(input: $input) {
    accessToken
    tokenType
    expiresIn
    user {
      id
      email
      name
      nickname
    }
  }
}
```

Variables:

```json
{
  "input": {
    "email": "john@example.com",
    "password": "StrongPassword123!"
  }
}
```

Successful response:

```json
{
  "data": {
    "login": {
      "accessToken": "eyJhbGciOi...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "user": {
        "id": "3af4f2de-3f2b-42ec-8b73-fd00a1cc7f8d",
        "email": "john@example.com",
        "name": "John Doe",
        "nickname": "johnny"
      }
    }
  }
}
```

Behavior:

1. Validate input.
2. Normalize email.
3. Find active user by email.
4. Reject login if user does not exist.
5. Reject login if user is soft-deleted.
6. Verify password using the configured password hashing algorithm.
7. On failure, create a failed login record.
8. On success:
   - Generate a unique `jti`.
   - Generate a signed JWT access token.
   - Create a successful session record in `user_login_sessions`.
   - Return token and user summary.

Possible errors:

| GraphQL Error Code | Scenario |
|---|---|
| `BAD_USER_INPUT` | Invalid input |
| `UNAUTHENTICATED` | Invalid email or password |
| `FORBIDDEN` | User is deleted or disabled, if disabled status is added later |
| `RATE_LIMITED` | Too many login attempts, if rate limiting is enabled |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

Security rule:

- The mutation should return a generic authentication error for invalid credentials to avoid revealing whether an email exists.

---

### 8.3 Logout

Mutation:

```graphql
mutation Logout {
  logout {
    success
  }
}
```

Required header:

```http
Authorization: Bearer <access_token>
```

Successful response:

```json
{
  "data": {
    "logout": {
      "success": true
    }
  }
}
```

Behavior:

1. Validate the token.
2. Extract `jti` and user id from token claims.
3. Find active login session by `jti`.
4. Set `revoked_at`.
5. Set status to `REVOKED`.
6. Return `success: true`.

Possible errors:

| GraphQL Error Code | Scenario |
|---|---|
| `UNAUTHENTICATED` | Missing, invalid, expired, or revoked token |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

---

### 8.4 Get Current User

Query:

```graphql
query Me {
  me {
    id
    email
    name
    nickname
    createdAt
    updatedAt
  }
}
```

Required header:

```http
Authorization: Bearer <access_token>
```

Successful response:

```json
{
  "data": {
    "me": {
      "id": "3af4f2de-3f2b-42ec-8b73-fd00a1cc7f8d",
      "email": "john@example.com",
      "name": "John Doe",
      "nickname": "johnny",
      "createdAt": "2026-06-08T22:00:00Z",
      "updatedAt": "2026-06-08T22:00:00Z"
    }
  }
}
```

Possible errors:

| GraphQL Error Code | Scenario |
|---|---|
| `UNAUTHENTICATED` | Missing, invalid, expired, or revoked token |
| `NOT_FOUND` | User no longer exists or is soft-deleted |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

---

### 8.5 Update Current User

Mutation:

```graphql
mutation UpdateMe($input: UpdateMeInput!) {
  updateMe(input: $input) {
    id
    email
    name
    nickname
    createdAt
    updatedAt
  }
}
```

Variables:

```json
{
  "input": {
    "name": "John Updated",
    "nickname": "john_updated"
  }
}
```

Required header:

```http
Authorization: Bearer <access_token>
```

Rules:

- `email` update is not included in MVP.
- `password` update is not included in this mutation.
- `updated_at` must be changed after a successful update.
- At least one editable field must be present.

Successful response:

```json
{
  "data": {
    "updateMe": {
      "id": "3af4f2de-3f2b-42ec-8b73-fd00a1cc7f8d",
      "email": "john@example.com",
      "name": "John Updated",
      "nickname": "john_updated",
      "createdAt": "2026-06-08T22:00:00Z",
      "updatedAt": "2026-06-08T23:00:00Z"
    }
  }
}
```

Possible errors:

| GraphQL Error Code | Scenario |
|---|---|
| `BAD_USER_INPUT` | Invalid input |
| `UNAUTHENTICATED` | Missing, invalid, expired, or revoked token |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

---

### 8.6 Soft Delete Current User

Mutation:

```graphql
mutation DeleteMe {
  deleteMe {
    success
  }
}
```

Required header:

```http
Authorization: Bearer <access_token>
```

Successful response:

```json
{
  "data": {
    "deleteMe": {
      "success": true
    }
  }
}
```

Behavior:

1. Validate token.
2. Set user's `deleted_at` timestamp.
3. Update user's `updated_at` timestamp.
4. Revoke all active sessions for the user.
5. Return `success: true`.

Possible errors:

| GraphQL Error Code | Scenario |
|---|---|
| `UNAUTHENTICATED` | Missing, invalid, expired, or revoked token |
| `NOT_FOUND` | User not found |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

---

## 9. Authentication Token Specification

### 9.1 Access Token Claims

The JWT access token should contain at least:

| Claim | Meaning |
|---|---|
| `sub` | User id |
| `jti` | Unique token/session id |
| `iat` | Issued at |
| `exp` | Expiration time |
| `iss` | Token issuer |
| `aud` | Intended API audience |

Example decoded payload:

```json
{
  "sub": "3af4f2de-3f2b-42ec-8b73-fd00a1cc7f8d",
  "jti": "5f9e3ce2-f126-49d0-bec9-0458f333f836",
  "iat": 1780956000,
  "exp": 1780956900,
  "iss": "favs-list-api",
  "aud": "favs-list-client"
}
```

### 9.2 Token Expiration

Recommended MVP default:

- Access token TTL: 15 minutes.

Alternative for simpler local development:

- Access token TTL: 1 hour.

The selected value must be configurable by environment variable.

### 9.3 Token Validation Rules

Every protected GraphQL operation must validate:

1. Token exists in `Authorization: Bearer <token>` header.
2. Token signature is valid.
3. Token algorithm is expected and allowed.
4. Token has not expired.
5. `iss` matches configured issuer.
6. `aud` matches configured audience.
7. `sub` references an active, non-deleted user.
8. `jti` exists in `user_login_sessions`.
9. Session is not revoked.
10. Session is not expired.

### 9.4 GraphQL Context

After successful token validation, the GraphQL context should expose:

```text
authenticatedUserId
sessionId
sessionJti
requestId
ipAddress
userAgent
```

Protected resolvers must read the authenticated user from the context, not from client-provided input.

---

## 10. Backend Architecture

Recommended layered architecture:

```text
GraphQL Resolver
        ↓
Input Validation
        ↓
Application Service
        ↓
Domain Rules
        ↓
Repository / DAO
        ↓
Database
```

### 10.1 Suggested Modules

```text
graphql/
  schema.graphqls
  resolvers/
    AuthResolver
    UserResolver
  scalars/
    DateTimeScalar
    UUIDScalar
  directives/
    AuthenticatedDirective
  errors/
    GraphQLErrorMapper

auth/
  AuthService
  TokenService
  PasswordHasher
  AuthContextFactory
  AuthGuard
  dto/
  errors/

users/
  UserService
  UserRepository
  dto/
  errors/

sessions/
  UserLoginSession
  UserLoginSessionRepository
  LoginAuditService

shared/
  Clock
  IdGenerator
  Validation
  SecurityConfig
  RequestContext
```

### 10.2 Resolver Responsibilities

#### AuthResolver

Responsibilities:

- Expose `registerUser` mutation.
- Expose `login` mutation.
- Expose `logout` mutation.
- Delegate business rules to `AuthService`.
- Never contain password hashing logic directly.
- Never generate tokens directly.

#### UserResolver

Responsibilities:

- Expose `me` query.
- Expose `updateMe` mutation.
- Expose `deleteMe` mutation.
- Read authenticated user id from GraphQL context.
- Delegate business rules to `UserService`.

### 10.3 Service Responsibilities

#### AuthService

Responsibilities:

- Register users.
- Validate login credentials.
- Generate login sessions.
- Revoke sessions during logout.
- Coordinate password hashing and token generation.

#### TokenService

Responsibilities:

- Generate access tokens.
- Validate access tokens.
- Extract claims.
- Enforce issuer, audience, expiration, and algorithm rules.

#### PasswordHasher

Responsibilities:

- Hash raw passwords.
- Verify raw passwords against stored password hashes.
- Encapsulate hashing algorithm configuration.

#### UserService

Responsibilities:

- Retrieve current user profile.
- Update current user profile.
- Soft-delete current user.
- Revoke active sessions after user deletion.

#### UserLoginSessionRepository

Responsibilities:

- Persist successful and failed login attempts.
- Find session by token `jti`.
- Revoke session by `jti`.
- Revoke all active sessions by user id.
- Update `last_used_at` when appropriate.

---

## 11. GraphQL Error Handling

GraphQL responses should follow the standard shape:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Invalid input",
      "path": ["registerUser"],
      "extensions": {
        "code": "BAD_USER_INPUT",
        "fieldErrors": [
          {
            "field": "email",
            "message": "Email is required"
          }
        ]
      }
    }
  ]
}
```

Recommended error codes:

| Code | Meaning |
|---|---|
| `BAD_USER_INPUT` | Invalid request input or validation failure |
| `UNAUTHENTICATED` | Missing, invalid, expired, or revoked token |
| `FORBIDDEN` | Authenticated user is not allowed to perform the operation |
| `NOT_FOUND` | Requested resource does not exist or is not accessible |
| `CONFLICT` | Unique constraint or domain conflict |
| `RATE_LIMITED` | Too many requests or login attempts |
| `INTERNAL_SERVER_ERROR` | Unexpected internal error |

Rules:

- Do not expose stack traces in GraphQL errors.
- Do not expose SQL errors or internal exception names.
- Do not reveal whether an email exists during login.
- Validation errors may include safe field-level messages.

---

## 12. Security Requirements

1. All production traffic must use HTTPS.
2. Passwords must be hashed using Argon2id or bcrypt.
3. Raw passwords must never be logged.
4. Raw tokens must never be logged.
5. Raw tokens should not be stored in the database.
6. Authentication errors should not reveal whether an email exists.
7. Token signing secrets/private keys must come from environment variables or a secrets manager.
8. Token validation must explicitly check the expected signing algorithm.
9. Token validation must check issuer and audience.
10. Soft-deleted users must not authenticate.
11. Logging should include security-relevant events without exposing secrets.
12. Login should be rate-limited by IP and/or email when infrastructure supports it.
13. CORS must be restricted to known frontend origins in production.
14. Error responses must not expose stack traces.
15. GraphQL introspection should be disabled in production unless there is an explicit operational need.
16. GraphQL query depth and complexity limits should be configured to reduce abuse risk.
17. GraphQL request body size should be limited.
18. Batched GraphQL requests should be disabled or limited unless explicitly needed.
19. Protected resolvers must not trust user ids provided by clients.
20. GraphQL playground/sandbox must not be publicly available in production unless protected.

---

## 13. Observability and Logging

### 13.1 Logs

Log the following events:

- User registration succeeded.
- User registration failed due to duplicate email.
- Login succeeded.
- Login failed.
- Token validation failed.
- Logout succeeded.
- User soft delete succeeded.
- Session revocation succeeded.
- GraphQL operation failed due to authentication.

Do not log:

- Raw passwords
- Password hashes
- Raw tokens
- Full sensitive headers
- Full GraphQL variables when they may contain passwords or tokens

### 13.2 Metrics

Recommended metrics:

- `auth_register_success_total`
- `auth_register_failure_total`
- `auth_login_success_total`
- `auth_login_failure_total`
- `auth_token_validation_failure_total`
- `auth_logout_success_total`
- `auth_active_sessions_total`
- `graphql_request_total`
- `graphql_request_duration_seconds`
- `graphql_error_total`
- `graphql_auth_error_total`

### 13.3 Tracing

If distributed tracing is available, tag spans with:

- GraphQL operation name
- GraphQL operation type: query/mutation
- Request id
- Authenticated user id, if available and allowed by privacy rules

Never tag spans with raw passwords, raw tokens, or full authorization headers.

---

## 14. Configuration

Required environment variables:

| Variable | Purpose | Example |
|---|---|---|
| `JWT_ISSUER` | Token issuer | `favs-list-api` |
| `JWT_AUDIENCE` | Token audience | `favs-list-client` |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | Access token lifetime | `900` |
| `JWT_SECRET` or `JWT_PRIVATE_KEY` | Signing secret/key | Secret value |
| `PASSWORD_HASH_ALGORITHM` | Password hashing algorithm | `argon2id` or `bcrypt` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | `https://app.example.com` |
| `GRAPHQL_INTROSPECTION_ENABLED` | Enable/disable introspection | `false` in production |
| `GRAPHQL_MAX_QUERY_DEPTH` | Maximum query depth | `8` |
| `GRAPHQL_MAX_QUERY_COMPLEXITY` | Maximum query complexity | `100` |

Optional environment variables:

| Variable | Purpose | Example |
|---|---|---|
| `LOGIN_RATE_LIMIT_PER_MINUTE` | Login throttling | `5` |
| `TOKEN_LAST_USED_UPDATE_INTERVAL_SECONDS` | Avoid updating `last_used_at` on every request | `60` |
| `GRAPHQL_PLAYGROUND_ENABLED` | Enable/disable playground/sandbox | `false` in production |
| `GRAPHQL_MAX_REQUEST_BODY_BYTES` | Maximum request body size | `1048576` |

---

## 15. Validation Rules

### 15.1 Email

- Required.
- Must be syntactically valid.
- Normalize to lowercase.
- Trim whitespace.
- Maximum length: 255 characters.

### 15.2 Name

- Required.
- Trim whitespace.
- Minimum length: 2 characters.
- Maximum length: 255 characters.

### 15.3 Nickname

- Required.
- Trim whitespace.
- Minimum length: 3 characters.
- Maximum length: 80 characters.
- Allowed characters: letters, numbers, underscore, hyphen, and dot.

Suggested regex:

```regex
^[a-zA-Z0-9_.-]{3,80}$
```

### 15.4 Password

- Required.
- Minimum length: 8 characters.
- Maximum length: 128 characters.
- Must contain at least one letter and one number.
- Should reject known weak passwords when possible.

---

## 16. Authorization Model

MVP authorization is simple:

Public GraphQL mutations:

- `registerUser`
- `login`

Protected GraphQL operations:

- `me`
- `logout`
- `updateMe`
- `deleteMe`

Future extension:

- Add roles such as `USER`, `ADMIN`.
- Add permission checks for admin resources.
- Add schema directives such as `@hasRole(role: ADMIN)`.

---

## 17. Sequence Flows

### 17.1 Registration Flow

```text
Client
  -> POST /graphql mutation registerUser
Backend
  -> Parse GraphQL request
  -> Validate input
  -> Normalize email and nickname
  -> Check email uniqueness
  -> Hash password
  -> Create user
  -> Return public user payload
```

### 17.2 Login Flow

```text
Client
  -> POST /graphql mutation login
Backend
  -> Parse GraphQL request
  -> Validate input
  -> Normalize email
  -> Find active user by email
  -> Verify password
  -> On failure: save failed login attempt and return GraphQL UNAUTHENTICATED error
  -> On success: generate jti
  -> Generate JWT access token
  -> Save successful login session
  -> Return access token and user summary
```

### 17.3 Protected GraphQL Operation Flow

```text
Client
  -> POST /graphql query me with Authorization header
GraphQL Context Factory
  -> Extract bearer token
  -> Validate signature and claims
  -> Extract sub and jti
  -> Find active session by jti
  -> Find active user by sub
  -> Attach authenticated user context
Resolver Guard / Directive
  -> Confirm operation requires authentication
Resolver
  -> Execute business handler
  -> Return GraphQL response
```

### 17.4 Logout Flow

```text
Client
  -> POST /graphql mutation logout with Authorization header
GraphQL Context Factory
  -> Validate token and session
AuthResolver
  -> Call AuthService.logout(currentSessionJti)
AuthService
  -> Revoke session by jti
  -> Return success true
```

---

## 18. Acceptance Criteria

### 18.1 Registration

- A user can register with valid email, name, nickname, and password using the `registerUser` mutation.
- The password is stored as a hash.
- The password hash includes a unique random salt.
- The raw password is never returned in GraphQL responses.
- Duplicate email registration returns a GraphQL `CONFLICT` error.
- Invalid input returns a GraphQL `BAD_USER_INPUT` error.
- `created_at` and `updated_at` are set on creation.
- `deleted_at` is null on creation.

### 18.2 Login

- A registered active user can log in with correct credentials using the `login` mutation.
- Login returns a bearer access token.
- A successful login creates a `user_login_sessions` record.
- Invalid credentials return a GraphQL `UNAUTHENTICATED` error.
- Failed login attempts are recorded.
- Deleted users cannot log in.
- Login response does not expose password hash or internal security fields.

### 18.3 Token Validation

- Protected GraphQL operations reject requests without a token.
- Protected GraphQL operations reject invalid tokens.
- Protected GraphQL operations reject expired tokens.
- Protected GraphQL operations reject revoked tokens.
- Protected GraphQL operations reject tokens for deleted users.
- Valid tokens allow access to protected operations.

### 18.4 Logout

- Logout revokes the current session.
- A revoked token can no longer access protected GraphQL operations.
- Logout returns `success: true` when completed.
- Logout is idempotent from the user's perspective where possible.

### 18.5 Soft Delete

- Deleting a user through `deleteMe` sets `deleted_at`.
- Deleting a user revokes all active sessions.
- Deleted users cannot log in.
- Deleted users cannot access protected GraphQL operations.

---

## 19. Test Plan

### 19.1 Unit Tests

AuthService:

- Registers a valid user.
- Rejects duplicate email.
- Hashes password before persistence.
- Logs in with valid credentials.
- Rejects invalid password.
- Rejects unknown email.
- Rejects soft-deleted user.
- Creates successful session on login.
- Creates failed login record on failed login.
- Revokes session on logout.

TokenService:

- Generates token with expected claims.
- Validates a valid token.
- Rejects expired token.
- Rejects token with invalid signature.
- Rejects token with invalid issuer.
- Rejects token with invalid audience.
- Rejects token with unexpected algorithm.

UserService:

- Returns authenticated user profile.
- Updates name and nickname.
- Soft-deletes current user.
- Revokes all sessions after soft delete.

Resolvers:

- `registerUser` delegates to `AuthService.registerUser`.
- `login` delegates to `AuthService.login`.
- `logout` reads session from GraphQL context.
- `me` reads user id from GraphQL context.
- `updateMe` reads user id from GraphQL context and never from input.
- `deleteMe` reads user id from GraphQL context and never from input.

### 19.2 Integration Tests

- Register user successfully through GraphQL.
- Login successfully through GraphQL.
- Use returned token to call `me`.
- Logout successfully through GraphQL.
- Verify token cannot be reused after logout.
- Soft-delete user and verify all sessions are invalidated.
- Attempt login after soft delete and expect rejection.
- Verify GraphQL validation errors for invalid input.
- Verify protected operations fail without authorization header.

### 19.3 Security Tests

- Password is never returned in GraphQL responses.
- Password hash is never returned in GraphQL responses.
- Login error does not reveal whether email exists.
- Token without `Bearer` prefix is rejected.
- Token with manipulated payload is rejected.
- Token with expired `exp` is rejected.
- Token with revoked `jti` is rejected.
- Protected operations cannot be accessed anonymously.
- Excessive query depth is rejected.
- Excessive query complexity is rejected.
- GraphQL introspection is disabled in production when configured.

---

## 20. Suggested Implementation Tasks

1. Create user entity/model.
2. Create login session entity/model.
3. Create database migration for `users`.
4. Create database migration for `user_login_sessions`.
5. Implement user repository.
6. Implement login session repository.
7. Implement password hashing adapter.
8. Implement token service.
9. Create GraphQL schema file.
10. Configure GraphQL custom scalars: `DateTime` and `UUID`.
11. Implement GraphQL context factory for authentication.
12. Implement authentication guard or directive.
13. Implement `registerUser` mutation resolver.
14. Implement `login` mutation resolver.
15. Implement `logout` mutation resolver.
16. Implement `me` query resolver.
17. Implement `updateMe` mutation resolver.
18. Implement `deleteMe` mutation resolver.
19. Implement GraphQL error mapper.
20. Add unit tests.
21. Add integration tests.
22. Add security-related negative tests.
23. Add GraphQL documentation.
24. Add environment configuration documentation.

---

## 21. Resolved Foundation Decisions

The foundation decisions that unblock implementation are recorded in [foundation.md](./foundation.md).

Resolved for the MVP:

1. Registration and login remain separate. `registerUser` does not issue a token.
2. Refresh tokens are out of scope for MVP.
3. Nicknames do not need to be unique.
4. Application-generated user ids use UUIDv7.
5. Password hashes include a unique random salt.
6. Email change is out of scope for MVP.
7. Soft-deleted emails remain reserved.
8. Failed login attempts are retained for 90 days.
9. The login session table serves both audit and active-session use cases in MVP.
10. Frontend token storage remains a frontend architecture decision; the backend contract stays `Authorization: Bearer <token>`.
11. GraphQL introspection should be disabled in public production environments unless explicitly protected.
12. The API uses schema-first GraphQL.
13. `deletedAt` is not exposed in the public GraphQL `User` type for MVP.
14. Persisted GraphQL queries are out of scope for MVP.

---

## 22. Suggested Future Enhancements

1. Email verification.
2. Password reset.
3. Password change mutation.
4. Refresh token rotation.
5. Account lockout or progressive delay after repeated failed logins.
6. Multi-factor authentication.
7. Role-based authorization.
8. Admin session revocation.
9. Device/session listing.
10. Login notification by email.
11. Suspicious login detection.
12. Separate audit table for immutable security events.
13. Persisted GraphQL queries.
14. Query allow-list for production clients.
15. Federation-ready schema organization if the backend grows into multiple services.

---

## 23. References

This specification follows common security practices aligned with:

- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- OWASP Session Management Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
- OWASP JWT for Java Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html
- OWASP GraphQL Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/GraphQL_Cheat_Sheet.html

---

## 24. Definition of Done

The implementation is complete when:

1. Database migrations are created and tested.
2. User registration works through the `registerUser` mutation with validation and password hashing.
3. Login works through the `login` mutation and creates a session/login record.
4. Tokens are generated with the expected claims.
5. Protected GraphQL operations require valid tokens.
6. Revoked, expired, malformed, or invalid tokens are rejected.
7. Logout revokes the current token/session.
8. Soft delete disables the user and revokes active sessions.
9. Unit and integration tests pass.
10. Sensitive data is not logged or returned in GraphQL responses.
11. Environment variables are documented.
12. Query depth and complexity limits are configured.
13. The GraphQL schema behavior matches this specification.
