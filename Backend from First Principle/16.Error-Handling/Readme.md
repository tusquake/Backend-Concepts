# Backend Error Handling — Complete Guide

## I. The 5 Major Types of Backend Errors

Understanding where failures originate is the first step toward building a fault-tolerant system.

```
Incoming Request
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│                     Your Backend                            │
│                                                             │
│  ┌──────────────┐  ┌──────────┐  ┌────────────────────┐   │
│  │ Validation   │  │  Logic   │  │  Config / Env Vars  │   │
│  │ Layer        │  │  Layer   │  │  at startup         │   │
│  └──────────────┘  └──────────┘  └────────────────────┘   │
│         │                │                                  │
│         ▼                ▼                                  │
│  ┌──────────────┐  ┌──────────────────────────────────┐   │
│  │  Database    │  │  External Services                │   │
│  │  Layer       │  │  (Stripe, Auth0, Email)           │   │
│  └──────────────┘  └──────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

### 1. Logic Errors

The most **dangerous** type — the app keeps running but produces silently wrong results.

```
Example: Discount applied twice

  Cart total:   $100.00
  Apply 10%  →  $90.00   ✅ (first call)
  Apply 10%  →  $81.00   ❌ (called again due to a retry — user was charged wrong)

No exception thrown. No crash. Just wrong data quietly written to the database.
```

**Common causes:**

- Misunderstood business requirements
- Unhandled edge cases (empty lists, zero values, null fields)
- Incorrect conditional logic (`>=` vs `>`)
- Functions with side effects called more than once (non-idempotent operations)

> **Why they're hard to catch:** Logic errors pass all syntax checks and often pass basic tests. They only surface in specific data conditions or usage patterns — sometimes in production, weeks after deployment.

---

### 2. Database Errors

```
Failure spectrum:

  Low severity                                        High severity
       │                                                    │
       ▼                                                    ▼
  Connection pool      Constraint         Deadlock     Data
  exhausted            violation          detected     corruption
       │                    │                 │
       │               "email already    Two transactions
  Too many open        exists"            waiting on each
  connections →                           other forever →
  new queries          → 409 Conflict     one is rolled
  are rejected         response           back, retry
```

| Error Type | Cause | Handling Strategy |
|---|---|---|
| Connection pool exhausted | Too many concurrent requests | Queue requests, scale pool, add timeout |
| Unique constraint violation | Duplicate email, duplicate order | Catch and return `409 Conflict` |
| Deadlock | Two transactions block each other | Retry with backoff; one will succeed |
| Query timeout | Slow query, missing index | Optimize query, add index, set timeout |
| Foreign key violation | Referencing a deleted parent row | Validate existence before insert |

---

### 3. External Service Errors

Any call leaving your server is a potential failure point — network latency, provider outages, expired credentials, and rate limits are all outside your control.

```
Your Server ──── HTTP call ────► Stripe / Auth0 / SendGrid / S3
                                          │
                             ┌────────────┼────────────┐
                             ▼            ▼            ▼
                        Timeout      Rate limit     Outage
                        (30s+)       (429)          (503)
                             │            │            │
                        Transient    Transient    Non-transient
                        → retry      → backoff    → degrade gracefully
                        with jitter  + retry      or return cached data
```

**Common failure modes:**

| Failure | Cause | Signal |
|---|---|---|
| Network timeout | Slow provider, packet loss | No response within deadline |
| Expired token | OAuth token not refreshed | `401 Unauthorized` response |
| Rate limit hit | Too many requests in window | `429 Too Many Requests` |
| Provider outage | Their infrastructure down | `503 Service Unavailable` |
| Invalid payload | API version mismatch | `400 Bad Request` from provider |

---

### 4. Input Validation Errors

Caused by users (or attackers) sending malformed, missing, or malicious data. The **easiest error type to handle** — intercept at the entry point before any business logic runs.

```
Request enters your API
         │
         ▼
┌─────────────────────────┐
│   Validation Layer      │  ← Catch everything here, immediately
│                         │
│  ✓ Required fields      │
│  ✓ Correct types        │
│  ✓ Value ranges         │
│  ✓ String formats       │
│  ✓ Enum membership      │
└─────────────────────────┘
         │
    ┌────┴────┐
  Valid     Invalid
    │           │
    ▼           ▼
Continue    400 Bad Request
to handler  with field-level details ✅
            (never reaches business logic)
```

```json
// Good validation error response — field-specific, actionable
{
  "error": "Validation failed",
  "status": 400,
  "fields": {
    "email":    "Must be a valid email address",
    "price":    "Must be a positive number",
    "dueDate":  "Cannot be in the past"
  }
}
```

---

### 5. Configuration Errors

Missing or corrupt environment variables — typically introduced during deploys when a new required variable isn't added to the production environment.

```
❌ Discovered at runtime (dangerous):

  App starts fine
         │
  User triggers a payment
         │
  Code reaches: stripe.charge(STRIPE_SECRET_KEY, amount)
         │
  STRIPE_SECRET_KEY = undefined
         │
  Uncaught error — user sees a 500, charge never attempted
  (may have partially completed other operations first)

✅ Validated at startup (safe):

  App starts
         │
  Startup check: validate all required env vars
         │
  STRIPE_SECRET_KEY missing → app crashes immediately ✅
         │
  Deploy pipeline catches the crash
         │
  Engineer adds the missing variable → redeploy
  (no user is ever affected)
```

```
Required env var checklist at startup:

  DATABASE_URL       ✅ present
  REDIS_URL          ✅ present
  STRIPE_SECRET_KEY  ❌ missing → CRASH → fix before any traffic is served
  JWT_SECRET         ✅ present
  SENDGRID_API_KEY   ❌ missing → CRASH
```

> **Crash fast, crash loud.** A server that starts with missing config will fail unpredictably under specific conditions. A server that refuses to start without valid config is safe, detectable, and fixable.

---

## II. Proactive Error Detection

The best error handling starts **before** an error occurs.

### Health Checks

Move beyond simple "is the server responding?" pings. A process can be alive but completely unable to serve traffic if its dependencies are broken.

```
❌ Shallow health check:
   GET /health → 200 OK
   (only proves the HTTP server is running — nothing else)

✅ Deep health check:
   GET /health/ready →

   ├── Run test query on database:     SELECT 1  ✅ / ❌
   ├── Ping Redis:                     PING       ✅ / ❌
   ├── Generate test token (Auth0):    POST /oauth/token ✅ / ❌
   └── Check disk space:               > 10% free ✅ / ❌

   All pass → 200 OK  (load balancer routes traffic here)
   Any fail → 503     (load balancer pulls instance from rotation)
```

| Check Type | What It Verifies |
|---|---|
| Liveness | Process is running and not deadlocked |
| Readiness | All dependencies reachable, ready for traffic |
| Dependency-specific | Each external service individually callable |

---

### Observability: Catch Problems Before Users Do

A spike in error rate or response time is almost always visible in metrics **before** users start complaining.

```
Normal state:
  Successful transactions/min:  ████████████ 1,200
  P95 response time:            ██ 180ms
  Error rate:                   ░ 0.2%

Early warning — something is wrong:
  Successful transactions/min:  ███████ 700        ← drop of 40%
  P95 response time:            ████████ 820ms     ← 4× slower
  Error rate:                   █████ 8%           ← 40× higher
         │
         ▼
  Alert fires → engineer investigates
         │
         ▼
  Found: database connection pool exhausted
  Fix applied before users notice large-scale failure ✅
```

**Key metrics to track:**

| Metric | Alert Threshold |
|---|---|
| Error rate | > 1% of requests |
| P95 response time | > 2× baseline |
| Successful transaction rate | Drop > 20% from baseline |
| External service call failures | > 5% failure rate |
| Database connection pool usage | > 80% utilized |

---

## III. Error Handling Philosophies

The strategy for handling an error depends on whether it is **transient** (temporary, likely to resolve) or **permanent** (requires human intervention or graceful degradation).

```
Error occurs
     │
     ▼
Is it transient?
(network blip, timeout, rate limit)
     │
  ┌──┴──┐
 Yes    No
  │      │
  ▼      ▼
Retry  Is the failed component essential?
with      │
backoff ┌─┴─┐
       Yes   No
        │     │
        ▼     ▼
   Contain  Degrade gracefully:
   damage   ├── Return cached data
   + alert  ├── Disable the feature
            └── Return partial response
```

---

### Recoverable Errors: Retry with Exponential Backoff

For transient failures — the operation is valid, the dependency is temporarily unavailable.

```
Attempt 1 → timeout → wait 1s
Attempt 2 → timeout → wait 2s
Attempt 3 → timeout → wait 4s
Attempt 4 → timeout → wait 8s
Attempt 5 → timeout → give up → escalate (DLQ / alert)

Add jitter to prevent synchronized retry storms:
  Wait = base_delay × 2^attempt + random(0ms, 500ms)
```

> **Never retry immediately.** Retrying a struggling service instantly is the same as hammering it — it makes the outage worse for everyone. Backoff gives the service time to recover.

---

### Non-Recoverable Errors: Graceful Degradation

When a non-essential dependency fails, **keep the core experience alive**.

```
E-commerce checkout — recommendation engine is down:

  ❌ Without degradation:
     Whole checkout page returns 500
     User cannot complete purchase
     Revenue lost ❌

  ✅ With degradation:
     Recommendation widget catches its error
     Returns cached "popular items" instead
     OR hides the widget entirely
     Checkout continues normally ✅
     User completes purchase ✅
```

| Component fails | Degraded behavior |
|---|---|
| Recommendation engine | Show cached/popular items |
| Search service | Return empty results with a message |
| Analytics tracker | Log to local queue, sync later |
| Avatar / image CDN | Show default placeholder |
| Non-critical notification | Queue for retry, don't block response |

---

### Data Integrity: The #1 Priority

No error handling strategy is worth anything if user data is lost or corrupted.

```
Operation that modifies multiple tables:

  ❌ Without transaction:
     UPDATE accounts SET balance = balance - 100  ✅
     (server crashes here)
     UPDATE accounts SET balance = balance + 100  ❌ never ran
     → $100 disappeared from the system ❌

  ✅ With transaction:
     BEGIN;
       UPDATE accounts SET balance = balance - 100;
       UPDATE accounts SET balance = balance + 100;
     COMMIT;   ← both succeed, or neither does
     → data is always consistent ✅
```

**Data integrity toolbox:**

| Tool | Purpose |
|---|---|
| Database transactions | All-or-nothing multi-step operations |
| Idempotency keys | Safe retries — same operation, same result |
| Write-ahead logs (WAL) | Recover to any point before a crash |
| Backups + point-in-time recovery | Restore from data corruption or deletion |
| Manual recovery workflows | Runbooks for incidents that can't be automated |

---

## IV. The Global Error Handler

A centralized middleware that sits at the **end of the request pipeline** as a final safety net. Every unhandled error bubbles up here, gets classified, and is transformed into a consistent API response.

```
Request → Middleware A → Handler → Service → Repository
                                                  │
                                              throws error
                                                  │
                                  bubbles up through call stack
                                                  │
                                                  ▼
                                    ┌─────────────────────────┐
                                    │   Global Error Handler  │
                                    │   (Final Safety Net)    │
                                    │                         │
                                    │  classify error type    │
                                    │         │               │
                                    │  map to HTTP status     │
                                    │         │               │
                                    │  sanitize message       │
                                    │         │               │
                                    │  log with request ID    │
                                    └──────────┬──────────────┘
                                               │
                                               ▼
                                    Standardized JSON response
```

### Error-to-Status Mapping

```
ValidationException      →  400 Bad Request     (invalid input, field details included)
AuthenticationException  →  401 Unauthorized    (missing or invalid token)
AuthorizationException   →  403 Forbidden       (valid token, wrong permissions)
NotFoundException        →  404 Not Found       (resource doesn't exist)
ConflictException        →  409 Conflict        (duplicate email, constraint violation)
ExternalServiceException →  502 Bad Gateway     (upstream provider failed)
Unknown / RuntimeError   →  500 Internal Error  (generic message, full details in logs only)
```

### Standardized Error Response Shape

```json
{
  "error":     "Validation failed",
  "status":    400,
  "requestId": "a3f9c21d-7b8e-4c1a-9f2d-3e6b8a0c5d1f",
  "timestamp": "2024-01-15T10:30:00Z",
  "fields": {
    "email": "Must be a valid email address",
    "price": "Must be a positive number"
  }
}
```

The `requestId` is the bridge between what the user sees and what the engineer sees in logs — without exposing internal details.

---

### Spring Boot Implementation

```java
@RestControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    // Input validation failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
            .collect(toMap(FieldError::getField, FieldError::getDefaultMessage));

        return ResponseEntity.badRequest().body(new ErrorResponse(
            "Validation failed", 400, getRequestId(request), fields
        ));
    }

    // Resource not found
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        NotFoundException ex, HttpServletRequest request
    ) {
        return ResponseEntity.status(404).body(new ErrorResponse(
            ex.getMessage(), 404, getRequestId(request)
        ));
    }

    // Catch-all — never leak internal details
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
        Exception ex, HttpServletRequest request
    ) {
        String requestId = getRequestId(request);
        // Full stack trace goes to logs only — never in the response
        log.error("Unhandled exception [requestId={}]", requestId, ex);

        return ResponseEntity.status(500).body(new ErrorResponse(
            "Something went wrong. Reference ID: " + requestId,
            500,
            requestId
        ));
    }
}
```

---

## V. Security and Compliance in Error Handling

**How you report errors can inadvertently help attackers.** The same information that helps engineers debug is a map for someone trying to compromise the system.

### Rule 1 — Never Leak Internal Details

```
❌ Leaking raw database error to the client:
   {
     "error": "ERROR: duplicate key value violates unique constraint
               'users_email_key' on table 'users' in schema 'public'"
   }
   → Attacker now knows: table name, column name, constraint name, schema name ❌

✅ Sanitized response:
   {
     "error": "An account with this email already exists",
     "status": 409
   }
   → Actionable for the user, nothing useful for an attacker ✅

Full error → logs only, keyed by requestId, never in API response
```

---

### Rule 2 — Prevent Account Enumeration

```
❌ Specific error messages on login:
   "No account found with this email"     ← attacker can enumerate valid emails
   "Incorrect password for this account"  ← confirms the email IS registered

✅ Generic message regardless of which field is wrong:
   "Invalid email or password"
   → attacker learns nothing about whether the email exists ✅
```

Same principle applies to password reset:

```
❌ "No account found for bob@example.com"
✅ "If an account exists for this email, a reset link has been sent"
```

---

### Rule 3 — Sanitize Logs

Logs are often less protected than databases — they flow through logging pipelines, third-party log aggregators, and are accessed by many engineers.

```
❌ Logging sensitive data:
   log.info("Login attempt: email={}, password={}", email, password);
   log.info("Payment: card={}, cvv={}, amount={}", cardNumber, cvv, amount);
   log.info("User data: {}", userObject);   ← userObject may contain PII

✅ Log only what's needed for debugging:
   log.info("Login attempt: userId={}, requestId={}", userId, requestId);
   log.info("Payment initiated: orderId={}, amount={}, requestId={}", orderId, amount, requestId);
   log.info("User action: userId={}, action={}", userId, action);
```

**Never log:**

| Data Type | Why |
|---|---|
| Passwords (plain or hashed) | Hashes can be cracked; plains are instant compromise |
| Full credit card numbers | PCI-DSS violation |
| CVV / security codes | PCI-DSS violation |
| Full PII (name + address + DOB together) | GDPR / data protection violation |
| Auth tokens / session IDs | Can be replayed to impersonate users |
| API keys / secrets | Direct system compromise |

> **Log IDs, not values.** `userId=42` and `orderId=abc123` give you full traceability without exposing the data itself.

---

## VI. Quick Reference Checklist

| Concern | Strategy |
|---|---|
| Logic errors | Unit tests for edge cases, idempotency, code review |
| DB connection exhaustion | Pool sizing, request queuing, circuit breakers |
| Unique constraint violation | Catch and return `409 Conflict` |
| Deadlock | Retry with backoff; consistent lock ordering |
| External service timeout | Retry with exponential backoff + jitter |
| External service outage | Circuit breaker + graceful degradation |
| Missing env var at deploy | Validate all config at startup → crash if missing |
| Health check | Deep readiness check per dependency (not just ping) |
| Transient error | Retry with exponential backoff + max attempts |
| Non-essential service fails | Degrade gracefully — cache, disable, partial response |
| Multi-step data operation | Wrap in database transaction |
| Standardized error responses | Global error handler middleware |
| Validation error → client | `400` with field-level details |
| Unknown error → client | `500` with generic message + requestId |
| Full error details | Logs only — never in API response |
| DB error messages | Intercept and sanitize before responding |
| Login error specificity | Always generic: "Invalid email or password" |
| PII in logs | Never — log IDs only (userId, orderId, requestId) |
| Secrets in logs | Never — log that the action occurred, not the value |