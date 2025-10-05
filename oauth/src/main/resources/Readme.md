# OAuth 2.0 Spring Boot Demo Application

A complete, beginner-friendly OAuth 2.0 implementation using Spring Boot 3.2. This project demonstrates the Authorization Code flow with JWT tokens in a single application that acts as both Authorization Server and Resource Server.

## Table of Contents

- [Understanding OAuth 2.0](#understanding-oauth-20)
- [Real World Analogy](#real-world-analogy)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Application](#running-the-application)
- [Testing the OAuth Flow](#testing-the-oauth-flow)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Technical Details](#technical-details)
- [Troubleshooting](#troubleshooting)

## Understanding OAuth 2.0

OAuth 2.0 is an authorization framework that enables applications to obtain limited access to user accounts. Instead of sharing your password with third-party applications, OAuth allows you to grant them access through tokens.

### Why OAuth 2.0?

1. **Security**: Your password never leaves your control
2. **Limited Access**: Tokens only provide specific permissions (scopes)
3. **Expiration**: Tokens automatically expire, reducing risk
4. **Revocable**: You can cancel access without changing your password

## Real World Analogy

Think of OAuth 2.0 like a hotel key card system:

### The Characters

- **You** = The User
- **Hotel Front Desk** = Authorization Server
- **Hotel Room, Gym, Pool** = Protected Resources (API endpoints)
- **Your Key Card** = Access Token

### The Process

**Step 1: Check-in at Front Desk**
- You arrive and request access to your room
- This is like clicking "Start OAuth Flow"

**Step 2: Prove Your Identity**
- You show your ID (login with username/password)
- Front desk verifies you're a registered guest

**Step 3: Specify Access Needs**
- Clerk asks: "What areas do you need? Room? Gym? Pool?"
- You approve the access (consent to scopes)

**Step 4: Receive Temporary Voucher**
- Clerk gives you a paper slip with a number
- This is the Authorization Code - useless to others, expires quickly

**Step 5: Exchange for Key Card**
- You take the voucher to the key card machine
- Provide proof you're the real guest (client credentials)
- Receive an actual Key Card (Access Token)

**Step 6: Use Your Key Card**
- Swipe at room door, gym, pool
- This is using the token to access protected endpoints

**Step 7: Key Card Expires**
- After checkout time (30 minutes), card stops working
- Need to get a new one (refresh or re-authenticate)

## Project Structure

```
oauth-demo/
├── pom.xml
├── README.md
├── src/main/java/com/example/oauth/
│   ├── OAuthApplication.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── ApiController.java
│   │   └── OAuthCallbackController.java
│   └── model/
│       └── User.java
└── src/main/resources/
    ├── application.yml
    └── static/
        └── index.html
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)
- cURL or Postman for testing API endpoints

## Installation

### Step 1: Clone or Create Project

Create the project directory structure:

```bash
mkdir -p oauth-demo/src/main/java/com/example/oauth/{config,controller,model}
mkdir -p oauth-demo/src/main/resources/static
cd oauth-demo
```

### Step 2: Copy Project Files

Copy all the files from the artifact into their respective locations as shown in the project structure above.

### Step 3: Build the Project

```bash
mvn clean install
```

## Running the Application

### Start the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Verify Startup

You should see logs indicating:

```
Started OAuthApplication in X.XXX seconds
```

### Access the Web Interface

Open your browser and navigate to:

```
http://localhost:8080
```

You will see the interactive OAuth 2.0 demo homepage.

## Testing the OAuth Flow

### Test Credentials

**User Accounts:**
- Username: `user` / Password: `password` (USER role)
- Username: `admin` / Password: `admin` (ADMIN role)

**OAuth Client:**
- Client ID: `demo-client`
- Client Secret: `secret`

### Method 1: Using the Web Interface

1. Open `http://localhost:8080`
2. Click the "Start OAuth Flow" button
3. Login with `user` / `password`
4. Approve the requested scopes (read, write)
5. You'll be redirected to `/authorized` with your authorization code displayed
6. Copy the pre-filled cURL command
7. Run it in your terminal to get an access token

### Method 2: Manual Step-by-Step

**Step 1: Get Authorization Code**

Open this URL in your browser:

```
http://localhost:8080/oauth2/authorize?response_type=code&client_id=demo-client&redirect_uri=http://localhost:8080/authorized&scope=read%20write
```

After login and consent, you'll be redirected to a URL like:

```
http://localhost:8080/authorized?code=AUTHORIZATION_CODE_HERE
```

**Step 2: Exchange Code for Access Token**

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u demo-client:secret \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=YOUR_AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:8080/authorized"
```

Response:

```json
{
  "access_token": "eyJraWQiOiI5ZjU2Y2M3Zi0...",
  "refresh_token": "l5tPcW1KvP7OWD...",
  "scope": "read write",
  "token_type": "Bearer",
  "expires_in": 1799
}
```

**Step 3: Use Access Token**

```bash
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

Response:

```json
{
  "message": "User profile endpoint - requires authentication with 'read' scope",
  "username": "user",
  "scopes": "read write",
  "issuedAt": "2024-01-15T10:30:00Z",
  "expiresAt": "2024-01-15T11:00:00Z"
}
```

### Method 3: Using Postman

**Get Token:**

1. Create new POST request: `http://localhost:8080/oauth2/token`
2. Authorization tab: Select "Basic Auth"
    - Username: `demo-client`
    - Password: `secret`
3. Body tab: Select "x-www-form-urlencoded"
    - `grant_type`: `authorization_code`
    - `code`: (paste authorization code)
    - `redirect_uri`: `http://localhost:8080/authorized`
4. Click Send

**Use Token:**

1. Create new GET request: `http://localhost:8080/api/user/profile`
2. Authorization tab: Select "Bearer Token"
    - Token: (paste access token from previous response)
3. Click Send

## API Endpoints

### Public Endpoints (No Authentication Required)

**GET /api/public/hello**
```bash
curl http://localhost:8080/api/public/hello
```

Response:
```json
{
  "message": "Welcome! This is a public endpoint - no authentication required",
  "timestamp": "2024-01-15T10:30:00"
}
```

**GET /api/public/info**
```bash
curl http://localhost:8080/api/public/info
```

### User Endpoints (Requires 'read' scope)

**GET /api/user/profile**

Returns the authenticated user's profile information.

**GET /api/user/data**

Returns user-specific data.

```bash
curl http://localhost:8080/api/user/data \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Admin Endpoints (Requires 'write' scope)

**GET /api/admin/dashboard**

Returns admin dashboard statistics.

**POST /api/admin/action**

Execute admin actions.

```bash
curl -X POST http://localhost:8080/api/admin/action \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action": "refresh_cache"}'
```

### OAuth 2.0 Server Endpoints

- **Authorization Endpoint**: `/oauth2/authorize`
- **Token Endpoint**: `/oauth2/token`
- **JWK Set Endpoint**: `/oauth2/jwks`
- **Token Introspection**: `/oauth2/introspect`
- **Token Revocation**: `/oauth2/revoke`

## Configuration

### Application Properties (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: oauth-demo
  security:
    oauth2:
      authorizationserver:
        issuer: http://localhost:8080

logging:
  level:
    org.springframework.security: INFO
    org.springframework.security.oauth2: DEBUG
```

### OAuth Client Configuration

The registered OAuth client is configured in `SecurityConfig.java`:

- **Client ID**: demo-client
- **Client Secret**: secret (BCrypt encoded)
- **Grant Types**: Authorization Code, Refresh Token, Client Credentials
- **Redirect URIs**:
    - `http://localhost:8080/login/oauth2/code/demo-client`
    - `http://localhost:8080/authorized`
- **Scopes**: openid, profile, read, write
- **Token Settings**:
    - Access Token TTL: 30 minutes
    - Refresh Token TTL: 24 hours

### User Configuration

In-memory users are configured in `SecurityConfig.java`:

```java
User: user/password - Role: USER
Admin: admin/admin - Roles: USER, ADMIN
```

## Technical Details

### OAuth 2.0 Flow Explained

**1. Authorization Request**

```
Client redirects user to:
  /oauth2/authorize?
    response_type=code&
    client_id=demo-client&
    redirect_uri=http://localhost:8080/authorized&
    scope=read write
```

**2. User Authentication**

- User logs in with username/password
- Spring Security validates credentials

**3. User Consent**

- Authorization server shows consent screen
- User approves requested scopes

**4. Authorization Code Issued**

```
User redirected to:
  http://localhost:8080/authorized?code=AUTHORIZATION_CODE
```

**5. Token Exchange**

```
Client sends POST to /oauth2/token:
  - Authorization: Basic base64(client_id:client_secret)
  - grant_type=authorization_code
  - code=AUTHORIZATION_CODE
  - redirect_uri=http://localhost:8080/authorized
```

**6. Access Token Issued**

```json
{
  "access_token": "JWT_TOKEN",
  "token_type": "Bearer",
  "expires_in": 1800,
  "refresh_token": "REFRESH_TOKEN",
  "scope": "read write"
}
```

**7. Resource Access**

```
Client sends request with:
  Authorization: Bearer JWT_TOKEN
```

### JWT Token Structure

The access token is a JWT with three parts:

**Header:**
```json
{
  "alg": "RS256",
  "kid": "key-id"
}
```

**Payload:**
```json
{
  "sub": "user",
  "aud": "demo-client",
  "scope": "read write",
  "iss": "http://localhost:8080",
  "exp": 1705318200,
  "iat": 1705316400
}
```

**Signature:**
- Signed with RSA private key
- Verified with RSA public key from /oauth2/jwks

### Security Features

1. **RSA Key Pair**: 2048-bit keys generated at startup
2. **BCrypt Password Encoding**: All passwords hashed with BCrypt
3. **JWT Validation**: Tokens validated using JWK Set
4. **Scope-Based Authorization**: Fine-grained access control
5. **CSRF Protection**: Enabled by default
6. **Secure Headers**: X-Frame-Options, X-Content-Type-Options, etc.

## Troubleshooting

### Common Issues

**Issue 1: Port 8080 already in use**

Solution: Change the port in `application.yml`:

```yaml
server:
  port: 8081
```

Remember to update redirect URIs accordingly.

**Issue 2: Authorization code expired**

Authorization codes expire after 5 minutes (default). Get a fresh code by starting the OAuth flow again.

**Issue 3: Invalid token error**

Access tokens expire after 30 minutes. Either:
- Get a new token using the authorization flow
- Use the refresh token to get a new access token

**Issue 4: 401 Unauthorized on protected endpoints**

Ensure:
- Token is included in the Authorization header
- Format is: `Authorization: Bearer YOUR_TOKEN`
- Token hasn't expired
- Token has the required scope

**Issue 5: 403 Forbidden**

Your token is valid but lacks the required scope:
- User endpoints need 'read' scope
- Admin endpoints need 'write' scope

### Debug Logging

Enable detailed OAuth logging in `application.yml`:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: TRACE
```

### Testing Token Validity

Use the introspection endpoint:

```bash
curl -X POST http://localhost:8080/oauth2/introspect \
  -u demo-client:secret \
  -d "token=YOUR_ACCESS_TOKEN"
```

## Key Concepts Summary

| Term | Simple Explanation | 
|------|-------------------|
| Authorization Code | Temporary voucher proving you logged in successfully |
| Access Token | The actual key to access protected resources |
| Refresh Token | A token to get new access tokens without logging in again |
| Client ID/Secret | Application's credentials to prove it's legitimate |
| Scopes | Specific permissions granted (read, write, etc.) |
| Resource Server | The API providing protected resources |
| Authorization Server | Where you login and obtain tokens |
| JWT | JSON Web Token - self-contained token with user info |
| Bearer Token | Token type sent in Authorization header |

## Extending the Application

### Add Database Storage

Replace in-memory storage with JPA:

1. Add dependencies: Spring Data JPA, H2/PostgreSQL
2. Create entity classes for users and clients
3. Implement UserDetailsService with database queries
4. Configure JPA repositories

### Add Custom Login Page

Create a custom login page instead of Spring's default:

1. Create `login.html` in `src/main/resources/templates`
2. Configure in SecurityConfig: `.formLogin(form -> form.loginPage("/login"))`

### Add More Scopes

Define custom scopes for fine-grained access:

```java
.scope("user:read")
.scope("user:write")
.scope("admin:read")
.scope("admin:write")
```

### Add Refresh Token Flow

Use refresh token to get new access token:

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u demo-client:secret \
  -d "grant_type=refresh_token" \
  -d "refresh_token=YOUR_REFRESH_TOKEN"
```

## Additional Resources

- [Spring Authorization Server Documentation](https://docs.spring.io/spring-authorization-server/docs/current/reference/html/)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [JWT.io - JWT Debugger](https://jwt.io/)
- [OAuth 2.0 Playground](https://www.oauth.com/playground/)

## License

This project is created for educational purposes and is free to use and modify.

## Support

For issues or questions:
1. Check the Troubleshooting section
2. Review Spring Security OAuth logs
3. Consult Spring Authorization Server documentation