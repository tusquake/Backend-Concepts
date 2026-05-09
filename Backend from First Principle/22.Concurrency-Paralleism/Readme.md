# # Concurrency & Async Programming — Complete Guide

## I. The Core Problem: The Cost of Waiting

A typical backend server does not spend most of its time computing — it spends most of its time **waiting**.

### The Idle CPU

```
A single API request timeline:

  0ms      ├── Parse request, authenticate          (CPU active ~0.1ms)
  0.1ms    ├── Query database ───────────────────── (CPU idle, waiting...)
           │                                                │
           │         DB processes query on its machine      │
           │         Network round trip adds latency         │
           │                                                │
  100ms    ├── DB response arrives                  (CPU active ~0.1ms)
  100.1ms  ├── Serialize response, send             (CPU active ~0.1ms)
  100.2ms  └── Done

  Total time:    100.2ms
  CPU active:      0.2ms   (0.2%)
  CPU idle:       100ms    (99.8%) ← waiting for IO
```

```
What the CPU could have done in those 100ms:

  CPU speed: ~3 billion instructions per second
  3,000,000 instructions per millisecond × 100ms = 300,000,000 instructions wasted

  That is the cost of one synchronous DB query on one idle thread.
```

### The IO Bottleneck

In a standard API call, up to **95% of the time** is spent on IO — waiting for responses from databases, caches, or external APIs. The CPU is the fastest component in the system, yet it spends almost all of its time waiting on the slowest ones.

```
Time breakdown of a typical API request:

  ████████████████████████████████████████████░░ 95% IO wait
                                               ██  5% actual computation

  IO sources:
    ├── Database query        20ms – 100ms
    ├── External API call     50ms – 500ms
    ├── Cache lookup (Redis)  1ms – 5ms
    └── File system read      5ms – 50ms
```

> **The core insight:** The problem is not that the CPU is too slow — it is that the CPU sits idle while IO completes. The goal of concurrency is to keep the CPU productive during that idle time.

---

## II. Concurrency vs. Parallelism

These terms describe fundamentally different mechanics and are frequently confused.

```
Parallelism — Doing things at the exact same moment:

  Core 1:  ████ Task A ████
  Core 2:  ████ Task B ████
           ──────────────── time →
  Requires multiple CPU cores. True simultaneous execution.

Concurrency — Dealing with many things by interleaving:

  Core 1:  ██ Task A ██░░░░░░██ Task A ██
                       ││
           ░░░░░░░░████ Task B ████░░░░░
           ──────────────────────────── time →
  Works on a single core. Tasks take turns; none block the others.
  Gives the illusion of simultaneous execution.
```

| | Concurrency | Parallelism |
|---|---|---|
| Cores required | One (or more) | Multiple |
| Mechanism | Interleaving tasks (start, pause, resume) | Simultaneous execution |
| Solves | IO-bound bottlenecks | CPU-bound bottlenecks |
| Analogy | One chef juggling multiple pots | Multiple chefs each at their own pot |

> **The chef analogy:** A chef making three dishes does not stand at the stove watching water boil — they start the pasta, chop vegetables while it heats, stir the sauce, and come back when the water is ready. That is concurrency. A second chef working in parallel on a separate dish at the same time — that is parallelism.

---

## III. IO-Bound vs. CPU-Bound Workloads

The right concurrency strategy depends entirely on what is limiting your program's performance.

### IO-Bound Workloads

Limited by the speed of **external resources** — network, database, file system.

```
Example: Web server handling API requests

  Request 1 → DB query (waiting 80ms) ←─────────────────────── IO bound
  Request 2 → External API call (waiting 200ms) ←────────────── IO bound
  Request 3 → Cache lookup (waiting 3ms) ←───────────────────── IO bound

  CPU is idle most of the time.
  Solution: Concurrency — keep the CPU busy by handling other requests
            while waiting for each IO operation to complete.
```

**IO-bound examples:**

| Workload | IO Source | Concurrency Benefit |
|---|---|---|
| REST API server | Database, cache, external APIs | High — 95%+ time is IO |
| Web scraper | Network requests | High — waiting on remote servers |
| File processor | Disk reads/writes | High — disk is slow |
| Email sender | SMTP / email API | High — network-bound |

---

### CPU-Bound Workloads

Limited by the **CPU's processing power** — no IO waiting, just raw computation.

```
Example: Image resizing pipeline

  Image upload → decompress → resize → re-encode → save
                 ─────────────────────
                 All CPU work, no waiting
                 One core can only go so fast

  Solution: Parallelism — distribute work across multiple CPU cores
            to crunch numbers simultaneously.
```

**CPU-bound examples:**

| Workload | Why CPU-bound | Parallelism Benefit |
|---|---|---|
| Image / video encoding | Pixel math on large files | High — split frames across cores |
| Cryptographic hashing | Heavy math per operation | High — parallelise independent hashes |
| ML model inference | Matrix multiplication | High — GPUs are massively parallel |
| PDF generation | Layout + rendering computation | Moderate |

```
Choosing the right tool:

  IO-bound  → Concurrency (async/await, event loop, virtual threads)
  CPU-bound → Parallelism (multiple OS threads, worker processes, GPU)
  Mixed     → Both: async for IO, thread pool for CPU-heavy steps
```

---

## IV. Concurrency Models

The industry relies on three primary models, each with different tradeoffs in memory, overhead, and complexity.

---

### A. The Threading Model

The OS manages independent execution units called **threads**. Each request gets its own thread; the OS scheduler switches between them.

```
Thread-per-request model:

  Request 1 → OS creates Thread 1 (1MB–8MB stack)
  Request 2 → OS creates Thread 2 (1MB–8MB stack)
  Request 3 → OS creates Thread 3 (1MB–8MB stack)
  ...
  Request 10,000 → OS creates Thread 10,000
                   → 10,000 × 4MB = 40GB RAM ❌ — server runs out of memory
```

#### Preemptive Scheduling

The OS does not wait for a thread to voluntarily pause — it **forcibly interrupts** it after a fixed time slice.

```
Time slice scheduling (e.g., 2ms slices):

  0ms:    Thread A starts
  2ms:    OS pauses Thread A → saves its CPU registers (context switch)
  2ms:    Thread B starts
  4ms:    OS pauses Thread B → saves its CPU registers (context switch)
  4ms:    Thread A resumes from exactly where it left off
  ...

  Context switch cost: ~1–10 microseconds per switch
  At 10,000 threads:  constant context switching overhead ❌
```

#### Threading Costs

| Cost | Detail |
|---|---|
| Memory per thread | 1MB – 8MB stack (OS-managed) |
| Context switch | Save/restore all CPU registers on every switch |
| Scheduling overhead | OS scheduler must track thousands of threads |
| Practical limit | ~hundreds to low thousands of concurrent threads |

**When threads work well:** CPU-bound work where you want true parallel execution across cores, or when you have a bounded, manageable number of concurrent operations.

---

### B. The Event Loop Model

Used by runtimes like **Node.js**. A single thread handles all requests by registering callbacks and never blocking.

```
Event loop — single thread, many concurrent requests:

  Thread: ── handle req1 ── register DB callback ──►
              (2ms CPU work)  (hands off to OS, continues immediately)
                          ── handle req2 ── register API callback ──►
                              (2ms CPU work)
                          ── handle req3 ──►
                              (2ms CPU work)
                          ── req1 DB response arrives ──►
                              (2ms CPU work, send response)
                          ── req2 API response arrives ──►
                              (2ms CPU work, send response)

  One thread handled 3 requests concurrently. ✅
  No context switching. No per-request memory overhead.
```

```
The event loop lifecycle:

  ┌─────────────────────────────────┐
  │          Event Queue            │
  │  [req1 callback] [req2 timer]   │
  └─────────────────────────────────┘
              │
              ▼
  ┌─────────────────────────────────┐
  │         Event Loop              │◄── single thread
  │  1. Take next event             │
  │  2. Execute its callback        │
  │  3. Register any new callbacks  │
  │  4. Go back to step 1           │
  └─────────────────────────────────┘
              │
              ▼
  OS / Thread Pool (libuv in Node.js)
  handles actual IO operations asynchronously
```

#### The Critical Rule: Never Block the Event Loop

```
❌ Blocking operation on the event loop:

  Event loop: handling req1
    → calls: for (let i = 0; i < 1_000_000_000; i++) {}  ← CPU-heavy loop
    → event loop is stuck for 3 seconds

  During those 3 seconds:
    req2, req3, req4, req5... all wait in the queue
    Server appears completely frozen to all users ❌

✅ CPU-heavy work → offload to a worker thread pool
   Event loop stays free to handle IO callbacks
```

---

### C. Virtual Threads (Go Routines)

Go's runtime maps thousands of **lightweight goroutines** onto a small pool of OS threads — combining the simplicity of the threading model with the efficiency of the event loop.

```
OS Thread model (Java/traditional):

  OS Thread:  [1MB–8MB stack each]
  10,000 requests → 10,000 OS threads → ~40GB RAM ❌

Go Routine model:

  OS Threads: [4] ← small, fixed number managed by Go runtime
                │
  Go Routines: [goroutine 1] [goroutine 2] ... [goroutine 100,000]
               [2KB–8KB stack each, grows dynamically]
                │
  Go runtime scheduler maps goroutines → OS threads
  When a goroutine blocks on IO, Go runtime moves it off the thread
  and puts another goroutine on → OS thread never sits idle ✅

  10,000 goroutines × 4KB = 40MB RAM ✅ (vs 40GB for OS threads)
```

```
Go routine lifecycle:

  HTTP request arrives
        │
        ▼
  go handleRequest(req)   ← spins up a goroutine (cheap, ~2KB)
        │
  Goroutine makes DB query
        │
  Go runtime: "this goroutine is waiting for IO"
        │
  Go runtime parks goroutine → puts another on the OS thread
        │
  DB responds → goroutine is rescheduled → continues ✅

  Result: One OS thread handles thousands of goroutines efficiently
```

### Model Comparison

| Model | Memory per task | Context switch | IO handling | CPU-bound |
|---|---|---|---|---|
| OS Threads | 1MB – 8MB | Expensive (kernel) | Blocks thread | Good (true parallel) |
| Event Loop | Minimal (shared) | None | Non-blocking callbacks | Dangerous (blocks all) |
| Virtual Threads / Goroutines | 2KB – 8KB | Cheap (runtime) | Parks goroutine | Good (via OS threads) |

---

## V. Async/Await as a State Machine

`async` and `await` are **syntactic sugar** — they make asynchronous code look synchronous, but the compiler transforms them into something fundamentally different under the hood.

```
What you write:

  async function fetchUserOrders(userId) {
    const user   = await db.findUser(userId);    // await point 1
    const orders = await db.findOrders(userId);  // await point 2
    return { user, orders };
  }

What the compiler actually generates (conceptually):

  State 0: start → call db.findUser() → register callback → suspend
                                                              │
                                                         event loop
                                                         handles other work
                                                              │
  State 1: db.findUser() returned → store result → call db.findOrders() → suspend
                                                                            │
                                                                       event loop
                                                                       handles other work
                                                                            │
  State 2: db.findOrders() returned → store result → return { user, orders } → done
```

```
Every await point is a state transition:

  ┌─────────┐  await db.findUser()   ┌─────────┐  await db.findOrders()  ┌─────────┐
  │ State 0 │ ──────────────────────► │ State 1 │ ───────────────────────► │ State 2 │
  │  Start  │  suspend, free thread   │  Got    │  suspend, free thread    │  Done   │
  └─────────┘                         │  user   │                          └─────────┘
                                      └─────────┘

  Between states: the thread is free to handle other requests ✅
  The function "remembers" exactly where it was when IO completes.
```

```java
// Spring Boot — reactive async with WebFlux
@GetMapping("/users/{id}/orders")
public Mono<UserOrders> getUserOrders(@PathVariable Long id) {
    return userRepository.findById(id)              // async, non-blocking
        .zipWith(orderRepository.findByUserId(id))  // runs both concurrently ✅
        .map(tuple -> new UserOrders(
            tuple.getT1(),
            tuple.getT2()
        ));
}

// Spring Boot — @Async for fire-and-forget
@Async
public CompletableFuture<Void> sendWelcomeEmail(String email) {
    emailClient.send(email);                        // runs in background thread ✅
    return CompletableFuture.completedFuture(null);
}
```

---

## VI. The Concurrency Trap: Race Conditions

Concurrency introduces a new class of bugs around **shared mutable state** — when two threads read and modify the same data simultaneously.

### The Read-Modify-Write Problem

```
Shared counter, starting at 0. Two threads increment it simultaneously.

  Expected result: 0 + 1 + 1 = 2

  What actually happens:

  Thread A:  READ counter  → gets 0
  Thread B:  READ counter  → gets 0    ← reads before A has written back
  Thread A:  ADD 1         → calculates 1
  Thread B:  ADD 1         → calculates 1
  Thread A:  WRITE 1       → counter = 1
  Thread B:  WRITE 1       → counter = 1   ← overwrites A's result ❌

  Final value: 1 (should be 2) — one increment was silently lost ❌
```

This is a **race condition** — the result depends on the unpredictable timing of thread scheduling.

```
Real-world consequences:

  ├── Bank account: two simultaneous withdrawals both read balance $100
  │   Both approve $80 withdrawal → account goes to $20 instead of -$60
  │   (or worse, both write back $20 and $100 is lost) ❌
  │
  ├── Inventory: two users buy the last item simultaneously
  │   Both see stock = 1, both decrement → stock = -1 ❌
  │
  └── Session counter: 1000 concurrent increments → result is 743 ❌
```

### The Fix: Mutual Exclusion with Locks (Mutexes)

A **mutex** (mutual exclusion lock) ensures only one thread can enter a critical section at a time.

```
With a lock:

  Thread A acquires lock ✅
  Thread A: READ counter  → 0
  Thread A: ADD 1         → 1
  Thread A: WRITE 1       → counter = 1
  Thread A releases lock

  Thread B was waiting...
  Thread B acquires lock ✅
  Thread B: READ counter  → 1   ← reads the updated value ✅
  Thread B: ADD 1         → 2
  Thread B: WRITE 2       → counter = 2 ✅
  Thread B releases lock

  Final value: 2 ✅ — both increments counted correctly
```

```java
// Java — synchronized block (intrinsic lock)
private int counter = 0;
private final Object lock = new Object();

public void increment() {
    synchronized (lock) {         // only one thread enters at a time
        counter++;
    }                             // lock released automatically
}

// Java — ReentrantLock (more control)
private final ReentrantLock lock = new ReentrantLock();

public void increment() {
    lock.lock();
    try {
        counter++;
    } finally {
        lock.unlock();            // always release in finally ✅
    }
}

// Java — AtomicInteger (lock-free for simple counters)
private final AtomicInteger counter = new AtomicInteger(0);

public void increment() {
    counter.incrementAndGet();    // atomic read-modify-write, no explicit lock ✅
}
```

### Deadlock: When Locks Go Wrong

```
Thread A holds Lock 1, waits for Lock 2
Thread B holds Lock 2, waits for Lock 1

  Thread A: ──► acquired Lock 1 ──► waiting for Lock 2 ──► (blocked forever)
  Thread B: ──► acquired Lock 2 ──► waiting for Lock 1 ──► (blocked forever)

  Both threads wait forever. Server hangs. ❌

Prevention:
  ├── Always acquire locks in the same order across all threads
  ├── Use tryLock() with a timeout instead of blocking indefinitely
  └── Minimize the number of locks held simultaneously
```

---

## VII. Spring Boot Concurrency Reference

### Thread Pool Configuration

```java
// Configure the async thread pool
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);         // always-alive threads
        executor.setMaxPoolSize(50);          // max under heavy load
        executor.setQueueCapacity(200);       // tasks queued before rejecting
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
```

### Parallel IO with CompletableFuture

```java
// ❌ Sequential — total time = A + B + C
User user     = userRepository.findById(id);         // wait 50ms
List<Order> orders = orderRepository.findByUser(id); // wait 50ms
List<Review> reviews = reviewRepository.findByUser(id); // wait 50ms
// Total: 150ms

// ✅ Parallel — total time = max(A, B, C)
CompletableFuture<User>         userFuture    = CompletableFuture.supplyAsync(() -> userRepository.findById(id));
CompletableFuture<List<Order>>  orderFuture   = CompletableFuture.supplyAsync(() -> orderRepository.findByUser(id));
CompletableFuture<List<Review>> reviewFuture  = CompletableFuture.supplyAsync(() -> reviewRepository.findByUser(id));

CompletableFuture.allOf(userFuture, orderFuture, reviewFuture).join();
// All three run simultaneously → total: ~50ms ✅
```

### Thread-Safe Shared State

```java
@Service
public class RequestMetrics {

    // ✅ Atomic — thread-safe counter without explicit locking
    private final AtomicLong requestCount   = new AtomicLong(0);
    private final AtomicLong errorCount     = new AtomicLong(0);

    // ✅ ConcurrentHashMap — thread-safe map
    private final ConcurrentHashMap<String, Long> endpointCounts = new ConcurrentHashMap<>();

    public void recordRequest(String endpoint) {
        requestCount.incrementAndGet();
        endpointCounts.merge(endpoint, 1L, Long::sum);
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }
}
```

---

## VIII. Quick Reference Checklist

| Concern | Strategy |
|---|---|
| CPU idle during DB / API calls | Concurrency — don't block, handle other requests |
| True simultaneous computation | Parallelism — multiple CPU cores |
| IO-bound workload (API, DB) | Async / event loop / virtual threads |
| CPU-bound workload (encoding, crypto) | Thread pool / worker processes / GPU |
| Many concurrent connections, low memory | Event loop (Node.js) or virtual threads (Go) |
| Mixed IO + CPU workload | Async for IO, offload CPU to separate thread pool |
| Blocking the event loop | Never — offload CPU work to a worker pool |
| Thread memory overhead | OS threads: 1–8MB each; goroutines: 2–8KB each |
| Context switching cost | OS threads: expensive (kernel); goroutines: cheap (runtime) |
| async / await internals | State machine — each await is a state transition |
| Running IO calls sequentially | Use `CompletableFuture.allOf()` to parallelise them |
| Shared mutable state | Protect with `synchronized`, `ReentrantLock`, or `Atomic*` |
| Simple counter / flag | `AtomicInteger` / `AtomicBoolean` — lock-free ✅ |
| Shared map or collection | `ConcurrentHashMap` / `CopyOnWriteArrayList` |
| Deadlock prevention | Consistent lock ordering; `tryLock()` with timeout |
| Spring async method | `@Async` + `@EnableAsync` + `ThreadPoolTaskExecutor` |
| Parallel IO in Spring | `CompletableFuture.supplyAsync()` + `allOf().join()` | — Complete Guide

## I. The Core Problem: The Cost of Waiting

A typical backend server does not spend most of its time computing — it spends most of its time **waiting**.

### The Idle CPU

```
A single API request timeline:

  0ms      ├── Parse request, authenticate          (CPU active ~0.1ms)
  0.1ms    ├── Query database ───────────────────── (CPU idle, waiting...)
           │                                                │
           │         DB processes query on its machine      │
           │         Network round trip adds latency         │
           │                                                │
  100ms    ├── DB response arrives                  (CPU active ~0.1ms)
  100.1ms  ├── Serialize response, send             (CPU active ~0.1ms)
  100.2ms  └── Done

  Total time:    100.2ms
  CPU active:      0.2ms   (0.2%)
  CPU idle:       100ms    (99.8%) ← waiting for IO
```

```
What the CPU could have done in those 100ms:

  CPU speed: ~3 billion instructions per second
  3,000,000 instructions per millisecond × 100ms = 300,000,000 instructions wasted

  That is the cost of one synchronous DB query on one idle thread.
```

### The IO Bottleneck

In a standard API call, up to **95% of the time** is spent on IO — waiting for responses from databases, caches, or external APIs. The CPU is the fastest component in the system, yet it spends almost all of its time waiting on the slowest ones.

```
Time breakdown of a typical API request:

  ████████████████████████████████████████████░░ 95% IO wait
                                               ██  5% actual computation

  IO sources:
    ├── Database query        20ms – 100ms
    ├── External API call     50ms – 500ms
    ├── Cache lookup (Redis)  1ms – 5ms
    └── File system read      5ms – 50ms
```

> **The core insight:** The problem is not that the CPU is too slow — it is that the CPU sits idle while IO completes. The goal of concurrency is to keep the CPU productive during that idle time.

---

## II. Concurrency vs. Parallelism

These terms describe fundamentally different mechanics and are frequently confused.

```
Parallelism — Doing things at the exact same moment:

  Core 1:  ████ Task A ████
  Core 2:  ████ Task B ████
           ──────────────── time →
  Requires multiple CPU cores. True simultaneous execution.

Concurrency — Dealing with many things by interleaving:

  Core 1:  ██ Task A ██░░░░░░██ Task A ██
                       ││
           ░░░░░░░░████ Task B ████░░░░░
           ──────────────────────────── time →
  Works on a single core. Tasks take turns; none block the others.
  Gives the illusion of simultaneous execution.
```

| | Concurrency | Parallelism |
|---|---|---|
| Cores required | One (or more) | Multiple |
| Mechanism | Interleaving tasks (start, pause, resume) | Simultaneous execution |
| Solves | IO-bound bottlenecks | CPU-bound bottlenecks |
| Analogy | One chef juggling multiple pots | Multiple chefs each at their own pot |

> **The chef analogy:** A chef making three dishes does not stand at the stove watching water boil — they start the pasta, chop vegetables while it heats, stir the sauce, and come back when the water is ready. That is concurrency. A second chef working in parallel on a separate dish at the same time — that is parallelism.

---

## III. IO-Bound vs. CPU-Bound Workloads

The right concurrency strategy depends entirely on what is limiting your program's performance.

### IO-Bound Workloads

Limited by the speed of **external resources** — network, database, file system.

```
Example: Web server handling API requests

  Request 1 → DB query (waiting 80ms) ←─────────────────────── IO bound
  Request 2 → External API call (waiting 200ms) ←────────────── IO bound
  Request 3 → Cache lookup (waiting 3ms) ←───────────────────── IO bound

  CPU is idle most of the time.
  Solution: Concurrency — keep the CPU busy by handling other requests
            while waiting for each IO operation to complete.
```

**IO-bound examples:**

| Workload | IO Source | Concurrency Benefit |
|---|---|---|
| REST API server | Database, cache, external APIs | High — 95%+ time is IO |
| Web scraper | Network requests | High — waiting on remote servers |
| File processor | Disk reads/writes | High — disk is slow |
| Email sender | SMTP / email API | High — network-bound |

---

### CPU-Bound Workloads

Limited by the **CPU's processing power** — no IO waiting, just raw computation.

```
Example: Image resizing pipeline

  Image upload → decompress → resize → re-encode → save
                 ─────────────────────
                 All CPU work, no waiting
                 One core can only go so fast

  Solution: Parallelism — distribute work across multiple CPU cores
            to crunch numbers simultaneously.
```

**CPU-bound examples:**

| Workload | Why CPU-bound | Parallelism Benefit |
|---|---|---|
| Image / video encoding | Pixel math on large files | High — split frames across cores |
| Cryptographic hashing | Heavy math per operation | High — parallelise independent hashes |
| ML model inference | Matrix multiplication | High — GPUs are massively parallel |
| PDF generation | Layout + rendering computation | Moderate |

```
Choosing the right tool:

  IO-bound  → Concurrency (async/await, event loop, virtual threads)
  CPU-bound → Parallelism (multiple OS threads, worker processes, GPU)
  Mixed     → Both: async for IO, thread pool for CPU-heavy steps
```

---

## IV. Concurrency Models

The industry relies on three primary models, each with different tradeoffs in memory, overhead, and complexity.

---

### A. The Threading Model

The OS manages independent execution units called **threads**. Each request gets its own thread; the OS scheduler switches between them.

```
Thread-per-request model:

  Request 1 → OS creates Thread 1 (1MB–8MB stack)
  Request 2 → OS creates Thread 2 (1MB–8MB stack)
  Request 3 → OS creates Thread 3 (1MB–8MB stack)
  ...
  Request 10,000 → OS creates Thread 10,000
                   → 10,000 × 4MB = 40GB RAM ❌ — server runs out of memory
```

#### Preemptive Scheduling

The OS does not wait for a thread to voluntarily pause — it **forcibly interrupts** it after a fixed time slice.

```
Time slice scheduling (e.g., 2ms slices):

  0ms:    Thread A starts
  2ms:    OS pauses Thread A → saves its CPU registers (context switch)
  2ms:    Thread B starts
  4ms:    OS pauses Thread B → saves its CPU registers (context switch)
  4ms:    Thread A resumes from exactly where it left off
  ...

  Context switch cost: ~1–10 microseconds per switch
  At 10,000 threads:  constant context switching overhead ❌
```

#### Threading Costs

| Cost | Detail |
|---|---|
| Memory per thread | 1MB – 8MB stack (OS-managed) |
| Context switch | Save/restore all CPU registers on every switch |
| Scheduling overhead | OS scheduler must track thousands of threads |
| Practical limit | ~hundreds to low thousands of concurrent threads |

**When threads work well:** CPU-bound work where you want true parallel execution across cores, or when you have a bounded, manageable number of concurrent operations.

---

### B. The Event Loop Model

Used by runtimes like **Node.js**. A single thread handles all requests by registering callbacks and never blocking.

```
Event loop — single thread, many concurrent requests:

  Thread: ── handle req1 ── register DB callback ──►
              (2ms CPU work)  (hands off to OS, continues immediately)
                          ── handle req2 ── register API callback ──►
                              (2ms CPU work)
                          ── handle req3 ──►
                              (2ms CPU work)
                          ── req1 DB response arrives ──►
                              (2ms CPU work, send response)
                          ── req2 API response arrives ──►
                              (2ms CPU work, send response)

  One thread handled 3 requests concurrently. ✅
  No context switching. No per-request memory overhead.
```

```
The event loop lifecycle:

  ┌─────────────────────────────────┐
  │          Event Queue            │
  │  [req1 callback] [req2 timer]   │
  └─────────────────────────────────┘
              │
              ▼
  ┌─────────────────────────────────┐
  │         Event Loop              │◄── single thread
  │  1. Take next event             │
  │  2. Execute its callback        │
  │  3. Register any new callbacks  │
  │  4. Go back to step 1           │
  └─────────────────────────────────┘
              │
              ▼
  OS / Thread Pool (libuv in Node.js)
  handles actual IO operations asynchronously
```

#### The Critical Rule: Never Block the Event Loop

```
❌ Blocking operation on the event loop:

  Event loop: handling req1
    → calls: for (let i = 0; i < 1_000_000_000; i++) {}  ← CPU-heavy loop
    → event loop is stuck for 3 seconds

  During those 3 seconds:
    req2, req3, req4, req5... all wait in the queue
    Server appears completely frozen to all users ❌

✅ CPU-heavy work → offload to a worker thread pool
   Event loop stays free to handle IO callbacks
```

---

### C. Virtual Threads (Go Routines)

Go's runtime maps thousands of **lightweight goroutines** onto a small pool of OS threads — combining the simplicity of the threading model with the efficiency of the event loop.

```
OS Thread model (Java/traditional):

  OS Thread:  [1MB–8MB stack each]
  10,000 requests → 10,000 OS threads → ~40GB RAM ❌

Go Routine model:

  OS Threads: [4] ← small, fixed number managed by Go runtime
                │
  Go Routines: [goroutine 1] [goroutine 2] ... [goroutine 100,000]
               [2KB–8KB stack each, grows dynamically]
                │
  Go runtime scheduler maps goroutines → OS threads
  When a goroutine blocks on IO, Go runtime moves it off the thread
  and puts another goroutine on → OS thread never sits idle ✅

  10,000 goroutines × 4KB = 40MB RAM ✅ (vs 40GB for OS threads)
```

```
Go routine lifecycle:

  HTTP request arrives
        │
        ▼
  go handleRequest(req)   ← spins up a goroutine (cheap, ~2KB)
        │
  Goroutine makes DB query
        │
  Go runtime: "this goroutine is waiting for IO"
        │
  Go runtime parks goroutine → puts another on the OS thread
        │
  DB responds → goroutine is rescheduled → continues ✅

  Result: One OS thread handles thousands of goroutines efficiently
```

### Model Comparison

| Model | Memory per task | Context switch | IO handling | CPU-bound |
|---|---|---|---|---|
| OS Threads | 1MB – 8MB | Expensive (kernel) | Blocks thread | Good (true parallel) |
| Event Loop | Minimal (shared) | None | Non-blocking callbacks | Dangerous (blocks all) |
| Virtual Threads / Goroutines | 2KB – 8KB | Cheap (runtime) | Parks goroutine | Good (via OS threads) |

---

## V. Async/Await as a State Machine

`async` and `await` are **syntactic sugar** — they make asynchronous code look synchronous, but the compiler transforms them into something fundamentally different under the hood.

```
What you write:

  async function fetchUserOrders(userId) {
    const user   = await db.findUser(userId);    // await point 1
    const orders = await db.findOrders(userId);  // await point 2
    return { user, orders };
  }

What the compiler actually generates (conceptually):

  State 0: start → call db.findUser() → register callback → suspend
                                                              │
                                                         event loop
                                                         handles other work
                                                              │
  State 1: db.findUser() returned → store result → call db.findOrders() → suspend
                                                                            │
                                                                       event loop
                                                                       handles other work
                                                                            │
  State 2: db.findOrders() returned → store result → return { user, orders } → done
```

```
Every await point is a state transition:

  ┌─────────┐  await db.findUser()   ┌─────────┐  await db.findOrders()  ┌─────────┐
  │ State 0 │ ──────────────────────► │ State 1 │ ───────────────────────► │ State 2 │
  │  Start  │  suspend, free thread   │  Got    │  suspend, free thread    │  Done   │
  └─────────┘                         │  user   │                          └─────────┘
                                      └─────────┘

  Between states: the thread is free to handle other requests ✅
  The function "remembers" exactly where it was when IO completes.
```

```java
// Spring Boot — reactive async with WebFlux
@GetMapping("/users/{id}/orders")
public Mono<UserOrders> getUserOrders(@PathVariable Long id) {
    return userRepository.findById(id)              // async, non-blocking
        .zipWith(orderRepository.findByUserId(id))  // runs both concurrently ✅
        .map(tuple -> new UserOrders(
            tuple.getT1(),
            tuple.getT2()
        ));
}

// Spring Boot — @Async for fire-and-forget
@Async
public CompletableFuture<Void> sendWelcomeEmail(String email) {
    emailClient.send(email);                        // runs in background thread ✅
    return CompletableFuture.completedFuture(null);
}
```

---

## VI. The Concurrency Trap: Race Conditions

Concurrency introduces a new class of bugs around **shared mutable state** — when two threads read and modify the same data simultaneously.

### The Read-Modify-Write Problem

```
Shared counter, starting at 0. Two threads increment it simultaneously.

  Expected result: 0 + 1 + 1 = 2

  What actually happens:

  Thread A:  READ counter  → gets 0
  Thread B:  READ counter  → gets 0    ← reads before A has written back
  Thread A:  ADD 1         → calculates 1
  Thread B:  ADD 1         → calculates 1
  Thread A:  WRITE 1       → counter = 1
  Thread B:  WRITE 1       → counter = 1   ← overwrites A's result ❌

  Final value: 1 (should be 2) — one increment was silently lost ❌
```

This is a **race condition** — the result depends on the unpredictable timing of thread scheduling.

```
Real-world consequences:

  ├── Bank account: two simultaneous withdrawals both read balance $100
  │   Both approve $80 withdrawal → account goes to $20 instead of -$60
  │   (or worse, both write back $20 and $100 is lost) ❌
  │
  ├── Inventory: two users buy the last item simultaneously
  │   Both see stock = 1, both decrement → stock = -1 ❌
  │
  └── Session counter: 1000 concurrent increments → result is 743 ❌
```

### The Fix: Mutual Exclusion with Locks (Mutexes)

A **mutex** (mutual exclusion lock) ensures only one thread can enter a critical section at a time.

```
With a lock:

  Thread A acquires lock ✅
  Thread A: READ counter  → 0
  Thread A: ADD 1         → 1
  Thread A: WRITE 1       → counter = 1
  Thread A releases lock

  Thread B was waiting...
  Thread B acquires lock ✅
  Thread B: READ counter  → 1   ← reads the updated value ✅
  Thread B: ADD 1         → 2
  Thread B: WRITE 2       → counter = 2 ✅
  Thread B releases lock

  Final value: 2 ✅ — both increments counted correctly
```

```java
// Java — synchronized block (intrinsic lock)
private int counter = 0;
private final Object lock = new Object();

public void increment() {
    synchronized (lock) {         // only one thread enters at a time
        counter++;
    }                             // lock released automatically
}

// Java — ReentrantLock (more control)
private final ReentrantLock lock = new ReentrantLock();

public void increment() {
    lock.lock();
    try {
        counter++;
    } finally {
        lock.unlock();            // always release in finally ✅
    }
}

// Java — AtomicInteger (lock-free for simple counters)
private final AtomicInteger counter = new AtomicInteger(0);

public void increment() {
    counter.incrementAndGet();    // atomic read-modify-write, no explicit lock ✅
}
```

### Deadlock: When Locks Go Wrong

```
Thread A holds Lock 1, waits for Lock 2
Thread B holds Lock 2, waits for Lock 1

  Thread A: ──► acquired Lock 1 ──► waiting for Lock 2 ──► (blocked forever)
  Thread B: ──► acquired Lock 2 ──► waiting for Lock 1 ──► (blocked forever)

  Both threads wait forever. Server hangs. ❌

Prevention:
  ├── Always acquire locks in the same order across all threads
  ├── Use tryLock() with a timeout instead of blocking indefinitely
  └── Minimize the number of locks held simultaneously
```

---

## VII. Spring Boot Concurrency Reference

### Thread Pool Configuration

```java
// Configure the async thread pool
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);         // always-alive threads
        executor.setMaxPoolSize(50);          // max under heavy load
        executor.setQueueCapacity(200);       // tasks queued before rejecting
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
```

### Parallel IO with CompletableFuture

```java
// ❌ Sequential — total time = A + B + C
User user     = userRepository.findById(id);         // wait 50ms
List<Order> orders = orderRepository.findByUser(id); // wait 50ms
List<Review> reviews = reviewRepository.findByUser(id); // wait 50ms
// Total: 150ms

// ✅ Parallel — total time = max(A, B, C)
CompletableFuture<User>         userFuture    = CompletableFuture.supplyAsync(() -> userRepository.findById(id));
CompletableFuture<List<Order>>  orderFuture   = CompletableFuture.supplyAsync(() -> orderRepository.findByUser(id));
CompletableFuture<List<Review>> reviewFuture  = CompletableFuture.supplyAsync(() -> reviewRepository.findByUser(id));

CompletableFuture.allOf(userFuture, orderFuture, reviewFuture).join();
// All three run simultaneously → total: ~50ms ✅
```

### Thread-Safe Shared State

```java
@Service
public class RequestMetrics {

    // ✅ Atomic — thread-safe counter without explicit locking
    private final AtomicLong requestCount   = new AtomicLong(0);
    private final AtomicLong errorCount     = new AtomicLong(0);

    // ✅ ConcurrentHashMap — thread-safe map
    private final ConcurrentHashMap<String, Long> endpointCounts = new ConcurrentHashMap<>();

    public void recordRequest(String endpoint) {
        requestCount.incrementAndGet();
        endpointCounts.merge(endpoint, 1L, Long::sum);
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }
}
```

---

## VIII. Quick Reference Checklist

| Concern | Strategy |
|---|---|
| CPU idle during DB / API calls | Concurrency — don't block, handle other requests |
| True simultaneous computation | Parallelism — multiple CPU cores |
| IO-bound workload (API, DB) | Async / event loop / virtual threads |
| CPU-bound workload (encoding, crypto) | Thread pool / worker processes / GPU |
| Many concurrent connections, low memory | Event loop (Node.js) or virtual threads (Go) |
| Mixed IO + CPU workload | Async for IO, offload CPU to separate thread pool |
| Blocking the event loop | Never — offload CPU work to a worker pool |
| Thread memory overhead | OS threads: 1–8MB each; goroutines: 2–8KB each |
| Context switching cost | OS threads: expensive (kernel); goroutines: cheap (runtime) |
| async / await internals | State machine — each await is a state transition |
| Running IO calls sequentially | Use `CompletableFuture.allOf()` to parallelise them |
| Shared mutable state | Protect with `synchronized`, `ReentrantLock`, or `Atomic*` |
| Simple counter / flag | `AtomicInteger` / `AtomicBoolean` — lock-free ✅ |
| Shared map or collection | `ConcurrentHashMap` / `CopyOnWriteArrayList` |
| Deadlock prevention | Consistent lock ordering; `tryLock()` with timeout |
| Spring async method | `@Async` + `@EnableAsync` + `ThreadPoolTaskExecutor` |
| Parallel IO in Spring | `CompletableFuture.supplyAsync()` + `allOf().join()` |