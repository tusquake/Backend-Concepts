# Data Skew in Distributed Systems

## What is Data Skew? (One-line)

**Data skew happens when a small portion of data receives a disproportionately large amount of traffic or processing, causing performance bottlenecks.**

## Understanding the Problem

In distributed systems, we aim for even distribution of load across nodes, partitions, or shards. Data skew breaks this assumption by creating "hot spots" where one part of the system becomes overwhelmed while others remain underutilized.

### Visual Representation

```
Ideal Distribution:
┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ Node 1 │  │ Node 2 │  │ Node 3 │  │ Node 4 │
│  25%   │  │  25%   │  │  25%   │  │  25%   │
└────────┘  └────────┘  └────────┘  └────────┘

Data Skew Reality:
┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ Node 1 │  │ Node 2 │  │ Node 3 │  │ Node 4 │
│  90%   │  │   3%   │  │   4%   │  │   3%   │
│  🔥    │  │   😴   │  │   😴   │  │   😴   │
└────────┘  └────────┘  └────────┘  └────────┘
```

---

## Scenario 1: E-commerce Flash Sale ⭐ (Most Common Interview Case)

### Situation

Flipkart runs a Big Billion Day sale:
- **1 product**: iPhone 15
- **Millions of users** simultaneously:
  - Viewing product details
  - Adding to cart
  - Checking availability
  - Placing orders

### What Goes Wrong?

If data is sharded by `productId`:

```
┌─────────────────────────────────┐
│ Shard-1: iPhone 15              │
│ Traffic: 90%                    │
│ Status: 🔥 OVERLOADED           │
│ - High CPU usage                │
│ - Memory pressure               │
│ - Slow queries                  │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Shard-2: Other Products         │
│ Traffic: 10%                    │
│ Status: 😴 IDLE                 │
│ - Low CPU usage                 │
│ - Underutilized resources       │
└─────────────────────────────────┘
```

**This is Data Skew** - One key (iPhone 15) dominates traffic.

### Real Impact

- ❌ Slow response times (3-5 seconds instead of 200ms)
- ❌ Timeout errors
- ❌ Cart addition failures
- ❌ Poor user experience
- ❌ Revenue loss during peak sale period

### Solutions

#### ✅ 1. Key Salting

Add a suffix to distribute hot keys across multiple shards:

```java
// Instead of: productId = "iPhone_15"
// Use:
String salt = String.valueOf(userId.hashCode() % 10);
String partitionKey = "iPhone_15_" + salt;

// This creates:
iPhone_15_0 → Shard-1
iPhone_15_1 → Shard-2
iPhone_15_2 → Shard-3
...
iPhone_15_9 → Shard-10
```

**Result**: Load distributed across 10 shards instead of 1.

#### ✅ 2. Read Replicas for Hot Data

```
Primary DB (Writes)
    ↓
Replica-1 (Reads) ← 33% traffic
Replica-2 (Reads) ← 33% traffic
Replica-3 (Reads) ← 34% traffic
```

#### ✅ 3. Aggressive Caching

```java
// Multi-layer caching strategy
L1: In-memory cache (application level)
    ↓ (miss)
L2: Redis cluster (distributed cache)
    ↓ (miss)
L3: Database read replica

Cache TTL: 5-10 seconds for flash sale items
```

---

## Scenario 2: Database Query Skew ⭐ (Interview Gold)

### Situation

You run this query on an orders table:

```sql
SELECT * FROM orders WHERE status = 'PENDING';
```

### Reality

Data distribution in the database:

```
PENDING    → 5,000,000 rows (90%)
COMPLETED  →   400,000 rows (7%)
FAILED     →   150,000 rows (3%)
```

### What Happens?

- Database performs full table scan on 90% of data
- CPU spikes to 100%
- Query takes 30+ seconds
- Other queries get blocked
- **This is Query Data Skew**

### Impact

```
Query Performance:
SELECT ... WHERE status = 'PENDING'    → 30 seconds ⚠️
SELECT ... WHERE status = 'COMPLETED'  → 0.5 seconds ✅
SELECT ... WHERE status = 'FAILED'     → 0.3 seconds ✅
```

### Solutions

#### ✅ 1. Composite Index

```sql
-- Create compound index
CREATE INDEX idx_status_date ON orders(status, created_at DESC);

-- Query with time range
SELECT * FROM orders 
WHERE status = 'PENDING' 
AND created_at >= NOW() - INTERVAL 7 DAY
LIMIT 100;
```

#### ✅ 2. Table Partitioning

```sql
-- Partition by date
CREATE TABLE orders (
    id BIGINT,
    status VARCHAR(20),
    created_at TIMESTAMP
) PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026)
);
```

#### ✅ 3. Pagination with Cursor

```java
// Instead of scanning millions of rows
String cursor = "2024-01-04T10:00:00";
String query = "SELECT * FROM orders " +
               "WHERE status = 'PENDING' " +
               "AND created_at > ? " +
               "ORDER BY created_at " +
               "LIMIT 100";
```

---

## Scenario 3: Kafka Consumer Lag (Real Production Issue)

### Situation

Kafka topic partitioned by `userId`:

```
Topic: user_events
Partitioning: hash(userId) % numPartitions
```

### Reality

```
Celebrity Account (userId: celebrity_123):
- 5,000,000 events/day
- All go to Partition-2

Regular Users:
- 10,000 events/day total
- Distributed across other partitions
```

### What Happens?

```
┌────────────────────────┐
│ Partition-0            │
│ Consumer-0: ✅ Healthy │
│ Lag: 100 messages      │
└────────────────────────┘

┌────────────────────────┐
│ Partition-1            │
│ Consumer-1: ✅ Healthy │
│ Lag: 150 messages      │
└────────────────────────┘

┌────────────────────────┐
│ Partition-2            │
│ Consumer-2: 🔥 LAGGING │
│ Lag: 50,000 messages   │
│ Processing: 1000 msg/s │
│ Incoming: 5000 msg/s   │
└────────────────────────┘
```

**Data Skew Impact**: One consumer can't keep up, while others are idle.

### Solutions

#### ✅ 1. Composite Partition Key

```java
// Instead of: key = userId
// Use:
String partitionKey = userId + "_" + (timestamp / 3600000);

// This distributes events across time windows
celebrity_123_hour1 → Partition-1
celebrity_123_hour2 → Partition-5
celebrity_123_hour3 → Partition-3
```

#### ✅ 2. Dedicated Topic for High-Volume Users

```java
if (isHighVolumeUser(userId)) {
    producer.send(new ProducerRecord<>("vip_events", event));
} else {
    producer.send(new ProducerRecord<>("regular_events", event));
}

// VIP topic: More partitions, more consumers
vip_events: 20 partitions
regular_events: 5 partitions
```

#### ✅ 3. Dynamic Partition Reassignment

```java
// Monitor consumer lag
if (consumerLag > threshold) {
    // Trigger rebalancing
    kafkaAdmin.reassignPartitions(
        overloadedPartition,
        targetConsumer
    );
}
```

---

## Scenario 4: Redis Hot Key Problem

### Situation

Homepage data stored in Redis:

```redis
Key: homepage_data
Value: { featured_products, banners, deals }
```

### Reality

```
Traffic Pattern:
homepage_data → 100,000 requests/second
other_keys    →   5,000 requests/second

Single Redis node handling homepage_data:
CPU: 95%
Memory: 8GB
Network: Saturated
```

**This is Read Skew** - One key dominates all reads.

### Impact

- Redis latency increases from 1ms to 50ms
- Connection timeouts
- Cascading failures in application
- Users see loading errors

### Solutions

#### ✅ 1. Replicate Key Across Nodes

```java
// Write to all replicas
for (int i = 0; i < numReplicas; i++) {
    String replicaKey = "homepage_data_replica_" + i;
    redis.set(replicaKey, data);
}

// Read from random replica
int replica = random.nextInt(numReplicas);
String key = "homepage_data_replica_" + replica;
Data data = redis.get(key);
```

#### ✅ 2. Local In-Memory Cache

```java
// Application-level cache
LoadingCache<String, Data> localCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .build(key -> fetchFromRedis(key));

// Redis is now fallback, not primary
Data data = localCache.get("homepage_data");
```

#### ✅ 3. CDN for Static Data

```
User Request
    ↓
CDN Edge (cache hit) → Return in 10ms ✅
    ↓ (cache miss)
Application Server
    ↓
Redis (only for dynamic content)
```

---

## Scenario 5: Big Data / Spark Job Failure

### Situation

Spark job processing user data, grouped by `country`:

```scala
val userData = spark.read.parquet("users")
val countryStats = userData.groupBy("country").count()
```

### Reality

```
Data Distribution:
India  → 700 million records (70%)
USA    → 150 million records (15%)
Others → 150 million records (15%)
```

### What Happens?

```
Executor-1 (India data):
- 700M records
- Processing time: 60 minutes
- Status: 🔥 Bottleneck

Executor-2 (USA data):
- 150M records
- Processing time: 10 minutes
- Status: ✅ Complete, now idle

Executor-3 (Others):
- 150M records
- Processing time: 10 minutes
- Status: ✅ Complete, now idle
```

**Job waits for slowest executor** → 60 minutes total time.

### Solutions

#### ✅ 1. Custom Partitioner with Salting

```scala
// Add salt to skewed keys
val saltedData = userData.withColumn(
  "salted_country",
  when(col("country") === "India", 
       concat(col("country"), lit("_"), (rand() * 10).cast("int")))
  .otherwise(col("country"))
)

// Now India data split across 10 partitions
val stats = saltedData.groupBy("salted_country").count()
```

#### ✅ 2. Adaptive Query Execution

```scala
// Enable AQE in Spark 3.x
spark.conf.set("spark.sql.adaptive.enabled", "true")
spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "true")
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")

// Spark automatically handles skew
```

#### ✅ 3. Two-Stage Aggregation

```scala
// Stage 1: Partial aggregation with salting
val partial = userData
  .withColumn("salt", (rand() * 10).cast("int"))
  .groupBy("country", "salt")
  .count()

// Stage 2: Final aggregation
val final = partial
  .groupBy("country")
  .agg(sum("count"))
```

---

## Scenario 6: Authentication System (Login Spike)

### Situation

During exam season:
- 5 million students login simultaneously
- Time window: 9:00 AM - 9:05 AM (5 minutes)
- Same authentication endpoint
- Same user database table

### What Happens?

```
Auth Database:
Table: users
Index: idx_username
Lock contention: HIGH

Request Pattern:
Login requests: 16,666 per second
Each request:
- SELECT from users table
- UPDATE last_login timestamp
- INSERT into sessions table

Result:
- Row-level locks
- Index contention
- Connection pool exhaustion
- 5-10 second login times
```

### Solutions

#### ✅ 1. Rate Limiting

```java
// Token bucket algorithm
RateLimiter loginRateLimiter = RateLimiter.create(1000.0); // 1000 req/s

@PostMapping("/login")
public Response login(@RequestBody Credentials creds) {
    if (!loginRateLimiter.tryAcquire()) {
        return Response.status(429).entity("Too many requests").build();
    }
    // Process login
}
```

#### ✅ 2. Cache Authentication Tokens

```java
// Cache user credentials hash
String userKey = "user:" + username;
String cachedHash = redis.get(userKey);

if (cachedHash != null && bcrypt.matches(password, cachedHash)) {
    // Skip database query
    return generateToken(username);
}

// Only hit DB on cache miss
User user = database.findByUsername(username);
redis.setex(userKey, 300, user.getPasswordHash()); // Cache for 5 mins
```

#### ✅ 3. Queue-Based Login Handling

```java
// Add to queue instead of processing immediately
@PostMapping("/login")
public Response login(@RequestBody Credentials creds) {
    String requestId = UUID.randomUUID().toString();
    
    // Add to Kafka/RabbitMQ
    loginQueue.add(new LoginRequest(requestId, creds));
    
    return Response.accepted()
        .entity(new LoginStatus(requestId, "QUEUED"))
        .build();
}

// Background workers process queue at controlled rate
@KafkaListener(topics = "login-requests")
public void processLogin(LoginRequest request) {
    // Process with controlled concurrency
}
```

---

## How to Answer in Interviews 💯

### Perfect Interview Answer Template

> "Data skew occurs when certain keys or values dominate traffic or computation, leading to uneven load distribution. This creates hot spots where one part of the system becomes overwhelmed while others remain underutilized.
> 
> I've encountered this in several scenarios:
> - **E-commerce flash sales** where one product receives 90% of traffic
> - **Kafka partitions** where celebrity accounts create consumer lag
> - **Redis hot keys** where homepage data saturates a single node
> - **Analytical queries** where one filter condition matches most data
> 
> Common solutions include:
> - **Key salting** to distribute hot keys across multiple partitions
> - **Better partitioning strategies** using composite keys
> - **Caching hot data** aggressively at multiple layers
> - **Isolating high-traffic entities** into separate systems
> - **Rate limiting** and queuing for traffic spikes
> 
> The key is identifying skewed access patterns early through monitoring and choosing the right strategy based on whether it's read-heavy, write-heavy, or compute-intensive."

---

## Detection and Monitoring

### Key Metrics to Track

```
1. Partition/Shard Metrics:
   - Request rate per partition
   - CPU usage per node
   - Memory usage distribution
   - Network throughput variance

2. Database Metrics:
   - Query execution time by filter
   - Index scan vs table scan ratio
   - Lock wait time
   - Connection pool usage

3. Cache Metrics:
   - Hit rate by key pattern
   - Eviction rate
   - Memory usage per key
   - Network bandwidth per node

4. Stream Processing:
   - Consumer lag by partition
   - Processing rate variance
   - Rebalance frequency
```

### Example Monitoring Query

```sql
-- Detect hot partitions in Kafka
SELECT 
    partition_id,
    COUNT(*) as message_count,
    MAX(timestamp) - MIN(timestamp) as time_span
FROM kafka_messages
WHERE timestamp > NOW() - INTERVAL 1 HOUR
GROUP BY partition_id
HAVING COUNT(*) > (SELECT AVG(cnt) * 2 
                   FROM (SELECT COUNT(*) as cnt 
                         FROM kafka_messages 
                         GROUP BY partition_id) t);
```

---

## Quick Reference: Skew Types and Solutions

| Skew Type | Cause | Primary Impact | Best Solution |
|-----------|-------|----------------|---------------|
| **Read Skew** | Hot keys in cache/DB | High latency, timeouts | Replication + Caching |
| **Write Skew** | Hot partition for writes | Lock contention, slow writes | Key salting, Sharding |
| **Computational Skew** | Uneven data distribution | Slow jobs, resource waste | Custom partitioning, AQE |
| **Temporal Skew** | Traffic spikes | System overload | Rate limiting, Queuing |
| **Query Skew** | Selective conditions | Slow queries | Better indexes, Partitioning |

---

## One-Line Memory Tricks 🧠

1. **"Data skew is when one VIP customer keeps an entire system busy."**

2. **"If one waiter serves 90% of customers, that's data skew in your restaurant."**

3. **"Hot keys are like traffic jams - everyone wants the same route."**

4. **"Skew = Some do all, most do nothing."**

---

## Conclusion

Data skew is a critical challenge in distributed systems that can severely impact performance, user experience, and system reliability. The key to handling it effectively is:

1. **Early Detection**: Monitor for uneven load distribution
2. **Right Strategy**: Choose solutions based on read/write/compute patterns
3. **Multi-Layer Defense**: Combine caching, partitioning, and isolation
4. **Continuous Tuning**: Adapt as traffic patterns change

Understanding data skew and its solutions demonstrates deep knowledge of distributed systems, making it a favorite topic in system design interviews at companies like Google, Amazon, Meta, and Netflix.