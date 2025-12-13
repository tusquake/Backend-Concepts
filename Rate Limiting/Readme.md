# Rate Limiter Algorithms - Complete Guide

## 📖 Introduction

Rate limiting is a technique used to control the rate of requests a user or service can make to an API or system within a specified time window. It's essential for:

- **Preventing abuse** and DDoS attacks
- **Ensuring fair resource usage** among users
- **Maintaining system stability** under high load
- **Protecting backend services** from overload
- **Managing costs** for paid APIs

---

## 🔹 1. Fixed Window Counter

### 🧠 How It Works

Time is divided into **fixed windows** of a specific duration (e.g., 1 second, 1 minute). Each user has a counter that tracks requests made in the current window.

**Algorithm Steps:**
1. Check current time window (e.g., current second)
2. Get request count for this user in current window
3. If `count < limit` → Allow request, increment counter
4. If `count >= limit` → Block request
5. When window ends → Reset counter to 0

### ✅ Example

**Limit:** 3 requests per second

**Timeline:**
```
12:00:05.100 → Request 1 ✅ (count: 1)
12:00:05.200 → Request 2 ✅ (count: 2)
12:00:05.400 → Request 3 ✅ (count: 3)
12:00:05.500 → Request 4 ❌ (limit exceeded)
12:00:06.000 → Counter resets (count: 0)
12:00:06.100 → Request 5 ✅ (count: 1)
```

### 🌍 Real-World Analogy

**Restaurant Buffet System:**
- You're allowed **3 plates per minute**
- Try to grab a 4th plate in the same minute → Waiter blocks you
- When the next minute starts → Your plate counter resets to 0

### ⚖️ Pros & Cons

**Pros:**
- ✅ Simple to implement and understand
- ✅ Memory efficient (only stores counter per user)
- ✅ Fast performance (O(1) operations)
- ✅ Easy to scale with distributed counters (Redis)

**Cons:**
- ❌ **Burst problem at window boundaries**
  - Example: 3 requests at 12:00:00.999, 3 more at 12:00:01.001 = 6 requests in 2ms
- ❌ Unfair for users making requests near window boundaries
- ❌ Can cause "thundering herd" at window reset

### 💻 Use Cases

- Simple APIs with moderate traffic
- When approximate rate limiting is acceptable
- Systems prioritizing performance over precision
- Internal microservices rate limiting

---

## 🔹 2. Sliding Window Log

### 🧠 How It Works

Maintains a **log of timestamps** for all requests. For each new request, the algorithm checks how many requests occurred in the past time window.

**Algorithm Steps:**
1. Record timestamp of new request
2. Remove all timestamps older than `(current_time - window_size)`
3. Count remaining timestamps in the log
4. If `count < limit` → Allow request, add timestamp to log
5. If `count >= limit` → Block request

### ✅ Example

**Limit:** 3 requests per second (1000ms window)

**Timeline:**
```
12:00:05.100 → Request 1 ✅ (log: [100])
12:00:05.200 → Request 2 ✅ (log: [100, 200])
12:00:05.400 → Request 3 ✅ (log: [100, 200, 400])
12:00:05.900 → Request 4 ❌ (log still has 4 requests)
12:00:06.200 → Request 5 ✅ (timestamp 100 expired, log: [200, 400, 900, 1200])
```

### 🌍 Real-World Analogy

**Office Coffee Machine:**
- Allows **3 cups per rolling minute**
- Coffee at `9:00:05`, `9:00:15`, `9:00:25`
- At `9:00:50` → Machine says "limit reached"
- At `9:01:06` → Your `9:00:05` coffee no longer counts, you can get another

### ⚖️ Pros & Cons

**Pros:**
- ✅ No burst problem - smooth and fair rate limiting
- ✅ Accurate tracking of requests
- ✅ Works perfectly for sliding time windows
- ✅ Fair for all users regardless of timing

**Cons:**
- ❌ High memory usage (stores all timestamps)
- ❌ Slower than fixed window (requires log cleanup)
- ❌ Can be expensive at scale (many users × many requests)
- ❌ Requires sorted data structure for efficiency

### 💻 Use Cases

- High-value APIs requiring precise rate limiting
- Premium/paid API tiers
- Security-critical systems
- When fairness is more important than performance

---

## 🔹 3. Token Bucket

### 🧠 How It Works

Each user has a **bucket of tokens** that refills at a constant rate. Each request consumes one token. When the bucket is empty, requests are rejected.

**Algorithm Steps:**
1. Calculate tokens to add: `(current_time - last_refill_time) × refill_rate`
2. Add tokens to bucket (max = bucket capacity)
3. If `bucket >= 1` → Allow request, consume 1 token
4. If `bucket < 1` → Block request
5. Update `last_refill_time`

### ✅ Example

**Configuration:**
- Bucket capacity: 5 tokens
- Refill rate: 2 tokens/second

**Timeline:**
```
Time 0.0s → Bucket: 5 tokens
  Request 1 ✅ → Bucket: 4 tokens
  Request 2 ✅ → Bucket: 3 tokens
  Request 3 ✅ → Bucket: 2 tokens
  
Time 0.5s → Refill: +1 token → Bucket: 3 tokens
  Request 4 ✅ → Bucket: 2 tokens
  Request 5 ✅ → Bucket: 1 token
  Request 6 ✅ → Bucket: 0 tokens
  Request 7 ❌ → Bucket: 0 tokens (blocked)
  
Time 1.0s → Refill: +1 token → Bucket: 1 token
  Request 8 ✅ → Bucket: 0 tokens
```

### 🌍 Real-World Analogy

**Parking Lot:**
- Parking lot has **5 spaces** (bucket capacity)
- Each car needs **1 space** (1 token per request)
- Every 30 minutes, **1 car leaves** (token refill)
- When lot is full → New cars must wait
- Ensures parking never gets overwhelmed

### ⚖️ Pros & Cons

**Pros:**
- ✅ Handles burst traffic elegantly (up to bucket size)
- ✅ Memory efficient (only stores bucket state)
- ✅ Flexible - balances bursts and sustained rate
- ✅ Industry standard (used by AWS, Stripe, etc.)

**Cons:**
- ❌ More complex than fixed window
- ❌ Requires careful tuning of bucket size and refill rate
- ❌ Can be difficult to reason about for users
- ❌ Potential for race conditions in distributed systems

### 💻 Use Cases

- Cloud APIs (AWS API Gateway uses this)
- Payment processing APIs (Stripe)
- Systems needing burst tolerance
- When smooth traffic shaping is desired

---

## 🔹 4. Leaky Bucket

### 🧠 How It Works

Similar to token bucket, but focuses on **outgoing traffic rate**. Requests are queued and processed at a fixed rate, like water leaking from a bucket.

**Algorithm Steps:**
1. Add incoming request to queue
2. Process requests from queue at fixed rate
3. If queue is full → Reject request
4. Ensures output rate is constant

### ✅ Example

**Configuration:**
- Queue size: 5 requests
- Process rate: 1 request/second

**Timeline:**
```
Time 0.0s → 5 requests arrive → Queue: [R1, R2, R3, R4, R5]
Time 0.0s → 6th request ❌ (queue full)
Time 1.0s → Process R1 ✅ → Queue: [R2, R3, R4, R5]
Time 1.0s → New request arrives ✅ → Queue: [R2, R3, R4, R5, R6]
Time 2.0s → Process R2 ✅ → Queue: [R3, R4, R5, R6]
```

### ⚖️ Pros & Cons

**Pros:**
- ✅ Smooth output rate
- ✅ Good for network traffic shaping
- ✅ Prevents system overload

**Cons:**
- ❌ Adds latency (queuing delay)
- ❌ Queue management overhead
- ❌ Not suitable for real-time APIs

### 💻 Use Cases

- Network traffic shaping
- Video streaming rate control
- Background job processing

---

## 🔹 5. Sliding Window Counter (Hybrid)

### 🧠 How It Works

Combines **Fixed Window** and **Sliding Window Log** for better accuracy without high memory cost.

**Formula:**
```
Rate = (prev_window_count × overlap_percentage) + current_window_count
```

### ✅ Example

**Limit:** 10 requests per minute

**Scenario at 12:00:30 (30 seconds into current minute):**
- Previous window (11:59): 8 requests
- Current window (12:00): 4 requests
- Overlap: 50% (30 seconds)

**Calculation:**
```
Rate = (8 × 0.5) + 4 = 4 + 4 = 8 requests
Result: ✅ Allow (under limit of 10)
```

### ⚖️ Pros & Cons

**Pros:**
- ✅ Better than fixed window (no burst problem)
- ✅ More memory efficient than sliding log
- ✅ Good balance of accuracy and performance

**Cons:**
- ❌ Still approximate (not as accurate as sliding log)
- ❌ More complex implementation

### 💻 Use Cases

- High-traffic APIs needing good accuracy
- Systems with memory constraints
- When precise sliding window is too expensive

---

## 📊 Algorithm Comparison

| Algorithm | Memory | Accuracy | Performance | Burst Handling | Complexity |
|-----------|--------|----------|-------------|----------------|------------|
| **Fixed Window** | Low | Moderate | Excellent | Poor | Simple |
| **Sliding Log** | High | Excellent | Good | Excellent | Moderate |
| **Token Bucket** | Low | Good | Excellent | Excellent | Moderate |
| **Leaky Bucket** | Moderate | Excellent | Good | Poor | Moderate |
| **Sliding Counter** | Low | Good | Excellent | Good | Moderate |

---

## 🎯 Choosing the Right Algorithm

### Use **Fixed Window** when:
- Simple internal APIs
- Performance is critical
- Approximate limiting is acceptable
- Low resource usage is priority

### Use **Sliding Window Log** when:
- Precision is critical
- Premium/paid API tiers
- Security-sensitive operations
- Fairness is more important than cost

### Use **Token Bucket** when:
- Need to allow bursts
- Industry-standard behavior expected
- Cloud/payment APIs
- Balance between flexibility and control

### Use **Leaky Bucket** when:
- Need constant output rate
- Network traffic shaping
- Background job processing

### Use **Sliding Window Counter** when:
- Need better accuracy than fixed window
- Memory is constrained
- High traffic volume

---

## 🛠️ Implementation Considerations

### Distributed Systems

**Challenges:**
- Race conditions across multiple servers
- Clock synchronization issues
- Network latency

**Solutions:**
- Use centralized storage (Redis, Memcached)
- Implement distributed locks
- Use Lua scripts for atomic operations
- Consider eventual consistency trade-offs

### Storage Options

| Storage | Pros | Cons |
|---------|------|------|
| **Redis** | Fast, atomic operations, TTL support | Single point of failure (without cluster) |
| **Memory** | Fastest, no network overhead | Lost on restart, not distributed |
| **Database** | Persistent, scalable | Slower, more overhead |

### Key Design Decisions

1. **Granularity:** Per user? Per IP? Per API key?
2. **Scope:** Global? Per endpoint? Per resource?
3. **Response:** Block (429 error)? Queue? Throttle?
4. **Headers:** Return rate limit info to clients?
5. **Bypass:** Allow whitelist for critical users?

---

## 🌐 Industry Examples

- **Twitter API:** Token bucket (15 requests per 15-min window)
- **GitHub API:** Sliding window (5000 requests per hour)
- **Stripe API:** Token bucket with different rates per endpoint
- **AWS API Gateway:** Token bucket (burst + sustained rate)
- **Cloudflare:** Multiple algorithms based on plan

---

## 💡 Summary

Rate limiting is essential for building robust, scalable APIs. The choice of algorithm depends on your specific requirements:

- **Simple & fast?** → Fixed Window
- **Precise & fair?** → Sliding Window Log
- **Flexible with bursts?** → Token Bucket
- **Smooth output?** → Leaky Bucket
- **Balanced approach?** → Sliding Window Counter

Most production systems use **Token Bucket** or **Sliding Window Counter** as they provide the best balance of accuracy, performance, and user experience.