# System Performance & Scalability — Complete Guide

## I. What Is Performance? Moving Beyond "Fast"

Performance is not a feeling — it is a set of **mathematically measurable properties**. Saying a system is "fast" is as useful as saying it is "good." Engineers define performance through concrete, trackable units.

```
Vague:    "The API feels slow today."
Precise:  "The p99 latency on /api/orders crossed 1.8s at 14:32 UTC,
           up from a baseline of 220ms — throughput held steady at 3,400 rps."
```

The two foundational metrics every engineer must understand:

```
┌─────────────────────────────────────────────────────────────┐
│  LATENCY                          THROUGHPUT                │
│                                                             │
│  Time from user action            Requests a system         │
│  to final rendered result         handles per second        │
│                                                             │
│  "How long does one              "How many can it           │
│   request take?"                  handle at once?"          │
│                                                             │
│  Measured in: ms / s             Measured in: req/s (rps)  │
└─────────────────────────────────────────────────────────────┘
```

---

## II. Why Averages Are Misleading

The average is the most commonly reported performance metric — and the most dangerous one to rely on alone.

### The Hidden Tail Problem

```
System processes 1,000,000 requests/day.

Average latency: 100ms  ← looks healthy ✅

Reality behind the average:
  990,000 requests → 50ms    (fast)
  9,000 requests   → 500ms   (noticeable)
  1,000 requests   → 5,000ms (5 seconds — users abandon ❌)

Average hides the 1,000 users experiencing a 5-second delay.
1,000 frustrated users per day = 365,000 per year.
```

> **The average is an optimistic lie.** A single extremely fast majority drowns out a painful slow minority. For high-value transactions like payments, that slow minority is often the most important.

---

## III. Percentiles — The Right Way to Measure Latency

Percentiles expose the **distribution** of latency, not just its center.

```
P50 (Median):  50% of requests complete at or below this time.
               The "typical" user experience.

P90:           90% of requests are at or below this time.
               The experience of users on slower networks or complex queries.

P99:           99% of requests are at or below this time.
               The worst experience seen by 1 in 100 users.

P999:          99.9% of requests are at or below this time.
               The absolute tail — relevant only at very high scale.
```

### Reading a Percentile Report

```
Endpoint: GET /api/orders

  P50  →   85ms   ← most users are happy ✅
  P90  →  210ms   ← acceptable ✅
  P99  →  1,840ms ← 1% of users wait nearly 2 seconds ⚠️
  P999 →  9,200ms ← edge case, but real ❌

An average of 90ms would hide the P99 and P999 entirely.
```

### Why Engineers Focus on P99

```
P99 requests are not random slow outliers. They often represent:

  ├── The most complex business logic
  │     (orders with 50 line items vs. 2)
  │
  ├── High-value transactions
  │     (a $10,000 enterprise checkout, not a $5 one)
  │
  └── Users most likely to report issues and churn
        (power users who transact frequently)
```

> **Optimizing for P99 protects your most valuable users and your most critical operations.**

---

## IV. Throughput and the Latency-Throughput Relationship

**Throughput** is the number of requests a system successfully handles per unit of time.

```
Low traffic:    200 rps   → latency:  80ms  ✅
Medium traffic: 1,000 rps → latency: 120ms  ✅ (slight increase)
High traffic:   2,000 rps → latency: 400ms  ⚠️
Near capacity:  2,400 rps → latency: 2,100ms ❌ (exponential spike!)
```

As throughput approaches system capacity, requests begin to **queue**. Queuing causes latency to grow not linearly, but exponentially.

```
Latency
  │                                        ╱
  │                                      ╱
  │                                    ╱
  │                                  ╱
  │                              ╱╱
  │                         ╱╱╱
  │─────────────────╱╱╱╱╱╱
  └─────────────────────────────────────── Throughput
                              ↑
                         "Knee" of the curve
                    (system approaches capacity)
```

> **Latency and throughput are coupled.** You cannot push throughput to 100% of capacity without paying an exponential latency cost.

---

## V. Utilization and the 60–80% Golden Rule

**Utilization** is the percentage of a resource (CPU, memory, DB connections) currently in use.

```
Utilization → Behavior

0–60%    → Latency is stable, predictable. Requests served immediately.
60–80%   → Latency creeps up slightly. Still acceptable. Headroom exists.
80–95%   → Latency climbs sharply. Queue forms. Burst traffic causes spikes.
95–100%  → Requests queue faster than processed. System appears "hung."
            Even a small traffic burst tips into cascade failure.
```

**Highway analogy:**

```
30% full:  Traffic flows freely.
70% full:  Slight slowdowns at on-ramps. Still moving.
90% full:  One fender-bender causes a 10-mile backup.
100% full: Gridlock. Adding more cars makes it worse.
```

> **Target: 60–80% in production.** The remaining 20–40% absorbs traffic spikes, bad deployments, retry storms, and background job bursts. A system at 95% is one viral tweet from an outage.

---

## VI. Identifying Bottlenecks: Measure, Don't Guess

A **bottleneck** is the single slowest component in a workflow — the one that determines the system's overall throughput ceiling.

```
Request pipeline:

  Auth check     →   5ms   ✅
  Business logic →  12ms   ✅
  DB query       → 980ms   ❌ ← bottleneck
  Serialization  →   3ms   ✅
  Network        →   8ms   ✅

  Total: ~1,008ms

Fixing anything except the DB query makes no meaningful difference.
Cutting serialization from 3ms → 1ms saves 0.2% of total time.
Fixing the DB query from 980ms → 50ms cuts total time by 93%.
```

> **Never guess the bottleneck.** Use New Relic, Prometheus/Grafana, or distributed tracing before applying any optimization.

### Tool 1: Profiling (CPU-Bound Tasks)

Profiling measures where the CPU spends its time. Use **Flame Graphs** — width equals time spent.

```
Flame Graph — each row is a call stack level, width = time spent

██████████████████████████████████████████████  main()
████████████████████████    ████████████████    processOrders()
████████████   █████████    ████   █████████    applyDiscount()
████████████                                    calculateTax()
  ↑
  Wide = more CPU time = look here first
```

### Tool 2: Distributed Tracing (IO-Bound Tasks)

Tracing records each step of a request as a **span** with precise timing:

```
Trace ID: a3f9c21d — GET /api/orders/789  (total: 1,008ms)

  Auth check            0ms ──── 5ms    ✅
  OrderService.get()    5ms ──── 17ms   ✅
  DB: SELECT orders    17ms ──────────── 997ms  ❌ ← 980ms here
  Serialization       997ms ─── 1,000ms ✅
  Network           1,000ms ─── 1,008ms ✅
```

---

## VII. Database Performance Optimization

Databases are almost always the primary bottleneck — responsible for disk I/O and consistency guarantees.

```
Operation speed (rough order of magnitude):

  CPU register access     →   1 ns
  L1 cache read           →   4 ns
  RAM read                → 100 ns
  SSD read                → 100 µs  (100,000 ns)
  Network round-trip      →   1 ms  (1,000,000 ns)
  DB query (no index)     →  10–100ms+ (full table scan)
  DB query (with index)   →   1–5ms
```

### Problem 1: The N+1 Query

```
❌ N+1 — 1,001 queries for 1,000 orders:

  Query 1:   SELECT * FROM orders LIMIT 1000
  Query 2:   SELECT * FROM order_items WHERE orderId = 1
  Query 3:   SELECT * FROM order_items WHERE orderId = 2
  ...
  Query 1001: SELECT * FROM order_items WHERE orderId = 1000

  1,001 round trips → ~1 second in DB calls alone ❌

✅ Bulk Fetch — 2 queries:

  Query 1:  SELECT * FROM orders LIMIT 1000
  Query 2:  SELECT * FROM order_items WHERE orderId IN (1, 2, ..., 1000)

  2 round trips → ~10ms total ✅
```

### Problem 2: Missing Indexes

```
No index — Sequential Scan (10M rows):
  DB reads every row → 8–15 seconds ❌

With B-Tree index on userId:
          [500]
         /     \
    [250]       [750]
    /   \       /   \
 [42] [300] [600] [900]
   │
   └── Pointer → row location on disk

  O(log n) lookup → 1–3ms ✅
```

```sql
-- Diagnose before adding indexes
EXPLAIN ANALYZE SELECT * FROM orders WHERE userId = 42;

--  Seq Scan on orders (10M rows)          ← no index ❌
--  Index Scan on orders_userid_idx        ← index used ✅
```

> Indexes speed up `SELECT` but slow down `INSERT`/`UPDATE`/`DELETE`. Every index is an extra write on every mutation. Add them based on query profiling, not speculation.

### Problem 3: Connection Pool Exhaustion

```
Without pooling:
  Open TCP + auth per request → ~65ms overhead
  5,000 simultaneous requests → 5,000 connections → DB crashes ❌

With pooling (e.g., HikariCP, PgBouncer):
  App startup: 20 idle connections pre-established
  Request borrows one → executes → returns it
  5,000 requests share 20 connections → queue briefly, DB survives ✅
```

| | No Pool | With Pool |
|---|---|---|
| Connection overhead | ~65ms per request | ~0.2ms per request |
| Traffic spike | DB crashes | Queue forms, DB stable |

> Start with 10–20 connections per app instance. If requests queue for connections, scale the pool or add more app instances.

---

## VIII. Caching Patterns

A cache stores expensive operation results in fast memory (Redis, Memcached) for instant retrieval.

```
Without cache:  Request → App → DB (50ms)    → Response: 50ms
With cache hit: Request → App → Redis (0.5ms) → Response: 0.5ms  (100× faster ✅)
With cache miss: Request → App → Redis miss → DB (50ms) → write Redis → Response
```

### Pattern 1: Cache-Aside (Lazy Loading)

```
Read:  Check cache → HIT: return value | MISS: query DB → store in cache → return
Write: Write to DB → invalidate cache key
```

Best for general-purpose reads. Cache failure is non-fatal — app falls back to DB.

### Pattern 2: Write-Through

```
Write: Write to DB + write to cache simultaneously
Read:  Almost always a cache hit — no stale data risk
```

Best for read-heavy data that must stay fresh. Every write is slower (two writes).

### Pattern 3: Write-Behind (Write-Back)

```
Write: Write to cache → return 200 instantly → async flush to DB
Read:  Always a cache hit for recent writes
```

Fastest writes. Risk: data loss if cache crashes before flush.

| Pattern | Best For | Risk |
|---|---|---|
| Cache-Aside | General reads, simple systems | Brief stale data window |
| Write-Through | Freshness-critical reads | Slower writes |
| Write-Behind | Write-heavy workloads | Data loss on cache failure |

> Start with Cache-Aside. It handles 80% of use cases safely.

---

## IX. Scaling: Vertical vs. Horizontal

### Vertical Scaling (Scale Up)

Replace the server with a more powerful one — more cores, more RAM.

```
Pros:  ✅ Zero code changes  ✅ No distributed complexity  ✅ Fast to implement
Cons:  ❌ Hard hardware ceiling  ❌ Single point of failure  ❌ No geo-distribution
```

### Horizontal Scaling (Scale Out)

Add more instances behind a load balancer.

```
Before:  Client → [Server A]

After:   Client → [Load Balancer] → [Server A]
                                  → [Server B]
                                  → [Server C]

Pros:  ✅ Theoretically unlimited  ✅ Redundancy  ✅ Geographic distribution
Cons:  ❌ App must be stateless  ❌ Distributed state complexity  ❌ Needs orchestration
```

### The Stateless Requirement

```
❌ Stateful (cannot scale horizontally):
  Login → Server A stores session in local memory
  Next request → Load balancer routes to Server B
  Server B has no session → user is logged out ❌

✅ Stateless (horizontally scalable):
  Login → session stored in Redis (shared external store)
  Any instance reads session from Redis → user stays logged in ✅

Rule: State belongs in an external store (Redis, DB) — never in the server process.
```

**File uploads follow the same rule:**

```
❌ Save uploaded file to Server A's local SSD
  Next request routed to Server B → file not found ❌

✅ Upload file to Amazon S3 (centralized object storage)
  Any instance can read it ✅
```

| | Vertical | Horizontal |
|---|---|---|
| Mechanism | Bigger server | More servers |
| Code changes | None | App must be stateless |
| Failure tolerance | Single point of failure | Survives instance loss |
| Capacity ceiling | Hard hardware limit | Effectively unlimited |
| Complexity | Low | High |
| When to use | Early stage, quick fix | Production at scale |

> **Strategy:** Vertical scaling buys time. Horizontal scaling is the long-term answer.

---

## X. Load Balancers: The Gatekeepers

A load balancer distributes incoming traffic across multiple server instances. It is **mandatory** for horizontal scaling.

### Load Balancing Algorithms

**Round Robin** — sends requests in a rotating sequence.

```
Request 1 → Server A
Request 2 → Server B
Request 3 → Server C
Request 4 → Server A  (wraps around)

Best for: identical servers, similar request complexity.
```

**Least Connections** — routes to the instance with fewest active connections.

```
Server A: 47 active connections
Server B: 12 active connections  ← next request goes here
Server C: 31 active connections

Best for: expensive or variable-length requests (long-running DB queries,
          file uploads) where some requests hold connections much longer.
```

**Weighted Algorithms** — assigns more traffic to more powerful instances.

```
Server A (8GB RAM, 8 cores)  → weight 4  → receives 4× more traffic
Server B (4GB RAM, 4 cores)  → weight 2  → receives 2× more traffic
Server C (2GB RAM, 2 cores)  → weight 1  → receives baseline traffic

Best for: mixed-capacity fleets during gradual hardware upgrades.
```

### Health Checks

The load balancer continuously pings every instance. If one fails to return `200 OK`, it is removed from rotation until it recovers.

```
Every second:
  LB pings Server A → 200 OK ✅ → keeps routing traffic
  LB pings Server B → 200 OK ✅ → keeps routing traffic
  LB pings Server C → timeout  ❌ → blacklisted immediately

  All traffic routed to A and B.
  Server C recovers → health check passes → re-added to rotation ✅

  User-facing impact: zero ✅
```

> Without health checks, a dead instance keeps receiving traffic and returning errors. Health checks make failure invisible to users.

---

## XI. Database Scaling Strategies

Scaling stateful systems (databases) is fundamentally harder than scaling stateless application code.

### Read Replicas

One **primary** (master) handles all write operations. Multiple **replicas** (slaves) handle read operations — offloading 70–90% of traffic from the primary.

```
                    ┌──────────────┐
  Write (INSERT/    │   Primary    │  ← all writes go here
  UPDATE/DELETE) ──►│   (Master)   │
                    └──────┬───────┘
                           │  replicates asynchronously
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         [Replica 1]  [Replica 2]  [Replica 3]
              │            │            │
         Read queries  Read queries  Read queries
         (SELECT)      (SELECT)      (SELECT)

  Primary handles: 100% of writes + 10% of reads
  Replicas handle: 90% of reads — primary freed up significantly ✅
```

**The trade-off: Replication Lag**

```
User writes a post → Primary updated instantly
                   → Replica updated after ~10–500ms (physics-based delay)

User immediately reads their post → routed to Replica
                                  → post not there yet ❌

Fix: Route "read-your-own-writes" to the Primary for a short window after writes.
```

---

### Sharding (Horizontal Partitioning)

Physically dividing a massive table across multiple database instances based on a **shard key**.

```
Problem: orders table has 10 billion rows. Even with indexes, queries are slow.

Solution — shard by date:

  Shard 1 (DB instance 1): orders from 2022
  Shard 2 (DB instance 2): orders from 2023
  Shard 3 (DB instance 3): orders from 2024

  Query for a 2024 order → routed to Shard 3 only
  Each shard scans 3.3B rows instead of 10B ✅

Or shard by region:

  Shard 1: US orders       Shard 2: EU orders      Shard 3: APAC orders
  → data physically closer to users → lower latency ✅
```

```
Sharding trade-offs:

  ✅ Each shard is smaller → faster queries
  ✅ Scales write capacity (each shard has its own primary)
  ❌ Cross-shard queries are expensive (must query all shards and merge)
  ❌ Rebalancing shards when data grows is complex
  ❌ Application must know which shard to route to
```

### Managed / Serverless Databases

Modern services handle replication and sharding automatically:

| Service | Type | Key Strength |
|---|---|---|
| PlanetScale | MySQL-compatible | Branching, zero-downtime schema changes |
| Neon | PostgreSQL serverless | Scales to zero, instant branching |
| CockroachDB | Distributed SQL | Auto-sharding, geo-distribution, ACID |
| Aurora | AWS managed PostgreSQL/MySQL | Auto-scaling read replicas |

> For most startups, a managed database eliminates months of infrastructure work. Only build your own sharding if you have exhausted managed options at significant scale.

---

## XII. Global Caching: Content Delivery Networks (CDNs)

Physics limits the speed of light in fiber optic cables — a round trip from Tokyo to Virginia takes approximately **100ms** before a single byte of application logic runs.

```
User in Tokyo → origin server in Virginia:

  Request leaves Tokyo
       │  ~50ms (speed of light across Pacific)
       ▼
  Arrives in Virginia → processed → response sent
       │  ~50ms (return journey)
       ▼
  Arrives in Tokyo
  Total network time: ~100ms — before your app does anything ❌
```

### The CDN Solution: Edge Locations (PoPs)

CDNs cache content at **Points of Presence (PoPs)** — servers located in dozens of cities worldwide.

```
Without CDN:
  Tokyo user → Virginia server (100ms) ❌

With CDN:
  Tokyo user → Tokyo PoP (2ms) ← cached content served locally ✅
  Tokyo PoP → Virginia origin (100ms, only on cache miss — rare)

  Cached content: static files, images, fonts, API responses with TTL
```

### Edge Computing

Modern CDNs (Cloudflare Workers, AWS Lambda@Edge) allow running **small code** at the edge — not just serving cached files.

```
Use case: Authorization check

  Without edge:
    Request → CDN → Origin server in Virginia (100ms) → auth check → 403 Forbidden
    Wasted: full round trip + origin server resources ❌

  With edge:
    Request → CDN edge node → auth check runs at edge (2ms) → 403 Forbidden
    Benefit: unauthorized requests rejected in 2ms, origin server never touched ✅

Other edge use cases:
  ├── Rate limiting (block abusive IPs at the edge)
  ├── A/B testing (serve variant A or B based on cookie at edge)
  ├── Geo-routing (redirect EU users to EU origin)
  └── Request/response transformation (add headers, rewrite URLs)
```

---

## XIII. Asynchronous Processing for Perceived Performance

To reduce perceived latency, offload time-consuming tasks from the request-response cycle entirely.

```
Without async processing:

  User deletes account
       │
       ▼
  Delete user posts      (500ms)
  Delete user comments   (300ms)
  Delete user orders     (800ms)
  Remove from Stripe     (400ms)
  Send confirmation email (200ms)
       │
       ▼
  Response: 2,200ms ← user waits ❌

With async processing:

  User deletes account
       │
       ▼
  Push "DeleteAccount" task to queue   (5ms)
       │
       ▼
  Response: "Account deletion in progress" → 200 OK (5ms) ✅

  (Background worker processes the task over the next few seconds)
```

**Tasks that belong in async queues:**

| Task | Why Async |
|---|---|
| Send verification/welcome email | External SMTP — slow, can fail |
| Resize / transcode uploaded media | CPU-heavy, takes seconds |
| Generate PDF reports | Computation-heavy |
| Delete user account (GDPR) | Multi-system, takes many DB calls |
| Sync data to external CRM | External API, unpredictable latency |
| Send webhook notifications | Recipient may be slow or down |

```
Async queue workflow:

  Producer (your app)
       │  push task + payload
       ▼
  Message broker (Redis / BullMQ / SQS)
       │  stores task until a worker is ready
       ▼
  Consumer (background worker)
       │  dequeues → processes → ACKs
       ▼
  Task complete ✅ (retry automatically on failure)
```

---

## XIV. Microservices vs. Monoliths

### Monolith

A single deployable unit containing all application code.

```
┌─────────────────────────────────────────────┐
│              Monolithic App                 │
│                                             │
│  Auth  │  Orders  │  Payments  │  Users    │
│  ──────────────────────────────────────     │
│  Shared DB, shared memory, one deploy       │
└─────────────────────────────────────────────┘
```

```
Pros:  ✅ Easier to develop, test, and refactor
       ✅ Simple deployment — one artifact
       ✅ Easy to trace a request end-to-end
       ✅ No network overhead between components
       ✅ ACID transactions across all modules

Cons:  ❌ Full redeploy for any change
       ❌ Hard to scale individual components independently
       ❌ Large codebase becomes difficult to navigate for big teams
       ❌ Technology lock-in — entire app uses one stack
```

### Microservices

Independent services, each responsible for one domain, communicating over the network.

```
  [Auth Service] ──────► [API Gateway] ◄────── [Client]
  [Order Service] ─────►      │
  [Payment Service] ───►      │
  [User Service] ──────►      │

  Each service:
    ├── Its own codebase and repository
    ├── Its own database
    ├── Its own deployment pipeline
    └── Its own scaling policy
```

```
Pros:  ✅ Teams deploy independently — no coordination required
       ✅ Scale individual services independently (more Order workers, not more Auth)
       ✅ Technology flexibility — each service can use a different stack
       ✅ Fault isolation — Payment Service outage doesn't kill Auth

Cons:  ❌ Network calls replace function calls — latency + failure points
       ❌ Distributed transactions are extremely complex
       ❌ Observability requires tracing across services
       ❌ Significant operational overhead (many repos, pipelines, deployments)
```

### The Real Reason for Microservices

```
Common misconception:
  "Microservices make the system faster / more scalable"

Reality:
  Microservices are primarily about scaling your TEAM — not your machines.

  Conway's Law: "Systems reflect the communication structure of the organizations that build them."

  100 engineers in one codebase:
    → Merge conflicts, blocked deployments, unclear ownership ❌

  100 engineers across 10 service teams:
    → Each team owns, deploys, and scales their service independently ✅
```

### The Decision Rule

```
Stay with a monolith if:
  ├── Team is fewer than ~100 engineers
  ├── Traffic is manageable on a few scaled-up servers
  └── Domain boundaries are not yet well understood

Consider microservices if:
  ├── Team is large (> 100 engineers) and coordination is the bottleneck
  ├── Different modules have vastly different scaling needs
  │     (search needs 100× more resources than auth)
  └── Modules need different technology stacks
  
Rule: Start with a well-structured monolith.
      Extract services only when you have a specific, measured reason.
```

---

## XV. Serverless Computing

Serverless (e.g., AWS Lambda, Google Cloud Functions) abstracts the server entirely. You provide only the function code and a trigger.

```
Traditional server:
  You provision → you configure → you pay 24/7 → you scale manually

Serverless:
  You write a function → attach a trigger (HTTP, queue, schedule)
  Provider handles: provisioning, scaling, patching, availability
  You pay for: actual execution time (CPU milliseconds) only
```

```
Serverless scaling:

  0 requests   → 0 instances running  → $0.00 cost
  1 request    → 1 instance spins up  → ~$0.000002
  10,000 req/s → 10,000 instances     → scales automatically ✅
  0 requests   → scales back to 0     → billing stops ✅
```

### Pros and Cons

```
Pros:
  ✅ Automatic scaling — zero to millions of requests with no config
  ✅ Pay only for execution — no cost for idle time
  ✅ No server management — no OS patches, no capacity planning
  ✅ Built-in high availability across zones

Cons:
  ❌ Cold starts — first invocation after idle period takes 200ms–2s extra
  ❌ Execution time limits — AWS Lambda max: 15 minutes
  ❌ Limited local state — functions are stateless by design
  ❌ Vendor lock-in — hard to move between cloud providers
  ❌ Debugging is harder — distributed, ephemeral execution environments
```

### Cold Starts

```
Function was idle for a few minutes:

  Request arrives
       │
       ▼
  Provider allocates a new container   ← ~100–500ms (cold start penalty)
  Load your code + dependencies        ← ~100–1,000ms
  Execute your function                ← ~5ms (actual work)
       │
  Total: 200ms–2,000ms on cold start ❌ vs. 5ms warm ✅

Mitigations:
  ├── Keep functions warm with scheduled pings
  ├── Use Provisioned Concurrency (AWS) — pre-warms instances, at extra cost
  ├── Minimize dependency bundle size — faster cold start
  └── Use edge runtimes (Cloudflare Workers) — near-zero cold start ✅
```

### When to Use Serverless

| Good Fit | Poor Fit |
|---|---|
| Infrequent or spiky traffic | Consistent high-throughput traffic |
| Event-driven tasks (webhooks, queue workers) | Long-running jobs (> 15 min) |
| Scheduled jobs (cron replacements) | Latency-sensitive paths (cold start risk) |
| Prototype / low-traffic APIs | Workloads with heavy local state |

---

## XVI. The Performance & Scaling Investigation Playbook

```
Step 1: Measure — percentiles, not averages
        └── "p99 on /api/checkout is 3.2s"

Step 2: Check utilization
        └── "DB CPU at 91% — above the 80% safe ceiling ⚠️"

Step 3: Find the bottleneck with distributed tracing
        └── "DB query: SELECT * FROM orders WHERE userId = ? → 2,980ms"

Step 4: Diagnose with EXPLAIN ANALYZE
        └── "Seq Scan on orders (10M rows) — no index on userId ❌"

Step 5: Fix the specific bottleneck
        └── "CREATE INDEX idx_orders_userid ON orders(userId)"

Step 6: Verify with measurements
        └── "p99 drops to 180ms. DB CPU drops to 42% ✅"

Step 7: If throughput ceiling reached → scale
        └── Vertical first (quick, no code change)
        └── Horizontal next (stateless app + load balancer)
        └── DB read replicas for read-heavy load
        └── Sharding if single-table row counts exceed hundreds of millions
        └── CDN for static content and edge auth
        └── Async queues for time-consuming non-blocking tasks
```

---

## XVII. Mental Models and Final Principles

### Always Measure First

```
❌ "Our DB queries must be slow — let's add a cache."
✅ Profile → trace → find the actual bottleneck → fix that specific thing.

The most common performance mistake: adding a cache before profiling.
You might be caching the wrong layer entirely.
```

### Prefer Simplicity

```
Simpler                                   More Complex
───────────────────────────────────────────────────────►
Monolith → Modular monolith → Microservices
Vertical → Horizontal scaling
Managed DB → Self-managed replicas → Custom sharding
No cache → Redis cache → Multi-tier CDN + edge cache
Sync API → Async queue → Event streaming (Kafka)

Add complexity only when simplicity has measurably failed.
```

### Implement Observability Early

```
Logs + Metrics + Traces from day one:

  Logs:    What happened? (structured JSON, keyed by requestId)
  Metrics: How much / how often? (Prometheus → Grafana dashboards)
  Traces:  Where did time go? (Jaeger, Datadog APM)

Adding observability after a production incident is painful.
Adding it at the start costs almost nothing and pays off enormously.
```

---

## XVIII. Quick Reference Checklist

| Concern | Tool / Strategy |
|---|---|
| Measure latency correctly | Percentiles (P50/P90/P99) — never averages alone |
| Find throughput limits | Load test — find the "knee" of the latency curve |
| Keep utilization safe | Target 60–80%; leave headroom for bursts |
| Find CPU bottlenecks | Profiling + Flame Graphs |
| Find IO bottlenecks | Distributed Tracing (spans per component) |
| Fix N+1 queries | Bulk fetch or JOIN; audit ORM-generated SQL |
| Speed up slow queries | `EXPLAIN ANALYZE` → index `WHERE`/`JOIN` columns |
| Reduce DB connection cost | Connection pooling (HikariCP, PgBouncer) |
| Cache frequently read data | Cache-Aside (general); Write-Through (freshness) |
| Scale quickly, no code change | Vertical scaling — upgrade the machine |
| Scale long-term | Horizontal scaling — stateless app + load balancer |
| Enable horizontal scaling | Sessions → Redis; Files → S3 (centralize all state) |
| Distribute traffic evenly | Round Robin (uniform load) |
| Distribute expensive requests | Least Connections algorithm |
| Mixed server capacities | Weighted load balancing |
| Remove dead instances | Load balancer health checks (every 1–5s) |
| Offload reads from DB primary | Read replicas (handle 70–90% of queries) |
| Read-after-write consistency | Route own writes to primary for short window |
| Scale a massive single table | Sharding by date, region, or user ID |
| Avoid infrastructure complexity | Managed DB (Neon, PlanetScale, CockroachDB) |
| Reduce global latency | CDN — cache at edge PoPs close to users |
| Reject bad requests cheaply | Edge computing — auth/rate-limit at CDN (2ms) |
| Slow tasks blocking responses | Async queue (BullMQ + Redis, SQS) |
| Uncertain monolith vs. services | Start monolith; extract only with measured reason |
| Team coordination bottleneck | Microservices (> 100 engineers, clear domain boundaries) |
| Spiky or low-traffic workloads | Serverless (Lambda, Cloud Functions) |
| Cold start latency in serverless | Provisioned concurrency or edge runtime |
| Monitoring and alerting | New Relic, Prometheus + Grafana; logs + metrics + traces |