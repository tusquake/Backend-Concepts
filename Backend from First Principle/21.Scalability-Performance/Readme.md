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
  990,000 requests → 50ms   (fast)
  9,000 requests   → 500ms  (noticeable)
  1,000 requests   → 5,000ms (5 seconds — users abandon ❌)

Average hides the 1,000 users experiencing a 5-second delay.
1,000 frustrated users per day = 365,000 per year.
```

> **The average is an optimistic lie.** A single extremely fast majority drowns out a painful slow minority. For high-value transactions like payments, that slow minority is often the most important.

---

## III. Percentiles — The Right Way to Measure Latency

Percentiles expose the distribution of latency, not just its center.

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

---

### Why Engineers Focus on P99

```
P99 requests are not random slow outliers.
They often represent:

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

**Throughput** is the number of requests a system successfully handles per unit of time (usually per second).

```
Low traffic:    200 rps   → latency: 80ms   ✅
Medium traffic: 1,000 rps → latency: 120ms  ✅ (slight increase)
High traffic:   2,000 rps → latency: 400ms  ⚠️
Near capacity:  2,400 rps → latency: 2,100ms ❌ (exponential spike!)
```

As throughput approaches system capacity, requests begin to **queue**. Queuing causes latency to grow not linearly, but **exponentially**.

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

**Utilization** is the percentage of a system resource (CPU, memory, DB connections) currently in use.

### The Counterintuitive Curve

Latency does not increase proportionally with utilization. It is well-behaved at low utilization, then **explodes** as you approach 100%.

```
Utilization → Behavior

0–60%   → Latency is stable, predictable. Most requests served immediately.

60–80%  → Latency creeps up slightly. Still acceptable. Headroom exists.

80–95%  → Latency climbs sharply. Queue forms. Burst traffic causes spikes.

95–100% → Requests queue faster than they are processed.
           Latency becomes unpredictable. System appears "hung."
           Even a small traffic burst tips into cascade failure.
```

**Highway analogy:**

```
30% full:  Traffic flows freely. Lane changes are instant.
70% full:  Slight slowdowns at on-ramps. Still moving.
90% full:  One fender-bender causes a 10-mile backup.
100% full: Gridlock. No one moves. More cars make it worse.
```

### The Golden Rule

```
✅ Target utilization: 60% – 80% in production

The remaining 20–40% is not waste.
It is the buffer that absorbs:
  ├── Sudden traffic spikes (viral post, flash sale)
  ├── Runaway queries from a bad deployment
  ├── Background jobs that spike CPU
  └── Retry storms from a failing downstream service
```

> **A system running at 95% utilization is one viral tweet away from an outage.**

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
Cutting serialization from 3ms to 1ms saves 0.2% of total time.
Fixing the DB query from 980ms to 50ms cuts total time by 93%.
```

> **The most common performance mistake:** Adding a cache before profiling. You might be caching the wrong thing entirely.

---

### Tool 1: Profiling (CPU-Bound Tasks)

Profiling measures where the CPU spends its time during execution. It is the right tool for computation-heavy code (sorting, parsing, encryption, image processing).

**Flame Graphs** are the standard visualization:

```
Flame Graph — each row is a call stack level, width = time spent

██████████████████████████████████████████████  main()
████████████████████████    ████████████████    processOrders()
████████████   █████████    ████   █████████    applyDiscount()
████████████                                    calculateTax()
  ↑
  Wide = more CPU time = look here first
```

> Functions that appear wide in a flame graph are consuming the most CPU. Narrow functions are cheap — don't optimize them.

---

### Tool 2: Distributed Tracing (I/O-Bound Tasks)

Profiling cannot find slowness caused by **waiting** — waiting for a database, an external API, or a network call. For these, use distributed tracing.

Tracing records each step of a request as a **span** with precise timing:

```
Trace ID: a3f9c21d — GET /api/orders/789  (total: 1,008ms)

  Auth check            0ms ──── 5ms    ✅
  OrderService.get()    5ms ──── 17ms   ✅
  DB: SELECT orders   17ms ──────────── 997ms  ❌ ← 980ms here
  Serialization       997ms ─── 1,000ms ✅
  Network             1,000ms ─ 1,008ms ✅
```

> Tracing tells you not just *that* the system is slow, but *which hop* is responsible — down to the specific DB query or external API call.

---

## VII. Database Performance Optimization

Databases are almost always the primary bottleneck because they are responsible for **disk I/O** and **consistency guarantees** — both inherently slower than in-memory operations.

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

---

### Problem 1: The N+1 Query

The N+1 problem is one of the most common and costly database mistakes. It occurs when you fetch a list of N items, then run an additional query **for each item** to fetch its details.

```
❌ N+1 — 1,001 queries for 1,000 orders:

  Query 1:  SELECT * FROM orders LIMIT 1000
             → returns 1,000 order rows

  For each order:
    Query 2:   SELECT * FROM order_items WHERE orderId = 1
    Query 3:   SELECT * FROM order_items WHERE orderId = 2
    Query 4:   SELECT * FROM order_items WHERE orderId = 3
    ...
    Query 1001: SELECT * FROM order_items WHERE orderId = 1000

  1,001 round trips to the database.
  At 1ms per query: ~1 second just in DB calls. ❌
```

```
✅ Bulk Fetch — 2 queries for 1,000 orders:

  Query 1:  SELECT * FROM orders LIMIT 1000
             → returns 1,000 order rows, collect all orderIds

  Query 2:  SELECT * FROM order_items
            WHERE orderId IN (1, 2, 3, ..., 1000)
             → returns all related items in one shot

  2 round trips. At 5ms per query: 10ms total. ✅
```

**Alternatively — use a JOIN:**

```sql
-- 1 query, all data in one result set
SELECT o.*, i.*
FROM orders o
JOIN order_items i ON i.orderId = o.id
WHERE o.userId = 42;
```

> Most ORMs silently generate N+1 queries. Always check the actual SQL your ORM produces in development.

---

### Problem 2: Missing Indexes

Without an index, the database performs a **sequential scan** — reading every row in the table to find the matching ones.

```
No index — Sequential Scan:

  Table: orders (10,000,000 rows)
  Query: SELECT * FROM orders WHERE userId = 42

  DB reads row 1... not userId 42.
  DB reads row 2... not userId 42.
  ...
  DB reads row 7,834,291... match! ✅
  ...
  DB reads row 10,000,000. Done.

  Time: 8–15 seconds ❌
```

```
With index — B-Tree lookup:

  Index on orders.userId acts like a library catalog.
  DB jumps directly to the entries for userId = 42.

  Time: 1–3ms ✅
```

**How a B-Tree index works:**

```
B-Tree Index on orders.userId:

          [500]
         /     \
    [250]       [750]
    /   \       /   \
 [42] [300] [600] [900]
   │
   └── Pointer → row location on disk
```

The database traverses the tree in O(log n) time — not O(n) like a full scan.

---

### The Cost of Indexes

Indexes are not free. Every index you add has ongoing costs:

```
✅ Indexes speed up:   SELECT, WHERE, JOIN, ORDER BY
❌ Indexes slow down:  INSERT, UPDATE, DELETE

Why? Every write must update not just the table row,
but also every index that covers that column.

A table with 8 indexes on it performs 8 extra write operations
on every INSERT or UPDATE.
```

**Diagnostic tool: EXPLAIN ANALYZE**

```sql
-- Run this before and after adding an index
EXPLAIN ANALYZE
SELECT * FROM orders WHERE userId = 42;

-- Output tells you:
--   Seq Scan on orders (cost=0.00..184,219.00 rows=10000000)  ← no index ❌
--   Index Scan on orders_userid_idx (cost=0.43..8.46 rows=3)  ← index used ✅
```

> Add indexes on columns frequently used in `WHERE`, `JOIN`, and `ORDER BY` clauses. Audit and remove indexes that are never used — they slow writes for no benefit.

---

### Connection Pooling

Establishing a fresh TCP connection to the database for every query is expensive — it involves a full handshake and authentication round-trip.

```
Without connection pooling:

  Request arrives
       │
       ▼
  Open TCP connection to DB     ← ~50ms overhead per request
  Authenticate                  ← ~10ms
  Execute query                 ← ~5ms
  Close connection              ← ~5ms
       │
  Total: ~70ms, mostly overhead ❌

  Under traffic spike: 5,000 simultaneous requests
  → 5,000 simultaneous connections → DB crashes ❌
```

```
With connection pooling:

  App startup: pool initializes 20 idle connections.

  Request arrives
       │
       ▼
  Borrow idle connection from pool  ← ~0.1ms
  Execute query                     ← ~5ms
  Return connection to pool         ← ~0.1ms
       │
  Total: ~5.2ms ✅

  Under traffic spike: 5,000 requests share 20 connections.
  Requests queue briefly — DB stays healthy ✅
```

| | No Pool | With Pool |
|---|---|---|
| Connection overhead | ~65ms per request | ~0.2ms per request |
| Traffic spike behavior | DB crashes at high concurrency | Queue forms, DB survives |
| Configuration needed | None | Pool size tuning required |

> **Recommended pool sizes:** Start at 10–20 connections per application instance. Monitor pool wait time — if requests queue for connections, increase pool size or scale app instances.

---

## VIII. Caching Patterns

A cache stores the result of an expensive operation in fast memory (e.g., Redis, Memcached) so it can be returned instantly on subsequent requests without repeating the work.

```
Without cache:
  Request → App → DB query (50ms) → Response (50ms total)

With cache (hit):
  Request → App → Redis (0.5ms) → Response (0.5ms total) — 100x faster ✅

With cache (miss):
  Request → App → Redis miss → DB query (50ms) → write to Redis → Response
```

---

### Pattern 1: Cache-Aside (Lazy Loading)

The most common pattern. The application is responsible for managing the cache.

```
Read path:
  1. Check cache for key
  2. Cache HIT  → return cached value ✅
  3. Cache MISS → query DB → store result in cache → return value

Write path:
  1. Write to DB
  2. Invalidate (delete) the cache key
     → next read will repopulate from DB
```

```
Pros:  Only caches data actually requested (no wasted memory)
       Cache failure is non-fatal — app falls back to DB
Cons:  First request after a miss is slow (cache population)
       Risk of stale data between DB write and cache invalidation
```

---

### Pattern 2: Write-Through

Cache and database are always updated together on every write.

```
Write path:
  1. Write to DB
  2. Write to cache simultaneously
  → Cache is always in sync with DB ✅

Read path:
  1. Check cache — almost always a HIT (no misses for active keys)
```

```
Pros:  Cache is always fresh — no stale data risk
       Reads are almost always cache hits
Cons:  Every write is slower (two writes instead of one)
       Cache may fill with data that is never read
```

---

### Pattern 3: Write-Behind (Write-Back)

Writes go to the cache immediately and return to the user. The database is updated **asynchronously** in the background.

```
Write path:
  1. Write to cache → return 200 OK instantly ✅
  2. (Asynchronously) worker flushes cache to DB

Read path:
  1. Read from cache — always a HIT for recent writes
```

```
Pros:  Fastest write response time
       DB write load is smoothed out (batched flushes)
Cons:  If cache crashes before flush — data is lost ❌
       DB is temporarily inconsistent with cache
       Highest complexity to implement safely
```

---

### Choosing a Caching Pattern

| Pattern | Best For | Risk |
|---|---|---|
| Cache-Aside | General purpose reads, simple systems | Brief stale data window |
| Write-Through | Read-heavy data that must stay fresh | Slower writes, memory waste |
| Write-Behind | Write-heavy workloads, high write throughput | Data loss on cache failure |

> **Start with Cache-Aside.** It handles 80% of use cases safely. Move to Write-Through or Write-Behind only when you have a measured need.

---

## IX. Scaling: Vertical vs. Horizontal

When a single server can no longer handle the load, you scale. There are two fundamentally different approaches.

---

### Vertical Scaling (Scale Up)

Replace the existing server with a more powerful one: more CPU cores, more RAM, faster SSD.

```
Before:   [Server: 4 cores, 16GB RAM]  → handles 1,000 rps

After:    [Server: 32 cores, 256GB RAM] → handles 8,000 rps
```

```
Pros:
  ✅ Zero code changes required
  ✅ Simple — no distributed systems complexity
  ✅ No load balancer needed
  ✅ Fast to implement (upgrade the machine)

Cons:
  ❌ Hardware ceiling — no single machine is infinitely upgradable
  ❌ Single point of failure — one machine goes down, everything goes down
  ❌ No geographic distribution — users far from the server face high latency
  ❌ Expensive at the high end (diminishing returns on cost)
```

---

### Horizontal Scaling (Scale Out)

Add more instances of the same server running in parallel, with a **load balancer** distributing traffic between them.

```
Before:
  Client → [Server A]

After:
  Client → [Load Balancer] → [Server A]
                           → [Server B]
                           → [Server C]
                           → [Server D]
```

```
Pros:
  ✅ Theoretically infinite capacity — add more instances as needed
  ✅ Redundancy — one instance failing doesn't take down the app
  ✅ Geographic distribution — run instances in multiple regions
  ✅ Cost-efficient at scale — many cheap commodity servers beat one superserver

Cons:
  ❌ Code complexity — app must be stateless (no local session state)
  ❌ Distributed state management is hard (caches, sessions must be shared)
  ❌ Requires a load balancer and orchestration (Kubernetes, ECS)
  ❌ Debugging is harder across multiple instances
```

---

### The Stateless Requirement for Horizontal Scaling

```
❌ Stateful app (cannot scale horizontally):

  User logs in → Server A stores session in local memory
  Next request → Load balancer routes to Server B
  Server B has no session → User is logged out ❌

✅ Stateless app (horizontally scalable):

  User logs in → Session stored in Redis (shared external store)
  Next request → Server B reads session from Redis → User stays logged in ✅

Rule: Any instance of the app must be able to handle any request.
      State belongs in an external store (Redis, DB), not in the server.
```

---

### Vertical vs. Horizontal — Side by Side

| | Vertical Scaling | Horizontal Scaling |
|---|---|---|
| Mechanism | Bigger server | More servers |
| Code changes required | None | App must be stateless |
| Failure tolerance | Single point of failure | Redundant — survives instance loss |
| Capacity ceiling | Hard hardware limit | Effectively unlimited |
| Geographic distribution | No | Yes |
| Cost curve | Steep at high end | Linear with instances |
| Complexity | Low | High |
| When to use | Early stage, quick fix | Production systems at scale |

> **Strategy:** Vertical scaling buys time. Horizontal scaling is the long-term answer. Most teams start vertical and migrate to horizontal as traffic grows and the engineering investment becomes justified.

---

## X. Putting It All Together: The Performance Investigation Playbook

```
Step 1: Measure with percentiles — not averages
        └── "p99 latency on /api/checkout is 3.2s"

Step 2: Check utilization
        └── "DB CPU at 91% — above the 80% ceiling ⚠️"

Step 3: Identify the bottleneck with tracing
        └── "Span: DB query 'SELECT * FROM orders WHERE userId = ?' → 2,980ms"

Step 4: Diagnose the DB query with EXPLAIN ANALYZE
        └── "Seq Scan on orders (10M rows) — no index on userId ❌"

Step 5: Fix the specific bottleneck
        └── "CREATE INDEX idx_orders_userid ON orders(userId)"

Step 6: Verify the fix with measurements
        └── "p99 drops from 3.2s to 180ms. DB CPU drops to 42% ✅"

Step 7: If throughput ceiling is reached, scale
        └── Vertical first (quick) → Horizontal (long-term)
```

---

## XI. Quick Reference Checklist

| Concern | Tool / Technique |
|---|---|
| Measure latency correctly | Percentiles (P50, P90, P99) — never averages alone |
| Spot throughput limits | Load testing — find the "knee" of the latency curve |
| Keep utilization safe | Target 60–80%; leave 20% headroom for bursts |
| Find CPU bottlenecks | Profiling + Flame Graphs |
| Find I/O bottlenecks | Distributed Tracing (spans per component) |
| Fix N+1 queries | Bulk fetch or JOIN — verify ORM-generated SQL |
| Speed up slow queries | `EXPLAIN ANALYZE` → add index on `WHERE`/`JOIN` columns |
| Reduce DB connection overhead | Connection pooling (PgBouncer, HikariCP) |
| Cache frequently read data | Cache-Aside for general use; Write-Through for freshness |
| Handle more load (quick) | Vertical scaling — upgrade server |
| Handle more load (long-term) | Horizontal scaling — stateless app + load balancer |
| Enable horizontal scaling | Move session/state to external store (Redis) |