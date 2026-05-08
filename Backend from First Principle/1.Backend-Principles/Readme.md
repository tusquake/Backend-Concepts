# Backend from First Principles — Complete Roadmap

> A comprehensive guide to moving beyond CRUD APIs and building systems that are **reliable**, **scalable**, and **maintainable**.
> Series by [Sriniously](https://www.youtube.com/@Sriniously).

---

## Why This Roadmap Exists

Most backend tutorials teach you how to build a working API. Very few teach you why it breaks under load, how to secure it properly, or how to deploy it without taking down production. This series addresses that gap.

```
Junior backend engineer:         Senior backend engineer:
  ├── Builds CRUD APIs              ├── Understands the request lifecycle
  ├── Follows tutorials             ├── Designs for failure
  ├── It works on my machine        ├── Measures before optimizing
  └── Hopes it scales               └── Builds systems that explain themselves
```

The roadmap is organized into five foundational modules. Each builds on the last.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Module 1: Communication & Protocols                                │
│  "How does a request actually travel from browser to server?"       │
├─────────────────────────────────────────────────────────────────────┤
│  Module 2: Application Architecture                                 │
│  "How should the code inside the server be structured?"             │
├─────────────────────────────────────────────────────────────────────┤
│  Module 3: Security & Data                                          │
│  "How do we protect and store data correctly?"                      │
├─────────────────────────────────────────────────────────────────────┤
│  Module 4: Advanced System Design                                   │
│  "How do we make the system fast and handle more load?"             │
├─────────────────────────────────────────────────────────────────────┤
│  Module 5: Operational Excellence                                   │
│  "How do we run this safely in production?"                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Module 1 — Communication & Protocols

Before writing a single line of application code, a backend engineer must understand how data physically travels from a user's browser to a server and back.

---

### 1.1 The Request Life Cycle

A complete mental model of what actually happens when a user clicks a button.

```
Browser                                                        Server (AWS)
   │                                                               │
   │  1. DNS resolution: "api.example.com" → 54.23.11.8           │
   │  2. TCP handshake (3-way)                                     │
   │  3. TLS handshake (if HTTPS)                                  │
   │                                                               │
   │──────────── HTTP Request ────────────────────────────────────►│
   │             GET /api/orders HTTP/1.1                          │
   │             Host: api.example.com                             │
   │             Authorization: Bearer eyJ...                      │
   │                                                               │
   │             [Firewall → Load Balancer → App Server]           │
   │                                                               │
   │◄─────────── HTTP Response ───────────────────────────────────│
   │             HTTP/1.1 200 OK                                   │
   │             Content-Type: application/json                    │
   │             {"orders": [...]}                                 │
```

> Understanding this end-to-end path is what separates engineers who debug by guessing from those who know exactly which layer to inspect.

---

### 1.2 HTTP Deep Dive

HTTP is the language of the web. Most engineers use it without understanding it.

**Raw HTTP message anatomy:**

```
Request:                              Response:
─────────────────────────────         ─────────────────────────────
POST /api/orders HTTP/1.1             HTTP/1.1 201 Created
Host: api.example.com                 Content-Type: application/json
Content-Type: application/json        Cache-Control: no-store
Authorization: Bearer eyJ...          ETag: "a3f9c21d"
                                      X-Request-Id: abc-123
{"items": [...], "total": 99.00}
                                      {"orderId": 789, "status": "pending"}
```

**HTTP Methods and their semantics:**

| Method | Purpose | Idempotent? | Safe? |
|---|---|---|---|
| `GET` | Retrieve a resource | ✅ Yes | ✅ Yes |
| `POST` | Create a new resource | ❌ No | ❌ No |
| `PUT` | Replace a resource entirely | ✅ Yes | ❌ No |
| `PATCH` | Partially update a resource | ❌ No | ❌ No |
| `DELETE` | Remove a resource | ✅ Yes | ❌ No |

**Key header categories:**

```
Security headers:
  Strict-Transport-Security  → force HTTPS
  Content-Security-Policy    → prevent XSS
  X-Frame-Options            → prevent clickjacking

Caching headers:
  Cache-Control: max-age=3600  → cache for 1 hour
  ETag: "a3f9c21d"             → fingerprint of resource version
  If-None-Match: "a3f9c21d"   → client asks: "has this changed?"
                              → server returns 304 Not Modified if unchanged ✅
```

**The evolution of HTTP:**

```
HTTP/1.1  → One request per TCP connection (or slow pipelining)
            Head-of-line blocking: request 6 waits for request 5

HTTP/2    → Multiplexing: many requests over one TCP connection in parallel
            Header compression (HPACK)
            Server push

HTTP/3    → Runs over QUIC (UDP-based) instead of TCP
            Eliminates TCP head-of-line blocking
            Faster connection setup (0-RTT)
            Better performance on lossy mobile networks
```

---

### 1.3 Serialization

Serialization is the translation layer between your application's in-memory data structures and the bytes sent over the network.

```
Your Application                   Network                    Client
─────────────────                  ───────                    ──────
Go struct / Python dict            bytes                      JSON string
{ userId: 42,          ──serialize──►  7b 22 75 73  ──►  {"userId":42,
  name: "Alice",                                             "name":"Alice"}
  balance: 99.50 }
                       ◄─deserialize─  7b 22 75 73  ◄──  {"userId":42,...}
```

**Format comparison:**

| Format | Type | Size | Human-readable | Speed | Best For |
|---|---|---|---|---|---|
| JSON | Text | Large | ✅ Yes | Medium | Public APIs, browser clients |
| XML | Text | Largest | ✅ Yes | Slow | Legacy systems, SOAP |
| Protobuf | Binary | Small | ❌ No | Fast | Internal microservices, high throughput |
| MessagePack | Binary | Small | ❌ No | Fast | Mobile, bandwidth-constrained APIs |

> **Rule of thumb:** Use JSON for public-facing APIs (human-readable, universal support). Use Protobuf for internal service-to-service communication where performance matters.

---

## Module 2 — Application Architecture

Once data reaches your server, the code must be organized so it is easy to test, modify, and scale as complexity grows.

---

### 2.1 Routing

Routing maps an incoming URL + HTTP method to the function that handles it.

```
Incoming request: GET /api/v2/users/42/orders?status=pending

  ├── Method:   GET
  ├── Version:  v2
  ├── Path:     /users/{userId}/orders
  ├── Param:    userId = 42
  └── Query:    status = pending
         │
         ▼
  Route: GET /api/v2/users/:userId/orders
  Handler: OrderController.listByUser()
```

**Route types:**

```
Static:   GET /api/health          → exact match
Dynamic:  GET /api/users/:id       → captures :id from path
Regex:    GET /api/files/*         → wildcard; matches any suffix
```

**API versioning strategies:**

```
URL versioning (most common, most explicit):
  /api/v1/users
  /api/v2/users

Header versioning:
  GET /api/users
  API-Version: 2

Deprecation: Always give consumers a migration window.
  Response header on v1 routes:
  Deprecation: true
  Sunset: Sat, 31 Dec 2025 23:59:59 GMT
  Link: </api/v2/users>; rel="successor-version"
```

---

### 2.2 Layered Architecture

Separating concerns into distinct layers keeps each layer testable, replaceable, and focused on a single responsibility.

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER — Handlers / Controllers            │
│  Responsibilities: parse request, validate input,       │
│  call service, format response, return HTTP status      │
│  Does NOT contain business logic.                       │
└─────────────────────────────┬───────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│  BUSINESS LOGIC LAYER — Services                        │
│  Responsibilities: enforce rules, orchestrate calls,    │
│  make decisions (e.g., "can this user place an order?") │
│  Does NOT know about HTTP or the database directly.     │
└─────────────────────────────┬───────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│  DATA ACCESS LAYER — Repositories                       │
│  Responsibilities: translate between domain objects     │
│  and database rows. All SQL/ORM lives here.             │
│  Does NOT contain business logic.                       │
└─────────────────────────────┬───────────────────────────┘
                              │
                              ▼
                         Database
```

> **Why this matters:** If you want to swap PostgreSQL for MongoDB, only the Repository layer changes. If you want to add a CLI interface, you reuse the Service layer directly. Each layer is independently testable.

---

### 2.3 Middleware

Middleware is a chain of functions that each request passes through **before and after** reaching its handler. Cross-cutting concerns — logic that applies to many routes — belong here.

```
Request
   │
   ▼
[Logger Middleware]          ← logs method, path, requestId
   │
   ▼
[Auth Middleware]            ← verifies JWT, attaches user to context
   │
   ▼
[Rate Limiter Middleware]    ← rejects if > 100 req/min for this IP
   │
   ▼
[Handler: OrderController]  ← only runs if all middleware passed
   │
   ▼
[Error Handler Middleware]   ← catches any thrown error, formats response
   │
   ▼
Response
```

**Common middleware responsibilities:**

| Middleware | What It Does |
|---|---|
| Logger | Records request method, path, duration, status code |
| Auth | Validates token, rejects unauthorized requests with 401 |
| Rate Limiter | Caps requests per IP/user, returns 429 Too Many Requests |
| CORS | Adds headers allowing cross-origin browser requests |
| Error Handler | Catches unhandled exceptions, returns structured error JSON |
| Request ID | Attaches a unique ID to every request for log correlation |

---

## Module 3 — Security & Data

---

### 3.1 Authentication Patterns

**Stateful vs. Stateless authentication:**

```
Stateful (Session-based):
  Login → Server creates session → stores in DB/Redis
        → returns session cookie to client
  Every request → server looks up session ID in store

  Pro:  Easy to invalidate (delete session from store)
  Con:  Requires shared session store for horizontal scaling

Stateless (JWT-based):
  Login → Server creates signed JWT → returns to client
  Every request → server validates JWT signature (no DB lookup)

  Pro:  No shared store needed — scales horizontally trivially
  Con:  Cannot be invalidated before expiry without a blocklist
```

**JWT structure:**

```
eyJhbGciOiJIUzI1NiJ9  .  eyJ1c2VySWQiOjQyfQ  .  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV
      │                          │                           │
   Header                    Payload                    Signature
 (algorithm)           (userId, role, exp)          (verifies authenticity)
```

**OAuth2 and OpenID Connect:**

```
OAuth2:          Authorization framework — "App X can access your Google Drive"
OpenID Connect:  Identity layer on top of OAuth2 — "Log in with Google"

Flow:
  User → "Login with Google" button
       → Redirect to Google (Authorization Server)
       → User consents
       → Google redirects back with authorization code
       → Your server exchanges code for access token + ID token
       → ID token contains: userId, email, name (OpenID Connect)
```

**Password storage — salting and hashing:**

```
❌ Never store plain text: "password123"
❌ Never store plain hash: SHA256("password123") → same hash for everyone

✅ Store salted hash:
   salt = random_bytes(32)            → unique per user
   hash = bcrypt(password + salt)     → slow by design (prevents brute force)
   store: { salt, hash }

On login: bcrypt(inputPassword + storedSalt) == storedHash? ✅
```

---

### 3.2 Database Mastery

**ACID properties — what makes a transaction safe:**

```
Atomicity:    All operations in a transaction succeed, or none do.
              "Transfer $100: debit A AND credit B — never just one."

Consistency:  Every transaction brings the DB from one valid state to another.
              Constraints, foreign keys, and rules are always enforced.

Isolation:    Concurrent transactions don't interfere with each other.
              "Two users booking the last seat don't both succeed."

Durability:   Once committed, data survives crashes.
              "Power cut after COMMIT → data is still there on restart."
```

**CAP Theorem — you can only guarantee two of three:**

```
        Consistency
            △
           / \
          /   \
         /     \
        ▽───────▽
  Availability   Partition Tolerance

CA: Consistent + Available (only possible with no network partitions — rare in distributed systems)
CP: Consistent + Partition Tolerant → may reject requests to stay consistent (e.g., HBase, Zookeeper)
AP: Available + Partition Tolerant → may return stale data to stay up (e.g., Cassandra, DynamoDB)
```

> See the [Performance & Scalability guide](./system-performance-and-scalability.md) for deep dives on indexes (B-Trees), connection pooling, and the N+1 query problem.

---

### 3.3 Elasticsearch — Full-Text Search

Traditional databases use B-Tree indexes optimized for exact-match lookups. Full-text search requires a fundamentally different data structure: the **inverted index**.

```
Documents:
  Doc 1: "The quick brown fox"
  Doc 2: "The fox jumped over"
  Doc 3: "A quick brown dog"

Inverted Index:
  "quick"  → [Doc 1, Doc 3]
  "fox"    → [Doc 1, Doc 2]
  "brown"  → [Doc 1, Doc 3]
  "jumped" → [Doc 2]

Query: "quick fox"
  → "quick": [1, 3]  ∩  "fox": [1, 2]  → Doc 1 (appears in both) ✅
```

**Elasticsearch use cases:**

```
Full-text search:     "Find all articles mentioning 'distributed systems'"
Type-ahead:           User types "dis" → suggest "distributed", "discount", "display"
Relevance scoring:    Results ranked by TF-IDF — how often the term appears
                      in the document vs. how rare it is across all documents
Log aggregation:      The "E" in the ELK Stack (Elasticsearch, Logstash, Kibana)
```

> **Rule of thumb:** Elasticsearch is not a replacement for a relational database. Use PostgreSQL as your source of truth, and sync relevant fields to Elasticsearch for search.

---

## Module 4 — Advanced System Design

---

### 4.1 Caching Strategies

> See the [Performance & Scalability guide](./system-performance-and-scalability.md) for full coverage of Cache-Aside, Write-Through, and Write-Behind patterns with diagrams.

**Hierarchical caching** — multiple cache layers from fastest to slowest:

```
Request
   │
   ▼
[Browser Cache]        ← fastest; no network at all (Cache-Control headers)
   │ miss
   ▼
[CDN Cache]            ← edge node close to user; no origin server hit
   │ miss
   ▼
[Application Cache]    ← Redis/Memcached; no DB hit
   │ miss
   ▼
[Database]             ← slowest; only reached if all caches miss
```

> Cache the output at the layer closest to the user. A CDN cache hit for a static asset costs ~1ms and ~$0. A database query costs ~50ms and server resources.

---

### 4.2 Background Jobs

Operations that are too slow, too unreliable, or too non-urgent to block a user response belong in a background task queue.

```
❌ Synchronous (blocks user):
  POST /register → save user → send email (3s) → 201 Created
                               ↑ user waits here

✅ Asynchronous (returns immediately):
  POST /register → save user → enqueue "SendVerificationEmail" → 201 Created
                                        │
                               (background worker)
                                        ▼
                               dequeue → call email API → done ✅
```

**Common background job use cases:**

| Task | Why async? |
|---|---|
| Send verification / welcome email | External API; failure shouldn't fail sign-up |
| Resize / transcode uploaded images | CPU-intensive; takes seconds |
| Generate PDF invoices | CPU-intensive; user doesn't need it instantly |
| Sync data to third-party CRMs | External API; retry silently on failure |
| Nightly billing report generation | Scheduled; no user waiting |
| GDPR account deletion (fan-out) | Multi-system; run in parallel |

**Broker options:**

| Broker | Best For |
|---|---|
| [Redis](https://redis.io) (BullMQ) | Simple queues; already in your stack |
| [RabbitMQ](https://www.rabbitmq.com) | Complex routing, priority queues |
| [AWS SQS](https://aws.amazon.com/sqs/) | Fully managed, cloud-native |
| [Apache Kafka](https://kafka.apache.org) | Event streaming at massive scale |

> See the [Background Tasks guide](./background-tasks-async-processing.md) for the full architecture: producers, brokers, consumers, idempotency, exponential backoff, and dead letter queues.

---

### 4.3 Scaling & Performance

> See the [Performance & Scalability guide](./system-performance-and-scalability.md) for full coverage of vertical vs. horizontal scaling, bottleneck identification, profiling, and tracing.

**Object storage for large files (e.g., AWS S3):**

```
❌ Don't store files in your database or app server filesystem:
  → DB bloat, slow queries, no CDN, no replication, lost on server restart

✅ Use object storage (S3, GCS, Azure Blob):

  Upload flow:
    1. Client requests a pre-signed upload URL from your API
    2. API generates pre-signed URL (valid 15 min) → returns to client
    3. Client uploads file directly to S3 (bypasses your server entirely)
    4. S3 triggers a webhook/event → your worker processes the file
    5. Store the S3 object URL in your database

  Benefits:
    ├── Your server never handles raw bytes
    ├── S3 scales to petabytes automatically
    ├── CDN can sit in front of S3 for fast global delivery
    └── Durability: 99.999999999% (11 nines) built-in
```

---

## Module 5 — Operational Excellence

---

### 5.1 Observability

A production system must be able to explain its own behavior. Observability is built on three pillars working together.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        OBSERVABILITY                                │
│                                                                     │
│  ┌────────────┐     ┌────────────┐     ┌────────────────────┐      │
│  │    LOGS    │     │  METRICS   │     │      TRACES        │      │
│  │ What       │     │ How many   │     │ Where in the       │      │
│  │ happened?  │     │ & how fast?│     │ chain did it fail? │      │
│  └────────────┘     └────────────┘     └────────────────────┘      │
│       Loki              Prometheus           Jaeger / Tempo         │
│                              └──────────────────┘                   │
│                                  Grafana Dashboards                 │
└─────────────────────────────────────────────────────────────────────┘
```

**Real-world investigation workflow:**

```
Alert: "p99 latency on /api/checkout > 3s"
  │
  ▼
Grafana dashboard → latency spike since last deployment ← Metrics
  │
  ▼
Loki log filter → "DB connection pool exhausted" ← Logs
  │
  ▼
Jaeger trace → Span: OrderRepository.findById() → 2,980ms ← Trace
  │
  ▼
Root cause: missing index on orders.userId. Fixed in 20 minutes ✅
```

> See the [Logging, Monitoring & Observability guide](./logging-monitoring-observability.md) for the complete deep dive including Spring Boot implementation.

---

### 5.2 Reliability

A reliable system is one that fails **gracefully** rather than catastrophically.

**Graceful Shutdown:**

```
❌ Hard kill (SIGKILL):
  Server process terminated instantly.
  In-flight requests: dropped ❌
  Open DB transactions: rolled back ❌
  Background tasks: lost mid-execution ❌

✅ Graceful shutdown (SIGTERM handler):
  1. Stop accepting new requests (remove from load balancer)
  2. Wait for in-flight requests to complete (with timeout)
  3. Flush in-memory queues to persistent store
  4. Close DB connections cleanly
  5. Exit ✅
```

**Fault Tolerance patterns:**

```
Circuit Breaker:
  Downstream service starts failing → circuit opens → requests fail fast
  instead of queuing up and timing out → protects your server from cascade failure

  CLOSED (normal) → failure rate > threshold → OPEN (fail fast)
       ↑                                              │
       └────────── retry after timeout ───────────────┘

Retry with Exponential Backoff:
  Attempt 1 → fail → wait 1s
  Attempt 2 → fail → wait 2s
  Attempt 3 → fail → wait 4s
  ...Max attempts → Dead Letter Queue → alert on-call

Timeout:
  Every external call (DB, API, cache) must have a timeout.
  A call with no timeout can hang forever and exhaust your thread pool.
```

**OpenAPI Documentation:**

```yaml
# openapi.yaml — machine-readable contract for your API
openapi: 3.1.0
info:
  title: Orders API
  version: 2.0.0
paths:
  /api/v2/orders:
    post:
      summary: Create an order
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          description: Order created
        '422':
          description: Validation error
```

> OpenAPI generates client SDKs, interactive docs (Swagger UI), and mock servers automatically — reducing the coordination cost between frontend and backend teams.

---

### 5.3 DevOps for Backend Engineers

Backend engineers who understand deployment are dramatically more effective at debugging production issues.

**CI/CD Pipeline:**

```
Developer pushes code
         │
         ▼
[CI: Continuous Integration]
  ├── Run unit tests
  ├── Run integration tests
  ├── Static analysis / lint
  ├── Security scan (SAST)
  └── Build Docker image → push to registry
         │
         ▼ (on merge to main)
[CD: Continuous Delivery]
  ├── Deploy to staging → run smoke tests
  └── Deploy to production (via deployment strategy below)
```

**Containerization:**

```
Without Docker:                    With Docker:
  "Works on my machine"              Same container runs everywhere:
  Different OS versions              Developer laptop
  Missing dependencies               CI runner
  Manual environment setup           Staging server
                                     Production server
```

```dockerfile
# Dockerfile — reproducible build environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Deployment strategies:**

```
Blue-Green Deployment:
  Blue (current): serving 100% of traffic
  Green (new):    deployed, tested, idle

  Switch: load balancer flips from Blue → Green instantly ✅
  Rollback: flip back to Blue in seconds ✅
  Cost: requires double the infrastructure during transition

Rolling Deployment:
  Replace instances one at a time:
  [v1] [v1] [v1] [v1]
  [v2] [v1] [v1] [v1]   ← replace instance 1
  [v2] [v2] [v1] [v1]   ← replace instance 2
  [v2] [v2] [v2] [v2]   ← done ✅

  Cost: gradual; both versions run simultaneously during rollout
  Risk: brief period of mixed API versions

Canary Deployment:
  Route 5% of traffic to new version → monitor error rate
  Healthy? → gradually increase to 20% → 50% → 100%
  Error spike? → route 100% back to old version instantly ✅
  Best for: high-risk changes, large user bases
```

---

## The Learning Path

Each topic in this series builds on the previous. A recommended progression:

```
Start here:
  1. Request Life Cycle          → understand the full path of every request
  2. HTTP Deep Dive              → understand the protocol you use daily
  3. Layered Architecture        → structure your code to survive growth
  4. Auth Patterns               → security cannot be retrofitted later

Then:
  5. Database Mastery            → your DB is always the first bottleneck
  6. Background Jobs             → decouple what doesn't need to block users
  7. Caching Strategies          → reduce DB load after you've measured
  8. Observability               → you cannot fix what you cannot see

Advanced:
  9. Scaling & Performance       → measure, profile, then scale
  10. Reliability Patterns       → design for failure from the start
  11. DevOps for Backend         → own your code all the way to production
```

---

## Module Index

| Guide | Topics Covered |
|---|---|
| [Background Tasks & Async Processing](./background-tasks-async-processing.md) | Producers, brokers, consumers, task types, idempotency, retries, DLQ, Spring Boot |
| [Logging, Monitoring & Observability](./logging-monitoring-observability.md) | Logs, metrics, traces, Grafana stack, OpenTelemetry, Spring Boot |
| [System Performance & Scalability](./system-performance-and-scalability.md) | Percentiles, throughput, utilization, profiling, DB optimization, caching, scaling |