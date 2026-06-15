# OAuth2 Authentication — Google & GitHub Login

This document covers the OAuth2 login integration added to the Enterprise RAG Wrapper API.
OAuth2 works **alongside** the existing JWT authentication — nothing was changed or removed.

---

## What is OAuth2?

OAuth2 is an authorization framework that lets users log in using their existing Google or GitHub account instead of creating a new username/password.

**Without OAuth2:**
```
User → fills registration form → username + password stored in DB → JWT issued
```

**With OAuth2:**
```
User → clicks "Login with Google" → Google verifies identity → JWT issued automatically
```

The end result is the same — a JWT token that works on all RAG endpoints.

---

## How It Works in This App

```
1. User opens browser and visits:
   http://localhost:8080/api/oauth2/authorization/google
                         (or /github)

2. Redirected to Google/GitHub login page

3. User logs in with their Google/GitHub account

4. Google/GitHub sends user info (email, name) back to our app

5. OAuth2UserService checks if user exists in DB:
   - First time → auto-creates user with ROLE_USER
   - Returning  → skips creation

6. OAuth2AuthenticationSuccessHandler issues a JWT token

7. User is redirected to:
   http://localhost:8080/api/oauth2/token?token=eyJhbGci...

8. User copies the token and uses it as Bearer token on all RAG API calls
```

---

## Project Structure

```
src/main/java/.../security/oauth2/
│
├── OAuth2UserService.java                  # Loads user from Google/GitHub, auto-creates in DB
├── OAuth2AuthenticationSuccessHandler.java # Issues JWT after successful OAuth2 login
└── OAuth2Controller.java                   # Returns JWT token as JSON after redirect
```

---

## New Files Added

### `OAuth2UserService.java`
- Extends Spring's `DefaultOAuth2UserService`
- Called after Google/GitHub successfully authenticates the user
- Extracts email and name from the provider's response
- If user doesn't exist in DB → creates them automatically with `ROLE_USER`
- Supports both Google and GitHub attribute formats

### `OAuth2AuthenticationSuccessHandler.java`
- Called when OAuth2 login succeeds
- Loads the user from DB by email
- Calls `JwtService.generateToken()` — the same service used by regular JWT login
- Redirects to `/api/oauth2/token?token=<jwt>` so the client can get the token

### `OAuth2Controller.java`
- `GET /api/oauth2/token?token=<jwt>` — returns the token as a JSON response
- `GET /api/oauth2/links` — shows login URLs for Google and GitHub

---

## API Endpoints

### Get Login Links
```
GET http://localhost:8080/api/oauth2/links
```
Response:
```json
{
  "google": "http://localhost:8080/api/oauth2/authorization/google",
  "github": "http://localhost:8080/api/oauth2/authorization/github",
  "instructions": "Open one of these URLs in your browser to login with Google or GitHub"
}
```

### Initiate Google Login
```
GET http://localhost:8080/api/oauth2/authorization/google
```
Open this URL in a browser — it redirects to Google login page.

### Initiate GitHub Login
```
GET http://localhost:8080/api/oauth2/authorization/github
```
Open this URL in a browser — it redirects to GitHub login page.

### Get Token After Login
```
GET http://localhost:8080/api/oauth2/token?token=<jwt>
```
Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "message": "OAuth2 login successful. Use this token in Authorization: Bearer <token> header.",
  "usage": "Add header: Authorization: Bearer eyJhbGci..."
}
```

---

## Setup Guide

### Google OAuth2 Setup

1. Go to [https://console.cloud.google.com/apis/credentials](https://console.cloud.google.com/apis/credentials)
2. Click **Create Credentials** → **OAuth 2.0 Client ID**
3. Application type: **Web application**
4. Add under **Authorized redirect URIs**:
   ```
   http://localhost:8080/api/login/oauth2/code/google
   ```
5. Copy the **Client ID** and **Client Secret**
6. Set environment variables before running the app:

```powershell
# Windows PowerShell
$env:GOOGLE_CLIENT_ID="your-client-id-here"
$env:GOOGLE_CLIENT_SECRET="your-client-secret-here"
mvn spring-boot:run
```

```bash
# Linux / Mac
export GOOGLE_CLIENT_ID=your-client-id-here
export GOOGLE_CLIENT_SECRET=your-client-secret-here
mvn spring-boot:run
```

---

### GitHub OAuth2 Setup

1. Go to [https://github.com/settings/developers](https://github.com/settings/developers)
2. Click **New OAuth App**
3. Fill in:
   - Application name: `RAG Wrapper API`
   - Homepage URL: `http://localhost:8080`
   - Authorization callback URL:
     ```
     http://localhost:8080/api/login/oauth2/code/github
     ```
4. Click **Register application**
5. Copy the **Client ID** and generate a **Client Secret**
6. Set environment variables:

```powershell
# Windows PowerShell
$env:GITHUB_CLIENT_ID="your-client-id-here"
$env:GITHUB_CLIENT_SECRET="your-client-secret-here"
```

---

## Configuration in application.properties

```properties
# OAuth2 — Google
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=email,profile

# OAuth2 — GitHub
spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}
spring.security.oauth2.client.registration.github.scope=user:email
```

> Credentials are read from environment variables — never hardcoded in the properties file.

---

## User Auto-Registration

When a user logs in via OAuth2 for the first time, they are automatically registered in the `users` table:

| Field | Value |
|---|---|
| `username` | Derived from their name (e.g. `johndoe`) |
| `email` | From Google/GitHub account |
| `password` | `OAUTH2_NO_PASSWORD` (can't be used for regular login) |
| `role` | `ROLE_USER` (default) |
| `enabled` | `true` |

On subsequent logins, the existing user is found by email and no new record is created.

---

## Using the Token

After OAuth2 login, copy the token from the redirect response and use it exactly like a regular JWT token:

### Postman
- Authorization tab → Bearer Token → paste token

### curl
```bash
curl -X POST "http://localhost:8080/api/rag/ask?question=what+is+heart" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Swagger UI
1. Login via browser: `http://localhost:8080/api/oauth2/authorization/google`
2. Copy token from redirect
3. Click **Authorize 🔒** in Swagger UI
4. Paste token → all endpoints work

---

## Coexistence with JWT Auth

Both authentication methods work side by side. Zero conflict.

| Method | Endpoint | How it works |
|---|---|---|
| Register | `POST /api/auth/register` | Username + password → JWT |
| Login | `POST /api/auth/login` | Username + password → JWT |
| Google Login | Browser → `/oauth2/authorization/google` | Google → JWT |
| GitHub Login | Browser → `/oauth2/authorization/github` | GitHub → JWT |

Once you have the JWT token (from any method), all RAG endpoints work the same way.

---

## Security Notes

- OAuth2 users cannot log in with username/password (their password is set to a placeholder)
- JWT tokens issued via OAuth2 have the same expiry (24 hours) as regular tokens
- Roles are assigned as `ROLE_USER` by default for OAuth2 users
- To promote an OAuth2 user to admin, update their role directly in the DB:
  ```sql
  UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'user@gmail.com';
  ```

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `redirect_uri_mismatch` | Callback URL not added in Google/GitHub console | Add exact redirect URI shown above |
| `401 Unauthorized` after login | Token not being sent | Add `Authorization: Bearer <token>` header |
| `GitHub email is null` | GitHub account has private email | Make email public in GitHub settings |
| `Invalid client_id` | Wrong env var | Double-check `GOOGLE_CLIENT_ID` / `GITHUB_CLIENT_ID` values |
| App starts without OAuth2 working | Client ID not set | Set env vars before `mvn spring-boot:run` |
