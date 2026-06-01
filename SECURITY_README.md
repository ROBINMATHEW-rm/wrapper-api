# Spring Security — JWT Authentication & Authorization

This document covers the security layer added to the Enterprise RAG Wrapper API.
It includes JWT-based authentication, role-based access control, and Swagger UI integration.

---

## Overview

| Feature | Details |
|---|---|
| Authentication | JWT (JSON Web Token) — stateless, no sessions |
| Password Storage | BCrypt hashed — never stored in plain text |
| Roles | `ROLE_USER`, `ROLE_ADMIN` |
| Token Expiry | 24 hours (86400000 ms) |
| User Storage | PostgreSQL `users` table (same `rag_db`) |
| Swagger UI | JWT Authorize button built in |

---

## Project Structure

```
src/main/java/com/enterprise_wrapper_api/wrapper_api/security/
│
├── config/
│   ├── SecurityConfig.java          # Spring Security filter chain, role rules
│   └── SwaggerConfig.java           # OpenAPI/Swagger with JWT support
│
├── controller/
│   └── AuthController.java          # /auth/register, /auth/login, /auth/me
│
├── dto/
│   ├── AuthRequest.java             # Login request body
│   ├── AuthResponse.java            # Token + user info response
│   └── RegisterRequest.java         # Registration request body
│
├── entity/
│   └── User.java                    # JPA entity → users table in PostgreSQL
│
├── filter/
│   └── JwtAuthenticationFilter.java # Intercepts every request, validates JWT
│
├── repository/
│   └── UserRepository.java          # JPA queries for users
│
└── service/
    ├── AuthService.java             # Register and login business logic
    ├── JwtService.java              # Generate, validate, parse JWT tokens
    └── UserDetailsServiceImpl.java  # Loads user from DB for Spring Security
```

---

## How It Works

### Request Flow

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter
    │  Reads Authorization: Bearer <token>
    │  Validates token signature + expiry
    │  Loads user from DB
    │  Sets SecurityContext
    ▼
SecurityConfig (authorization rules)
    │  Is endpoint public? → allow
    │  Has required role? → allow
    │  Otherwise → 403 Forbidden
    ▼
Controller
```

### Token Flow

```
POST /auth/register  →  User saved to DB (password BCrypt hashed)  →  JWT returned
POST /auth/login     →  Credentials verified  →  JWT returned
All /rag/** requests →  JWT validated  →  Request processed
```

---

## API Endpoints

### Authentication (Public — no token needed)

#### Register
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@example.com",
  "password": "secret123",
  "admin": false
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "john",
  "email": "john@example.com",
  "role": "ROLE_USER",
  "message": "Registration successful"
}
```

#### Register Admin
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "admin",
  "email": "admin@example.com",
  "password": "admin123",
  "admin": true
}
```

#### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "secret123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "john",
  "email": "john@example.com",
  "role": "ROLE_USER",
  "message": "Login successful"
}
```

#### Get Current User (requires token)
```
GET /api/auth/me
Authorization: Bearer <token>
```

Response:
```json
{
  "username": "john",
  "role": "ROLE_USER"
}
```

---

## Role-Based Access Control

| Endpoint | ROLE_USER | ROLE_ADMIN |
|---|---|---|
| `POST /api/rag/upload` | ✅ | ✅ |
| `POST /api/rag/ask` | ✅ | ✅ |
| `GET /api/rag/documents` | ✅ | ✅ |
| `GET /api/rag/documents/{id}` | ✅ | ✅ |
| `GET /api/rag/health` | ✅ | ✅ |
| `DELETE /api/rag/documents/{id}` | ❌ 403 | ✅ |
| `DELETE /api/rag/clear` | ❌ 403 | ✅ |
| `POST /api/auth/register` | public | public |
| `POST /api/auth/login` | public | public |

---

## Using the Token

### Postman
1. Call `POST /api/auth/login` to get the token
2. In your request → **Authorization tab** → select **Bearer Token**
3. Paste the token value

### curl
```bash
curl -X POST "http://localhost:8080/api/rag/ask?question=what+is+heart" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Swagger UI
1. Open `http://localhost:8080/api/swagger-ui/index.html`
2. Call `POST /auth/register` or `POST /auth/login`
3. Copy the `token` from the response
4. Click the **Authorize 🔒** button at the top
5. Paste the token (no `Bearer` prefix needed in Swagger)
6. Click **Authorize** — all endpoints now work

---

## Database Schema

The `users` table is auto-created by Hibernate in the same `rag_db` PostgreSQL database:

```sql
CREATE TABLE users (
    id          VARCHAR(255) PRIMARY KEY,   -- UUID
    username    VARCHAR(255) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,       -- BCrypt hash
    role        VARCHAR(50)  NOT NULL,       -- ROLE_USER or ROLE_ADMIN
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP
);
```

---

## Configuration

In `application.properties`:

```properties
# JWT Secret (Base64 encoded, min 256-bit for HS256)
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Token expiry in milliseconds (86400000 = 24 hours)
jwt.expiration=86400000
```

> **Important:** Change `jwt.secret` to a strong random value before deploying to production.
> Generate one with: `openssl rand -base64 32`

---

## Code Walkthrough

### `JwtService.java`
Handles all JWT operations:
- `generateToken(userDetails)` — creates a signed JWT with username as subject, 24h expiry
- `isTokenValid(token, userDetails)` — checks signature and expiry
- `extractUsername(token)` — parses the subject claim

### `JwtAuthenticationFilter.java`
Runs on every HTTP request:
1. Reads the `Authorization` header
2. Extracts the token after `Bearer `
3. Validates it using `JwtService`
4. If valid, sets the authentication in `SecurityContextHolder`
5. If invalid or missing, passes through — Spring Security handles the 401/403

### `SecurityConfig.java`
Defines the security rules:
- CSRF disabled (stateless REST API, no cookies)
- Session policy: `STATELESS` — no server-side sessions
- Public routes: `/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- Admin-only: `DELETE /rag/clear`, `DELETE /rag/documents/**`
- Everything else under `/rag/**` requires `ROLE_USER` or `ROLE_ADMIN`

### `AuthService.java`
- `register()` — validates uniqueness, BCrypt-hashes password, saves user, returns JWT
- `login()` — delegates to `AuthenticationManager`, loads user, returns JWT

### `User.java`
JPA entity with:
- UUID primary key (auto-generated)
- `Role` enum: `ROLE_USER`, `ROLE_ADMIN`
- `enabled` flag for account activation/deactivation
- `createdAt` auto-set on persist

---

## Error Responses

| Scenario | HTTP Code | Message |
|---|---|---|
| No token provided | 403 | Forbidden |
| Invalid/expired token | 403 | Forbidden |
| Wrong role (user tries admin endpoint) | 403 | Forbidden |
| Wrong password on login | 401 | Unauthorized |
| Username already taken | 400 | Username already taken: john |
| Email already registered | 400 | Email already registered: john@example.com |

---

## Security Best Practices Applied

- Passwords are **never stored in plain text** — BCrypt with strength 10
- JWT is **signed with HMAC-SHA256** — tamper-proof
- Sessions are **stateless** — no server memory used per user
- CSRF is **disabled** — appropriate for stateless REST APIs
- Token expiry is **enforced** — expired tokens are rejected
- Role checks are **enforced at the filter chain level** — not just controller level
