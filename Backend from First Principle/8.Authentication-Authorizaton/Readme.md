# Authentication & Authorization — Complete Guide for Backend Engineers

## I. The Two Fundamental Questions

Every secured system must answer two separate questions — and engineers who conflate them introduce serious security vulnerabilities.

```
Authentication (AuthN):    "Who are you?"
                           Verify the identity of the subject.
                           → Login, token validation, biometrics

Authorization (AuthZ):     "What can you do?"
                           Determine what the verified identity is permitted to do.
                           → Role checks, permission gates, access control
```

```
Real-world example:

  You arrive at an office building.

  Security desk checks your ID card.         ← Authentication
  "Yes, this is Alice. Identity confirmed."

  Security desk checks the access list.      ← Authorization
  "Alice is allowed on floors 1–3 only.
   The server room on floor 7 is off-limits."

  These are two separate checks.
  Passing one does not imply passing the other.
```

**The critical distinction in code:**

```
AuthN failure → 401 Unauthorized
  "I don't know who you are. Please identify yourself."
  → Missing token, expired token, invalid credentials

AuthZ failure → 403 Forbidden
  "I know exactly who you are. You just can't do this."
  → Valid token, but insufficient role or permission
  → Re-authenticating will not help
```

---

## II. The Evolution of Identity Verification

Understanding why modern auth systems are designed the way they are requires tracing the history of the problems they solved.

```
Era               Method              Core Principle
────              ──────              ──────────────
Pre-Industrial    Human recognition   Something you ARE (known face, trusted person)
Medieval          Wax seals           Something you HAVE (physical token of possession)
Industrial        Passphrases         Something you KNOW (shared secret)
Digital (1960s)   Passwords + hashing Something you KNOW, stored safely
Modern (1990s+)   MFA                 Two or more of the above, combined
```

---

### Pre-Industrial: Recognition and Trust

```
Village elder vouches for a stranger.
The innkeeper recognizes a regular customer.
Identity was verified by human memory and social trust networks.

Limitation: Doesn't scale beyond a community you personally know.
            Anyone outside the network is unverifiable.
```

---

### Medieval: Wax Seals — Physical Tokens

```
A royal decree is sealed with the king's unique wax seal.
Anyone can verify the seal's pattern → trust the document's authenticity.

Core principle: Something you HAVE.
  You possess the seal → you are authorized to issue decrees.

Modern equivalent: Hardware security keys (YubiKey),
                   smart cards, physical access badges.

Limitation: If the seal is stolen, the attacker has full authority.
            Possession alone is not enough for high-stakes systems.
```

---

### Industrial Era: Passphrases — Something You Know

```
Telegraph operators used rotating passphrases:
  Operator A: "What is the weather in London?"
  Operator B: "The fog is thick tonight."      ← correct passphrase → trusted ✅
  Attacker:   "The sun is bright."             ← wrong → rejected ❌

Core principle: Something you KNOW (shared secret).

Modern equivalent: Passwords.

Limitation: Secrets can be intercepted, forgotten, shared, or guessed.
```

---

### Digital Era (1960s): Passwords and Hashing

The first multi-user mainframes introduced the modern password — and immediately created a new problem: how do you store passwords safely?

```
❌ Storing plain text passwords (what early systems did):
  Database: { username: "alice", password: "hunter2" }
  DB breach → attacker has every user's password ❌

❌ Storing encrypted passwords:
  Database: { username: "alice", password: encrypt("hunter2") }
  If the encryption key leaks → all passwords decrypted ❌

✅ Storing hashed passwords (the correct approach):
  Database: { username: "alice", passwordHash: bcrypt("hunter2" + salt) }

  Properties of a cryptographic hash:
    ├── One-way: hash("hunter2") → "a3f9c21d..." but not reversible
    ├── Deterministic: same input always produces same output
    ├── Collision-resistant: different inputs produce different hashes
    └── Avalanche effect: "hunter2" and "hunter3" produce completely different hashes
```

**Why salting is essential:**

```
Without salt:
  hash("password123") → "482c811da5d5b4bc6d497ffa98491e38"

  Two users with the same password → same hash in the DB.
  Attacker builds a Rainbow Table (precomputed hash→password lookup):
    "482c811..." → "password123" ← instantly cracked ❌

With salt:
  salt_alice = random_bytes(32)    → "a7f3..."
  salt_bob   = random_bytes(32)    → "9c21..."

  hash("password123" + "a7f3...") → "7bc4e..."   (Alice)
  hash("password123" + "9c21...") → "2a8f1..."   (Bob)

  Same password → completely different hashes in DB.
  Rainbow tables are useless. Each must be brute-forced individually. ✅
```

---

### Modern Era (1990s+): Multi-Factor Authentication (MFA)

No single factor is sufficient for high-security systems. MFA combines two or more independent factors from different categories.

```
Factor Categories:

  Something you KNOW:      Password, PIN, security question answer
  Something you HAVE:      Phone (TOTP app), hardware key (YubiKey), smart card
  Something you ARE:       Fingerprint, face ID, iris scan (biometrics)

MFA requires at least two categories:
  Password (KNOW) + phone TOTP code (HAVE)   → 2FA ✅
  Password (KNOW) + fingerprint (ARE)         → 2FA ✅
  Password (KNOW) + PIN (KNOW)               → NOT MFA (same category) ❌

Why MFA works:
  Attacker steals your password (KNOW) ← compromised
  Attacker still needs your phone (HAVE) ← they don't have it ✅
  Two independent factors = two independent security layers
```

---

## III. Stateful vs. Stateless Authentication

Once identity is verified, the server needs a way to remember that verification across subsequent requests — without requiring the user to log in on every click. There are two fundamentally different approaches.

---

### Stateful Authentication (Session-Based)

The server stores session state. The client holds only a reference (session ID) to that state.

```
Login:
  Client ──POST /login { username, password }──────────────────► Server
                                                                    │
                                                              verify credentials
                                                                    │
                                                         create session in Redis:
                                                         { sessionId: "abc123",
                                                           userId: 42,
                                                           role: "admin",
                                                           expiresAt: "..." }
  Client ◄──Set-Cookie: sessionId=abc123; HttpOnly; Secure──────────│

Subsequent requests:
  Client ──GET /api/orders (Cookie: sessionId=abc123)─────────────► Server
                                                                    │
                                                          lookup Redis["abc123"]
                                                          → { userId: 42, role: "admin" }
                                                                    │
  Client ◄──200 OK { orders: [...] }──────────────────────────────── │

Logout / revocation:
  DELETE Redis["abc123"]   → session instantly invalid ✅
  User is logged out immediately, across all devices. ✅
```

**Cookie security flags:**

```
Set-Cookie: sessionId=abc123; HttpOnly; Secure; SameSite=Strict

HttpOnly:         JavaScript cannot read the cookie (prevents XSS theft)
Secure:           Cookie only sent over HTTPS (never plain HTTP)
SameSite=Strict:  Cookie not sent on cross-site requests (prevents CSRF)
```

---

### Stateless Authentication (JWT-Based)

The server stores nothing. The token itself contains all the information, cryptographically signed.

```
Login:
  Client ──POST /login { username, password }──────────────────► Server
                                                                    │
                                                              verify credentials
                                                                    │
                                                         create JWT (signed, not stored):
                                                         header.payload.signature
  Client ◄──{ token: "eyJ..." }───────────────────────────────────── │
  (Client stores in memory or localStorage)

Subsequent requests:
  Client ──GET /api/orders (Authorization: Bearer eyJ...)──────────► Server
                                                                    │
                                                         verify signature
                                                         (no DB/Redis lookup)
                                                         decode payload:
                                                         { userId: 42, role: "admin" }
                                                                    │
  Client ◄──200 OK { orders: [...] }──────────────────────────────── │
```

**JWT structure:**

```
eyJhbGciOiJIUzI1NiJ9 . eyJ1c2VySWQiOjQyLCJyb2xlIjoiYWRtaW4ifQ . SflKxwRJSMeKKF2QT
────────────────────   ─────────────────────────────────────────   ──────────────────
      Header                          Payload                           Signature
  (Base64 encoded)               (Base64 encoded)                  (HMAC-SHA256)

Decoded header:                Decoded payload:
{                              {
  "alg": "HS256"                 "userId": 42,
}                                "role": "admin",
                                 "iat": 1705312200,   ← issued at
                                 "exp": 1705398600    ← expires at
                               }

Signature = HMAC_SHA256(header + "." + payload, SECRET_KEY)

If anyone tampers with the payload → signature verification fails → rejected ✅
```

> **Important:** JWT payloads are Base64-encoded, not encrypted. Anyone can decode and read the payload. Never store sensitive data (passwords, secrets, PII) in a JWT. Store only what the server needs to identify and authorize the user.

---

### The Revocation Problem with JWTs

```
Scenario: Admin revokes a compromised user's access.

Stateful (session):
  DELETE Redis[sessionId]   → immediate effect ✅
  Next request → session not found → 401 ✅

Stateless (JWT):
  Token is valid for 24 hours.
  No server-side record to delete.
  User continues making valid requests for up to 24 hours ❌

Solutions (each with tradeoffs):
  Short expiry (15 min):   Limits damage window. Requires refresh tokens. ✅
  Token blocklist (Redis):  Store revoked token IDs. Reintroduces statefulness. ⚠️
  Refresh token rotation:  Revoke refresh token. User must re-login at next refresh. ✅
```

---

### Stateful vs. Stateless — Side by Side

| | Stateful (Session) | Stateless (JWT) |
|---|---|---|
| Server storage | Required (Redis / DB) | None |
| Scalability | Requires shared session store | Trivially horizontal ✅ |
| Revocation | Instant (delete session) ✅ | Complex (blocklist or wait for expiry) |
| Mobile app support | Limited (cookies not native) | ✅ Native (Authorization header) |
| Microservices | Requires shared session store | ✅ Each service validates independently |
| Token size | Small (just an ID) | Larger (full payload) |
| Security on compromise | Revoke immediately | Wait for expiry or use blocklist |
| Best for | Web browsers, high-security apps | APIs, mobile, microservices |

---

### The Hybrid Approach

Most production systems at scale use both strategies simultaneously:

```
Web browser users:
  └── Stateful sessions (cookies)
      → Easy revocation, native browser support, CSRF protection via SameSite

Mobile app users and third-party API consumers:
  └── Stateless JWT tokens (Authorization header)
      → No cookie dependency, works across domains, horizontally scalable

Result: Best security properties for each client type. ✅
```

---

## IV. Modern Protocols: OAuth 2.0 and OpenID Connect

---

### The Delegation Problem

```
Problem:
  You use App A (e.g., a calendar app).
  App A wants to access your Google Contacts to auto-fill attendees.

  ❌ Old approach: Give App A your Google password.
     → App A now has full access to your entire Google account.
     → If App A is breached, your Google account is compromised.
     → You can't revoke App A's access without changing your password.

  ✅ OAuth 2.0: App A gets a limited-scope token.
     → Token grants access to Contacts only (not Gmail, not Drive).
     → Token expires after a short window.
     → You can revoke App A's access from Google's settings at any time,
        without changing your password.
```

---

### OAuth 2.0 — The Authorization Framework

OAuth 2.0 solves the delegation problem by introducing four distinct roles:

```
Resource Owner:       You — the user who owns the data
Client:               App A — the application wanting access
Authorization Server: Google's auth server — issues tokens
Resource Server:      Google Contacts API — holds the data
```

**The OAuth 2.0 Authorization Code Flow:**

```
You (browser)          App A (Client)          Google Auth Server       Google Contacts API
─────────────          ──────────────          ─────────────────────    ───────────────────
Click "Connect
Google Contacts"
      │
      │──────────────► Redirect to Google ──────────────────────────►
                       with:
                       client_id=APP_A
                       scope=contacts.read
                       redirect_uri=app-a.com/callback
                       state=random_csrf_token

                                               Show consent screen ──────────────────────►
                                                                    "App A wants to read
                                                                     your contacts. Allow?"
◄────────────────────────────────────────────── You click "Allow"
      │
      │ Redirect to app-a.com/callback
      │ ?code=AUTH_CODE&state=random_csrf_token
      │
      ▼
App A backend:
  Verify state token (CSRF protection)
  POST /token
  { code: AUTH_CODE,
    client_secret: APP_A_SECRET }
                                               Verify code + secret
                                               Issue tokens:
                                               { access_token: "abc...",   ← short-lived (1hr)
                                                 refresh_token: "xyz..." } ← long-lived
      │
      │── GET /contacts ──────────────────────────────────────────────────────────────────►
      │   Authorization: Bearer abc...                                                     │
      │                                                                                    │ validate token
      │◄─── { contacts: [...] } ──────────────────────────────────────────────────────────│
```

**Scopes — granular access control:**

```
scope=contacts.read          → read contacts only
scope=contacts.read,calendar → read contacts AND calendar
scope=gmail                  → full Gmail access (dangerous — avoid requesting this)

The user sees exactly what permissions they are granting.
The token is strictly limited to those scopes. Nothing more.
```

---

### OpenID Connect (OIDC) — Authentication on Top of OAuth

OAuth 2.0 handles authorization (what can App A access?). It does not define a standard way to verify *who the user is*.

OpenID Connect extends OAuth 2.0 with a second token specifically for identity:

```
OAuth 2.0 tokens:
  access_token:   "What can this app access?" → used to call APIs
  refresh_token:  "Get a new access token without re-login"

OIDC adds:
  id_token:       "Who is the user?" → a JWT containing user identity

Decoded id_token payload:
{
  "sub": "google-user-id-12345",   ← subject (stable user identifier)
  "email": "alice@gmail.com",
  "name": "Alice Chen",
  "picture": "https://...",
  "iss": "https://accounts.google.com",  ← issuer (who signed this)
  "aud": "app-a-client-id",              ← audience (intended recipient)
  "exp": 1705398600                      ← expiry
}

Your server:
  1. Verify id_token signature against Google's public keys ✅
  2. Check iss = "https://accounts.google.com" ✅
  3. Check aud = your client_id ✅
  4. Extract sub → this is your user's stable identifier ✅
  5. Create or look up the user account in your DB ✅
```

> **OAuth 2.0 vs. OIDC in one sentence:** OAuth 2.0 tells App A what it can access on your behalf. OIDC tells App A *who you are*.

---

## V. Authorization Patterns: Role-Based Access Control (RBAC)

Once a user is authenticated, the server must determine what they are permitted to do. RBAC is the standard industry pattern.

---

### The RBAC Model

```
Users are assigned Roles.
Roles are assigned Permissions.
Permissions are checked before actions are executed.

Users ──────► Roles ──────► Permissions
  Alice          Admin          read, write, delete
  Bob            Moderator      read, write
  Charlie        User           read
```

**Concrete example — content platform:**

```
Roles and their permissions:

  User:
    ├── read:posts    ✅
    ├── write:posts   ✅ (their own only)
    ├── delete:posts  ❌
    └── manage:users  ❌

  Moderator:
    ├── read:posts    ✅
    ├── write:posts   ✅
    ├── delete:posts  ✅ (any post)
    └── manage:users  ❌

  Admin:
    ├── read:posts    ✅
    ├── write:posts   ✅
    ├── delete:posts  ✅
    └── manage:users  ✅
```

---

### Implementing RBAC as Middleware

```
Request arrives:
  DELETE /api/posts/789
  Authorization: Bearer eyJ...
       │
       ▼
[Auth Middleware]
  → Verify JWT signature ✅
  → Extract: { userId: 42, role: "user" }
       │
       ▼
[RBAC Middleware: requirePermission("delete:posts")]
  → Check: does role "user" have "delete:posts"?
  → No ❌
  → Return 403 Forbidden
  → Handler never reached ✅

  If role = "moderator" or "admin":
  → Yes ✅
  → Pass to handler ✅
```

```java
// Spring Boot — RBAC with method-level security
@PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
@DeleteMapping("/posts/{id}")
public ResponseEntity<Void> deletePost(@PathVariable Long id) {
    postService.delete(id);
    return ResponseEntity.noContent().build();
}

@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public ResponseEntity<List<User>> listAllUsers() {
    return ResponseEntity.ok(userService.findAll());
}
```

---

## VI. Critical Security Pitfalls

---

### Pitfall 1: Specific Error Messages — Information Leakage

```
❌ Specific errors (dangerous):
  POST /login { username: "alice", password: "wrong" }
  → 401: "Incorrect password"       ← attacker knows "alice" is a valid username

  POST /login { username: "ghost", password: "anything" }
  → 401: "User not found"           ← attacker knows "ghost" is NOT registered

  With two requests, an attacker can enumerate all valid usernames in your system.
  Valid usernames → targeted phishing, credential stuffing attacks.

✅ Generic error (correct):
  POST /login { username: "alice", password: "wrong" }
  → 401: "Authentication failed"    ← reveals nothing ✅

  POST /login { username: "ghost", password: "anything" }
  → 401: "Authentication failed"    ← same response, same information ✅

Rule: Failed authentication should always return the identical
      response regardless of whether the username, password,
      or both were incorrect.
```

---

### Pitfall 2: Timing Attacks — Leaking Information Through Response Time

Even with generic error messages, an attacker can extract information by measuring *how long* the server takes to respond.

```
Attacker sends:
  POST /login { username: "alice@example.com", password: "x" }
  Response time: 180ms   ← long (password hashing happened → user exists!)

  POST /login { username: "nobody@fake.com", password: "x" }
  Response time: 2ms     ← instant (user not found, no hashing needed → user doesn't exist!)

The response time reveals whether a username is registered —
even if the error message is identical in both cases.
```

**Why the timing difference occurs:**

```
Normal login flow:
  1. Find user by email                     ~2ms
  2. Hash incoming password (bcrypt)        ~150ms  ← intentionally slow
  3. Compare hash to stored hash            ~1ms
  Total: ~153ms

User not found flow:
  1. Find user by email → not found         ~2ms
  2. Return immediately                     (no hashing)
  Total: ~2ms

The 150ms difference is the timing oracle.
```

**Defense — constant time operations:**

```java
// Always hash, even when user is not found
public AuthResult login(String email, String password) {
    Optional<User> user = userRepository.findByEmail(email);

    // If user not found, hash against a dummy hash anyway
    // to ensure constant time execution
    String hashToCompare = user
        .map(User::getPasswordHash)
        .orElse(DUMMY_HASH);  // pre-computed bcrypt hash of a dummy password

    boolean passwordMatches = BCrypt.checkpw(password, hashToCompare);

    // Only succeed if BOTH user exists AND password matches
    if (user.isEmpty() || !passwordMatches) {
        return AuthResult.failure("Authentication failed");
    }

    return AuthResult.success(user.get());
}
```

```
With constant-time defense:
  Valid user, wrong password:    ~153ms
  Invalid user:                  ~153ms  ← same time (dummy hash computed) ✅

Timing oracle eliminated. Attacker learns nothing from response time. ✅
```

---

### Pitfall 3: Storing Passwords Incorrectly

```
❌ Never:   Store plain text passwords
❌ Never:   Store encrypted passwords (reversible)
❌ Never:   Use fast hashes (MD5, SHA-1, SHA-256) for passwords
            → Fast hashes can be brute-forced at billions/second on GPUs

✅ Always:  Use purpose-built password hashing algorithms:
            bcrypt    → adaptive cost factor, built-in salting
            Argon2id  → winner of Password Hashing Competition (PHC), recommended
            scrypt    → memory-hard, resistant to ASIC attacks

These are intentionally slow (~100-300ms) to make brute-force impractical.
```

---

## VII. Quick Reference Checklist

| Concern | Mechanism |
|---|---|
| Identity verification | Authentication (AuthN) |
| Permission enforcement | Authorization (AuthZ) |
| AuthN failure response | `401 Unauthorized` |
| AuthZ failure response | `403 Forbidden` |
| Store passwords safely | Argon2id or bcrypt with salt — never plain text or MD5/SHA |
| Session management (web) | Stateful sessions with `HttpOnly; Secure; SameSite=Strict` cookies |
| Session management (mobile/API) | Stateless JWT with short expiry + refresh token rotation |
| Instant session revocation | Stateful session (delete from Redis) |
| Third-party access delegation | OAuth 2.0 Authorization Code Flow |
| "Login with Google/GitHub" | OpenID Connect (OIDC) — verify `id_token` |
| Role-based access control | RBAC — users → roles → permissions |
| Prevent username enumeration | Always return generic "Authentication failed" |
| Prevent timing attacks | Always hash (including for non-existent users) — constant time |
| Limit token blast radius | Short-lived access tokens (15min) + long-lived refresh tokens |
| Revoke JWT before expiry | Token blocklist in Redis + short expiry as backup |
| Protect against CSRF | `SameSite=Strict` cookie flag for session-based auth |
| Protect against XSS token theft | `HttpOnly` cookie flag — never store tokens in localStorage |