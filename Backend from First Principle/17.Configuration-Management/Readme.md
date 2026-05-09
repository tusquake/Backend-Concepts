# Configuration Management — Complete Guide

## I. What Is Configuration Management?

Configuration management is the **systematic approach to organizing and maintaining application settings** — the values that control how your application behaves without changing a single line of code.

### Configuration as the DNA of Your Application

The same compiled codebase behaves completely differently across environments based solely on its configuration:

```
Same Code → Different Config → Different Behavior

  ┌─────────────────────────────────────────────────────────────────┐
  │                      Your Application                           │
  └─────────────────────────────────────────────────────────────────┘
           │                    │                    │
           ▼                    ▼                    ▼
    Dev Config           Staging Config        Prod Config
    ─────────────        ──────────────        ────────────
    DB: localhost        DB: staging-db        DB: prod-cluster
    Log: DEBUG           Log: INFO             Log: WARN
    Pool size: 2         Pool size: 5          Pool size: 20
    Stripe: test key     Stripe: test key      Stripe: live key
    Feature flags: all   Feature flags: some   Feature flags: stable
           │                    │                    │
           ▼                    ▼                    ▼
    Fast local dev       Mirrors production    Handles live traffic
    Verbose logging      Catches deploy bugs   Maximum performance
```

> **Rule of thumb:** If changing a behavior requires a code change and a redeploy, it's logic. If it should change per environment or be toggled at runtime — it belongs in configuration.

---

## II. The 5 Types of Configuration

Managing a production system requires categorizing settings based on their sensitivity and impact.

---

### 1. Application Settings

Operational values that control runtime behavior — safe to version-control, low sensitivity.

```
PORT=8080
LOG_LEVEL=INFO
HTTP_TIMEOUT_MS=5000
MAX_REQUEST_BODY_SIZE=10mb
CORS_ALLOWED_ORIGINS=https://app.example.com
```

| Setting | Dev | Staging | Prod |
|---|---|---|---|
| `LOG_LEVEL` | `DEBUG` | `INFO` | `WARN` |
| `HTTP_TIMEOUT_MS` | `30000` | `10000` | `5000` |
| `PORT` | `8080` | `8080` | `443` |

---

### 2. Database Configurations

Connection details and performance tuning — **high sensitivity**, never commit to source control.

```
DB_HOST=prod-cluster.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=myapp_prod
DB_USER=myapp_service
DB_PASSWORD=••••••••••••••           ← secret, never hardcode
DB_POOL_SIZE=20
DB_POOL_IDLE_TIMEOUT_MS=30000
DB_CONNECT_TIMEOUT_MS=5000
```

```
Connection pool sizing by environment:

  Dev:     ██ 2 connections   (one developer, local machine)
  Staging: █████ 5            (automated tests, light QA traffic)
  Prod:    ████████████████████ 20   (concurrent live users)
```

---

### 3. External Service Credentials

API keys and tokens for third-party integrations — **maximum sensitivity**, rotate regularly.

```
STRIPE_SECRET_KEY=sk_live_••••••••••   ← different key per environment
STRIPE_WEBHOOK_SECRET=whsec_••••••••
RESEND_API_KEY=re_••••••••••••••••
CLERK_SECRET_KEY=sk_live_••••••••••
S3_BUCKET=myapp-prod-uploads
AWS_ACCESS_KEY_ID=AKIA••••••••••••••
AWS_SECRET_ACCESS_KEY=••••••••••••••
```

```
Test vs. Live credentials — never mix them:

  Dev / Staging:  STRIPE_SECRET_KEY=sk_test_...  ← no real charges ✅
  Production:     STRIPE_SECRET_KEY=sk_live_...  ← real money ❌ if misused

  Using a test key in prod → payments silently fail
  Using a live key in dev  → accidental real charges
```

---

### 4. Feature Flags

Toggles that **enable or disable features at runtime** — no redeploy required.

```
FEATURE_NEW_CHECKOUT=true
FEATURE_AI_RECOMMENDATIONS=false
FEATURE_DARK_MODE=true
FEATURE_BETA_DASHBOARD=false
```

```
Without feature flags:

  Build new checkout flow
         │
         ▼
  Merge to main → deploy to prod
         │
         ▼
  All users see new checkout at once
         │
  Bug discovered → rollback entire deploy ❌

With feature flags:

  Build new checkout flow → deploy (flag OFF)
         │
         ▼
  Flag ON for 5% of users → monitor
         │
         ▼
  Flag ON for US users only → monitor
         │
         ▼
  Flag ON for 100% → full rollout ✅
  Bug discovered → flip flag OFF instantly, no redeploy needed ✅
```

**Feature flag use cases:**

| Use Case | Example |
|---|---|
| Gradual rollout | Enable for 10% → 50% → 100% of users |
| Geographic targeting | New checkout flow for US users only |
| A/B testing | Show layout A vs. layout B, measure conversion |
| Kill switch | Disable a feature instantly under load |
| Internal beta | Enable only for users with `beta=true` |

---

### 5. Business Rules & Security Settings

Centralized rules and security parameters — changing these in config avoids hardcoded magic numbers scattered across the codebase.

```
# Business rules
MAX_ORDER_AMOUNT=10000
MIN_PASSWORD_LENGTH=12
FREE_TIER_RATE_LIMIT=100
PAID_TIER_RATE_LIMIT=10000
SESSION_TIMEOUT_MINUTES=30

# Security
JWT_SECRET=••••••••••••••••••••••••   ← secret
JWT_EXPIRY=15m
REFRESH_TOKEN_EXPIRY=7d
BCRYPT_ROUNDS=12
ALLOWED_IP_RANGES=10.0.0.0/8,172.16.0.0/12
```

```
Hardcoded magic number (dangerous):
  if (order.total > 10000) throw new Error("Limit exceeded");
                   ─────
                     └── buried in code, different in 3 places, out of sync ❌

Centralized config (safe):
  if (order.total > config.MAX_ORDER_AMOUNT) throw new Error("Limit exceeded");
                            ─────────────────
                              └── one source of truth, one place to change ✅
```

---

## III. Storage and Delivery Mechanisms

Choosing the right storage mechanism involves balancing **security, speed, and operational complexity**.

### 1. Environment Variables (`.env` files)

The most common standard. A `.env` file is loaded at startup by a library (e.g., `dotenv`) — values are injected into the OS environment and read by the application.

```
# .env.local  (developer's machine — never committed)
DATABASE_URL=postgres://localhost:5432/myapp_dev
LOG_LEVEL=DEBUG
STRIPE_SECRET_KEY=sk_test_...

# .env.example (committed to Git — shows required vars, no real values)
DATABASE_URL=
LOG_LEVEL=
STRIPE_SECRET_KEY=
```

```
Startup flow:

  App starts
      │
      ▼
  dotenv loads .env file
      │
      ▼
  Values injected into process environment
      │
      ▼
  App reads: process.env.DATABASE_URL  ✅
```

**Rules:**
- `.env` → **never commit** (add to `.gitignore`)
- `.env.example` → **always commit** (documents required vars, empty values)
- `.env.test`, `.env.staging` → environment-specific overrides

---

### 2. Configuration Files (YAML / JSON / TOML)

Preferred for **hierarchical, complex, or non-secret** configuration. YAML is favored over JSON because it supports comments — essential for team documentation.

```yaml
# config/application.yaml
server:
  port: 8080
  timeout: 5000

database:
  pool:
    min: 2
    max: 20            # increase in prod
    idle_timeout: 30s

logging:
  level: INFO          # DEBUG in dev, WARN in prod
  format: json         # structured for log aggregators

features:
  new_checkout: false  # enable via feature flag service
  dark_mode: true
```

```
YAML vs JSON for config:

  JSON:                          YAML:
  {                              server:
    "server": {                    port: 8080     # no quotes needed
      "port": 8080,                timeout: 5000  # comments allowed ✅
      "timeout": 5000              # much easier to read in teams
    }
  }
  ← no comments ❌
```

---

### 3. Cloud Secret Managers

For **high-sensitivity secrets** in production. Secrets are encrypted at rest and in transit, access is audited, and rotation can be automated.

| Service | Best For |
|---|---|
| AWS Parameter Store / Secrets Manager | AWS-hosted applications |
| HashiCorp Vault | Multi-cloud, on-premise, fine-grained access |
| Google Secret Manager | GCP-hosted applications |
| Azure Key Vault | Azure-hosted applications |

```
Benefits over plain env vars:

  ┌─────────────────────────────────┬──────────────────────────────────┐
  │ Environment Variables           │ Cloud Secret Manager             │
  ├─────────────────────────────────┼──────────────────────────────────┤
  │ Stored in plain text on server  │ Encrypted at rest and in transit │
  │ No audit log                    │ Full audit log (who accessed)    │
  │ Rotation requires redeploy      │ Automatic rotation               │
  │ Hard to share across services   │ Centralized, versioned           │
  │ No access control per secret    │ Per-secret IAM permissions       │
  └─────────────────────────────────┴──────────────────────────────────┘
```

---

### 4. Hybrid Strategy (Production Standard)

Most production systems use a layered approach — fetch from the most trusted source first, fall back to less trusted sources.

```
App starts → resolve config with priority order:

  Priority 1: Cloud Secret Manager (AWS / Vault)
                      │ found? use it ✅
                      │ not found? ↓
  Priority 2: Environment variables (injected by CI/CD or orchestrator)
                      │ found? use it ✅
                      │ not found? ↓
  Priority 3: Config file (application.yaml)
                      │ found? use it ✅
                      │ not found? ↓
  Priority 4: Default value (hardcoded fallback for non-critical settings)
                      │ found? use it ✅
                      │ not found? ↓
  CRASH — required config is missing, fail loudly at startup ❌
```

```
Why this layering works:

  Secrets (DB passwords, API keys)  → Cloud Manager — never in files or env ✅
  Deploy-time settings (PORT, ENV)  → Environment variables ✅
  Structural config (timeouts, etc) → YAML config files ✅
  Safe fallbacks (log format)       → Default values ✅
```

---

## IV. Environment-Specific Configuration

Each environment has a different **primary goal** — configuration should reflect and serve that goal.

```
Dev ──────────────► Staging ──────────────► Production
(Productivity)     (Mirror Prod)            (Reliability)
```

### Development (Local)

**Goal: Productivity.** Make the feedback loop as fast as possible.

```yaml
log_level: DEBUG          # verbose — see everything
database:
  host: localhost         # local DB, no network latency
  pool_size: 2            # only one developer
stripe:
  key: sk_test_...        # test mode — no real charges
features:
  all_flags: true         # all features enabled for local testing
hot_reload: true          # restart on code change
```

---

### Staging

**Goal: Mirror production to catch deployment bugs — at reduced cost.**

```yaml
log_level: INFO           # same as prod
database:
  host: staging-db.internal
  pool_size: 5            # smaller than prod to save cost
stripe:
  key: sk_test_...        # still test mode
features:
  new_checkout: true      # test features before prod rollout
  ai_recommendations: false
```

```
Staging should match prod in:        Staging can differ from prod in:
  ├── Log level                        ├── Database pool size (cost)
  ├── Config file structure            ├── Instance count (1 vs N)
  ├── Env var names                    ├── Stripe keys (test vs live)
  ├── Feature flag evaluation          └── External service quotas
  └── Migration state
```

---

### Production

**Goal: Reliability, security, and performance.**

```yaml
log_level: WARN           # only warnings and errors — reduce noise and cost
database:
  host: prod-cluster.rds.amazonaws.com
  pool_size: 20           # handle concurrent live traffic
  ssl: required           # encrypted connections only
stripe:
  key: sk_live_...        # real payments
features:
  new_checkout: false     # only stable, tested features on by default
session_timeout: 15m      # tighter security
rate_limit: 1000          # protect against abuse
```

### Environment Comparison

| Setting | Dev | Staging | Prod |
|---|---|---|---|
| `LOG_LEVEL` | `DEBUG` | `INFO` | `WARN` |
| DB pool size | 2 | 5 | 20 |
| DB host | `localhost` | staging cluster | prod cluster |
| Stripe keys | test | test | live |
| Feature flags | all on | selective | stable only |
| SSL required | no | yes | yes |
| Rate limiting | off | on | on (strict) |
| Hot reload | yes | no | no |

---

## V. Security Best Practices

### Rule 1 — Never Hardcode Secrets

```
❌ Hardcoded in source code:
   const stripe = new Stripe("sk_live_abc123xyz...");
                              ─────────────────────
                              now in Git history forever
                              visible to every engineer
                              leaked if repo is ever public ❌

✅ Loaded from environment:
   const stripe = new Stripe(process.env.STRIPE_SECRET_KEY);
   // actual key lives in Secret Manager, injected at runtime ✅
```

```
If a secret is ever accidentally committed:
  1. Invalidate / rotate the key immediately (assume it is compromised)
  2. Remove from Git history (git filter-branch or BFG Repo Cleaner)
  3. Audit access logs for unauthorized use
  4. Never assume "it was only in the repo for a few minutes" is safe
```

---

### Rule 2 — Principle of Least Privilege

Each person and service should only have access to the configs it absolutely needs.

```
Config access matrix:

                        DB Password  Stripe Key  Log Level  Feature Flags
                        ───────────  ──────────  ─────────  ─────────────
  Backend service            ✅          ✅          ✅           ✅
  Frontend developer         ❌          ❌          ✅           ✅
  Data analyst               ❌          ❌          ✅           ❌
  CI/CD pipeline             ✅          ✅          ✅           ❌
  Intern / new hire          ❌          ❌          ✅           ❌
```

```
In practice (AWS IAM example):

  Backend service role  → can read: db/*, stripe/*, jwt/*
  Frontend team         → can read: feature-flags/* only
  Analytics team        → can read: analytics/* only

  No role has "read everything" access ✅
```

---

### Rule 3 — Validate All Config at Startup

Every missing or malformed variable should be caught **before the app handles a single request**.

```
❌ Discovered at runtime (dangerous):

  App starts → serves traffic for 2 hours
       │
  User triggers Stripe charge
       │
  Code reads STRIPE_SECRET_KEY → undefined
       │
  Uncaught error → 500 to user
  Payment never attempted, but DB may be partially updated ❌

✅ Validated at startup (safe):

  App starts
       │
  Config validation runs:
    DATABASE_URL      ✅ present, valid URL
    STRIPE_SECRET_KEY ❌ missing
    JWT_SECRET        ✅ present, length >= 32
       │
  App refuses to start → logs clear error ✅
       │
  Deploy pipeline catches failure before any traffic is served
  Engineer adds missing variable → redeploy ✅
```

**Startup validation in Spring Boot:**

```java
@Configuration
@Validated
public class AppConfig {

    @Value("${DATABASE_URL}")
    @NotBlank(message = "DATABASE_URL is required")
    private String databaseUrl;

    @Value("${STRIPE_SECRET_KEY}")
    @NotBlank(message = "STRIPE_SECRET_KEY is required")
    @Pattern(regexp = "sk_(test|live)_.+", message = "STRIPE_SECRET_KEY must start with sk_test_ or sk_live_")
    private String stripeSecretKey;

    @Value("${JWT_SECRET}")
    @NotBlank(message = "JWT_SECRET is required")
    @Size(min = 32, message = "JWT_SECRET must be at least 32 characters")
    private String jwtSecret;

    @Value("${DB_POOL_SIZE:10}")          // default of 10 if not set
    @Min(value = 1, message = "DB_POOL_SIZE must be at least 1")
    @Max(value = 100, message = "DB_POOL_SIZE must not exceed 100")
    private int dbPoolSize;
}
```

```
App startup output when validation fails:

  [ERROR] Configuration validation failed:
    - STRIPE_SECRET_KEY: required but not set
    - JWT_SECRET: must be at least 32 characters (got 12)

  Application failed to start. Fix configuration and retry.

  → Deploy pipeline sees non-zero exit code → marks deploy as failed ✅
  → On-call engineer is notified immediately
  → Zero users are affected ✅
```

---

### Rule 4 — Separate Config from Code (12-Factor App)

```
❌ Config baked into code:
   if (environment == "prod") {
     dbUrl = "postgres://prod-cluster/myapp";
   } else {
     dbUrl = "postgres://localhost/myapp_dev";
   }
   → Env-specific logic scattered across the codebase
   → Adding a new environment requires code changes and a deploy ❌

✅ Config externalized, code is environment-agnostic:
   dbUrl = process.env.DATABASE_URL;
   // the same line works in dev, staging, and prod
   // only the injected value differs ✅
```

---

## VI. Spring Boot Configuration Reference

### application.yaml with Environment Profiles

```yaml
# src/main/resources/application.yaml  (base config — committed)
server:
  port: ${PORT:8080}

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}

logging:
  level:
    root: ${LOG_LEVEL:INFO}

app:
  jwt:
    secret: ${JWT_SECRET}
    expiry: ${JWT_EXPIRY:15m}
  stripe:
    secret-key: ${STRIPE_SECRET_KEY}
  features:
    new-checkout: ${FEATURE_NEW_CHECKOUT:false}
```

```yaml
# src/main/resources/application-dev.yaml  (dev overrides)
logging:
  level:
    root: DEBUG
    com.myapp: TRACE

spring:
  datasource:
    hikari:
      maximum-pool-size: 2
```

```yaml
# src/main/resources/application-prod.yaml  (prod overrides)
logging:
  level:
    root: WARN

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
```

**Activate a profile:**

```bash
# Via environment variable (preferred for containers)
SPRING_PROFILES_ACTIVE=prod java -jar app.jar

# Via JVM argument
java -Dspring.profiles.active=prod -jar app.jar
```

---

### Reading Config in Services

```java
@Service
public class PaymentService {

    private final String stripeKey;
    private final int maxOrderAmount;
    private final boolean newCheckoutEnabled;

    public PaymentService(
        @Value("${app.stripe.secret-key}") String stripeKey,
        @Value("${app.max-order-amount:10000}") int maxOrderAmount,
        @Value("${app.features.new-checkout:false}") boolean newCheckoutEnabled
    ) {
        this.stripeKey = stripeKey;
        this.maxOrderAmount = maxOrderAmount;
        this.newCheckoutEnabled = newCheckoutEnabled;
    }

    public PaymentResult charge(Order order) {
        if (order.getTotal() > maxOrderAmount) {
            throw new ValidationException("Order exceeds maximum allowed amount");
        }
        // ...
    }
}
```

---

## VII. Quick Reference Checklist

| Concern | Strategy |
|---|---|
| Behavior differs per environment | Externalize to config, never hardcode |
| Operational settings (port, timeouts) | Application settings via env vars or YAML |
| DB connection details | Config + Secret Manager for password |
| Third-party API keys | Secret Manager — never in source code |
| Toggle features without redeploy | Feature flags |
| Business rules (max amounts, limits) | Centralized config — one source of truth |
| Simple secrets, single server | Environment variables + `.env` file |
| Complex hierarchical config | YAML with comments |
| Production secrets | Cloud Secret Manager (AWS / Vault) |
| Multi-source resolution | Hybrid: Secret Manager → Env vars → YAML → Defaults |
| Dev environment | `DEBUG` logging, local DB, test keys, all flags on |
| Staging environment | Mirror prod structure, reduced pool size, test keys |
| Production environment | `WARN` logging, full pool, live keys, stable flags only |
| Secrets in source code | Never — rotate immediately if accidentally committed |
| Access to sensitive config | Least privilege — per-role, per-secret IAM |
| Missing config at runtime | Validate all required vars at startup — crash fast |
| Config coupled to code | Externalize — app reads env, never branches on env name |
| Spring profile activation | `SPRING_PROFILES_ACTIVE=prod` via environment variable |
| Spring config validation | `@Validated` + `@NotBlank` / `@Size` on config class |