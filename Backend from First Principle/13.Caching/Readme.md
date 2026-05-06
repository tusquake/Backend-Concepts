# Caching & In-Memory Databases — Complete Guide

## I. Real-World Examples of Caching

Caching is not an optimization — at scale, it is a **survival mechanism**. The world's largest platforms are built around it.

### Google Search

Millions of users search for identical queries every day — `"weather today"`, `"stock price"`, `"news headlines"`. Recomputing results from scratch for each of those requests would be computationally catastrophic.

Google uses a **distributed in-memory system** to store and instantly return cached results. The heavy computation happens once; the cached result serves millions.

```
User 1 searches "weather today"
      │
      ▼
Cache miss → compute result → store in cache → return result

User 2, 3, 4 ... 10,000,000 search "weather today"
      │
      ▼
Cache hit → return instantly ← no computation, no DB query ✅
```

---

### Netflix

Video streaming has a unique challenge — a single movie file can be hundreds of gigabytes. Serving that from one central server to users across the world creates massive latency and bandwidth cost.

Netflix uses **Content Delivery Networks (CDNs)** to store video files at **edge locations** — data centers geographically close to the user. When you press play, the video is served from a server a few hundred kilometers away, not from Netflix's origin servers across the globe.

```
Without CDN:
User in Mumbai → Origin Server in USA → 200ms+ latency, buffering

With CDN:
User in Mumbai → Edge Server in Mumbai → <10ms latency, instant playback ✅
```

---

### X (Twitter) — Trending Topics

Identifying what is "trending" requires scanning billions of tweets, counting mentions, applying time-decay algorithms, and personalizing by region. This cannot happen in real time per user request.

Twitter calculates trending topics **every few minutes** via a background job, then stores the pre-computed result in a key-value store like **Redis**. Every user request hits Redis — not the computation engine.

```
Background Job (runs every ~5 minutes):
  Scan billions of tweets → compute trends → store in Redis

User request:
  GET /trending → Redis lookup (microseconds) → return result ✅
```

> **The pattern:** Expensive computation is decoupled from request time. Pre-compute, store, serve instantly.

---

## II. Levels of Caching

As a backend engineer, you will interact with caching at multiple levels of the stack.

```
┌─────────────────────────────────┐
│         Network Level           │  CDN, DNS caching
├─────────────────────────────────┤
│        Hardware Level           │  CPU L1 / L2 / L3 cache
├─────────────────────────────────┤
│     Application / Software      │  Redis, Memcached
├─────────────────────────────────┤
│         Database Level          │  Query result cache, buffer pool
└─────────────────────────────────┘
```

---

### 1. Network Level

**CDN (Content Delivery Network)**
Caches static assets — images, videos, JavaScript bundles, CSS files — at edge locations worldwide. Requests are served from the nearest edge node, not the origin server.

```
First request → Origin Server → file returned → cached at edge node
All subsequent requests → Edge Node (cached) → no origin hit ✅
```

**DNS Caching**
Every time you visit a domain, your OS must resolve it to an IP address. This lookup is cached at multiple levels to avoid repeating it on every request:

```
Browser cache → OS cache → ISP resolver cache → Root DNS server

First visit:  full resolution chain (slow)
Return visit: OS cache hit → IP returned instantly ✅
```

---

### 2. Hardware Level — CPU Caches

CPUs have multiple layers of high-speed memory built directly into the chip. Each layer is faster but smaller than the one below it.

| Cache | Speed | Size | Location |
|---|---|---|---|
| L1 | ~1 ns | ~32 KB | Per core |
| L2 | ~5 ns | ~256 KB | Per core |
| L3 | ~20 ns | ~8–32 MB | Shared across cores |
| RAM | ~100 ns | GBs | Outside CPU |
| Disk | ~1–10 ms | TBs | Physical storage |

**Why this matters for code:** Sequential data access (iterating an array) is dramatically faster than random access (traversing a linked list) because predictive algorithms **pre-load the next array elements** into L1/L2 cache. This is called **cache locality** — and it's why arrays outperform linked lists even when they have the same algorithmic complexity.

---

### 3. Application / Software Level

This is what backend engineers primarily control. In-memory databases like **Redis** and **Memcached** sit between your application and your primary database.

```
Client Request
      │
      ▼
Application Server
      │
      ├── Check Redis (fast, in-memory)
      │       │
      │   Cache Hit → return instantly ✅
      │       │
      │   Cache Miss → query PostgreSQL → store in Redis → return
      │
      ▼
PostgreSQL (slow, disk-based)
```

---

## III. The Science of In-Memory Databases (Redis)

### Why Is Redis So Fast?

The answer comes down to **where** data lives physically.

| | Traditional Database (Postgres) | Redis |
|---|---|---|
| Storage | Disk (HDD/SSD) | RAM |
| Access mechanism | Mechanical seek / I/O | Electrical signal (direct address) |
| Typical read latency | 1–10 milliseconds | < 1 millisecond |
| Capacity | Terabytes | Gigabytes (limited by RAM) |
| Persistence | ✅ Durable by default | ⚠️ Volatile (configurable) |

**RAM access is nearly constant regardless of where the data is stored.** There is no "seeking" — the CPU sends an electrical signal to the exact memory address and retrieves the data immediately.

```
Disk read:
  Move read head → find sector → read data → transfer
  Time: 1–10ms

RAM read:
  Send address signal → retrieve data
  Time: <1ms (often microseconds)
```

---

### The Volatility Trade-off

RAM is **volatile** — all data is lost when power is cut or the process restarts. This makes Redis unsuitable as a primary datastore for critical data.

**How Redis solves this:**

Redis offers two persistence mechanisms:

| Mechanism | How It Works | Trade-off |
|---|---|---|
| **RDB (Snapshot)** | Periodically writes a full snapshot of memory to disk | Fast restarts, but may lose recent writes |
| **AOF (Append-Only File)** | Logs every write operation to disk in real time | Minimal data loss, but slower and larger files |

Most production deployments use **both** — AOF for durability, RDB for fast restarts.

```
Redis in memory: { "trending": [...], "session:42": "..." }
          │
          ▼ (every 60 seconds, or per write with AOF)
Snapshot saved to disk
          │
          ▼ (on restart)
Data reloaded from disk → Redis back online ✅
```

---

## IV. Caching Strategies & Policies

### Strategy 1: Lazy Caching (Cache-Aside)

The most common pattern. Data is only cached **when it is first requested**.

```
Request: GET /products/42
         │
         ▼
    Check Redis
         │
    ┌────┴────┐
  HIT       MISS
    │          │
    ▼          ▼
Return     Query Postgres
instantly  → store in Redis (with TTL)
           → return result
```

**Pros:** Cache only contains data that is actually requested — no wasted memory on unused records.
**Cons:** First request always hits the database (cold start penalty). Under sudden traffic spikes on a fresh cache, many requests simultaneously hit the DB — called a **cache stampede**.

---

### Strategy 2: Write-Through

Every database write **simultaneously updates the cache**. The cache is always in sync with the database.

```
Client: Update product 42
         │
         ▼
  Write to Postgres ─── AND ──► Write to Redis
         │
         ▼
    Both updated ✅
```

**Pros:** Cache is always fresh — no stale reads.
**Cons:** Every write operation pays double the cost (DB + cache). For write-heavy workloads, this overhead can negate the performance gains.

---

### Strategy 3: Write-Behind (Write-Back) *(Added)*

Writes go to the cache **first**, and are asynchronously flushed to the database in the background.

```
Client: Update product 42
         │
         ▼
    Write to Redis (instant response to client ✅)
         │
    (background)
         ▼
    Flush to Postgres
```

**Pros:** Extremely fast writes — client doesn't wait for DB.
**Cons:** Risk of data loss if Redis crashes before flushing. Not suitable for financial or critical data.

---

### Eviction Policies

Cache memory is limited. When it fills up, the system must decide **what to delete** to make room for new entries.

| Policy | Full Name | How It Works | Best For |
|---|---|---|---|
| **LRU** | Least Recently Used | Evicts data not accessed for the longest time | General-purpose — most common |
| **LFU** | Least Frequently Used | Evicts data with the lowest total access count | Workloads with clear hot/cold data patterns |
| **TTL** | Time to Live | Automatically expires keys after a set duration | Time-sensitive data (sessions, trending topics) |
| **Random** | — | Evicts a random key | Simple use cases, low overhead |

```
LRU Example — cache capacity: 3 items

Access order: A → B → C → D (new)

Cache state before D: [A, B, C]
LRU evicts A (least recently used)
Cache state after D:  [B, C, D] ✅
```

> **TTL** is not just an eviction policy — it is also a **data freshness guarantee**. Setting `TTL = 60s` on trending topics ensures users never see data older than 60 seconds, even if the background job is delayed.

---

## V. Common Backend Use Cases

### 1. Database Query Caching

Complex SQL joins, aggregations, and analytics queries are expensive. Cache their results for a duration that matches how often the data changes.

```
Query: "Total revenue by region for Q3"
Cost:  Full table scan across 10M rows — 3 seconds

With caching:
  First call  → query DB → store in Redis (TTL: 5 minutes) → 3s
  All calls   → Redis hit → <1ms ✅
```

**Cache key design:**

```
"query:revenue:region:Q3"                  ← simple
"query:products:page:2:limit:20:sort:price" ← parameterized
```

---

### 2. Session Storage

Storing user authentication sessions in Redis allows **instant verification on every API call** without touching the primary database.

```
Login:
  User authenticates → session token generated
  → stored in Redis: { "session:abc123": { userId: 42, role: "admin" } }

Every subsequent request:
  Token "abc123" in header
  → Redis lookup: O(1), <1ms ✅
  → No Postgres query needed
```

Redis is ideal for sessions because:
- Automatic expiry via TTL (sessions expire naturally)
- Shared across all application server instances (works in distributed deployments)
- Fast enough to add negligible overhead to every request

---

### 3. Rate Limiting

Track how many requests a user makes per time window using a Redis counter. Redis is preferred over SQL here because this check happens on **every single incoming request** — latency must be sub-millisecond.

```
Request from IP: 192.168.1.1

Redis: INCR rate:192.168.1.1:minute:2024-01-15-14-32
       → current count: 47

If count <= 100: allow request ✅
If count > 100:  return 429 Too Many Requests ❌

Key expires automatically after 60 seconds (TTL) → counter resets
```

---

## VI. Caching in Spring Boot

### A. Setup — Spring Cache Abstraction + Redis

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  cache:
    type: redis
    redis:
      time-to-live: 300000   # 5 minutes in milliseconds
```

```java
@SpringBootApplication
@EnableCaching   // ← required to activate Spring Cache
public class Application { ... }
```

---

### B. Cache-Aside with `@Cacheable`

Spring's `@Cacheable` implements lazy caching automatically — check cache first, call method only on a miss, store result in cache.

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        // Only called on cache miss — Spring handles the rest
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Cacheable(value = "products:list",
               key = "#page + '-' + #limit + '-' + #sortBy")
    public PagedResponse<Product> list(int page, int limit, String sortBy) {
        return productRepository.findAll(page, limit, sortBy);
    }
}
```

---

### C. Write-Through with `@CachePut`

Updates **both** the cache and the database on every write:

```java
@CachePut(value = "products", key = "#result.id")
public Product update(Long id, UpdateProductRequest request) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product", id));

    product.setName(request.getName());
    product.setPrice(request.getPrice());

    return productRepository.save(product);  // cache updated with return value ✅
}
```

---

### D. Cache Eviction with `@CacheEvict`

Remove stale entries from the cache when data is deleted:

```java
@CacheEvict(value = "products", key = "#id")
public void delete(Long id) {
    productRepository.deleteById(id);
    // Cache entry for this id is evicted automatically ✅
}

// Evict all entries in a cache (e.g., after bulk update)
@CacheEvict(value = "products:list", allEntries = true)
public void bulkUpdate(List<UpdateProductRequest> updates) {
    productRepository.saveAll(updates.stream().map(...).toList());
}
```

---

### E. Session Storage with Spring Session + Redis

```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  session:
    store-type: redis
    timeout: 1800   # session TTL: 30 minutes
```

Spring Session automatically stores and retrieves sessions from Redis — no manual Redis calls needed. Works transparently with Spring Security.

---

### F. Rate Limiting with Redis

```java
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SECONDS = 60;

    public boolean isAllowed(String ipAddress) {
        String key = "rate:" + ipAddress + ":" +
                     LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            // First request in this window — set expiry
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }

        return count <= MAX_REQUESTS;
    }
}

// Usage in a filter
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        if (!rateLimiter.isAllowed(ip)) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);  // 429
            return;
        }

        chain.doFilter(request, response);
    }
}
```

---

### G. Redis Configuration — TTL and Eviction Policy

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))             // global TTL
            .disableCachingNullValues()                  // don't cache nulls
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "products",       config.entryTtl(Duration.ofMinutes(10)),
            "trending",       config.entryTtl(Duration.ofMinutes(5)),
            "sessions",       config.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

> Set eviction policy in Redis config (`redis.conf`):
> ```
> maxmemory 512mb
> maxmemory-policy allkeys-lru   # evict LRU keys when memory is full
> ```

---

### Spring Boot Caching — Quick Reference Checklist

| Concern | Spring Boot Mechanism |
|---|---|
| Enable caching | `@EnableCaching` on main class |
| Cache-aside (lazy) | `@Cacheable(value = "...", key = "...")` |
| Write-through | `@CachePut(value = "...", key = "...")` |
| Evict on delete | `@CacheEvict(value = "...", key = "...")` |
| Evict all entries | `@CacheEvict(allEntries = true)` |
| Per-cache TTL | `RedisCacheManager` with `withInitialCacheConfigurations` |
| Session storage | `spring-session-data-redis` — transparent, no manual calls |
| Rate limiting | `StringRedisTemplate.increment()` + `expire()` |
| Redis eviction policy | `maxmemory-policy allkeys-lru` in `redis.conf` |
| Persistence | Enable RDB + AOF in `redis.conf` for production |
| Null value safety | `.disableCachingNullValues()` in cache config |