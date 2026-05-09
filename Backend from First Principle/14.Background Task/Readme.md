# Background Tasks & Async Processing — Complete Guide

## I. What Is a Background Task?

A background task is any piece of logic that runs **outside the request-response lifecycle**.

### Synchronous vs Asynchronous

```
Synchronous:
  Client → Request → [Do everything: save DB + send email + resize image] → Response
                      └──────────── User waits the entire time ────────────┘

Asynchronous:
  Client → Request → [Save DB + push task to queue] → 201 Response (instant ✅)
                                        │
                                   (background)
                                        ▼
                              Worker picks up task
                              → sends email
                              → resizes image
                              → User has already moved on
```

| | Synchronous | Asynchronous |
|---|---|---|
| User experience | Waits for everything | Immediate response |
| External API failure | User request fails | Task retried silently |
| Long operation (video encoding) | Request times out | Runs to completion in background |
| Server resource usage | Thread held for entire operation | Thread freed immediately |

> **Rule of thumb:** If an operation takes more than ~200ms, involves an external service, or doesn't need to block the user — it belongs in a background task.

---

## II. Why We Need Them: The Sign-Up Example

### The Problem — Synchronous Email Sending

A typical user sign-up flow includes sending a verification email via a third-party service (Resend, Mailgun, SendGrid). Done synchronously:

```
User submits sign-up form
         │
         ▼
  Save user to database         ← fast ✅
         │
         ▼
  Call Resend API (external)    ← what if it takes 3s? what if it's down?
         │
    ┌────┴────┐
  Fast      Slow / Down
    │          │
    ▼          ▼
  201 ✅    User waits…
             Request times out ❌
             User thinks sign-up failed
             They try again → duplicate account ❌
```

The user's sign-up experience is now **held hostage** by a third-party email provider.

---

### The Solution — Asynchronous Email Task

```
User submits sign-up form
         │
         ▼
  Save user to database         ← fast ✅
         │
         ▼
  Push "SendVerificationEmail"
  task into queue               ← near-instant ✅
         │
         ▼
  Return 201 Created            ← user is done, moves on ✅

                    (separately, in background)
                              │
                              ▼
                    Worker dequeues task
                    → calls Resend API
                    → email sent ✅
                    (if it fails, retry automatically)
```

The sign-up is **decoupled** from the email delivery. Even if the email service is down for 10 minutes, the user is unaffected — the task retries until it succeeds.

---

## III. The Architecture: Producers, Brokers, and Consumers

A background task system has three core components that work together.

```
┌──────────────┐     enqueue      ┌──────────────┐     dequeue     ┌──────────────┐
│   Producer   │ ───────────────► │    Broker    │ ──────────────► │   Consumer   │
│ (Your App)   │                  │  (The Queue) │                  │  (Worker)    │
└──────────────┘                  └──────────────┘                  └──────────────┘
       │                                 │                                  │
  Creates task                   Stores tasks                      Processes task
  Serializes to JSON             until processed                   Sends ACK when done
  Pushes to queue                Handles retries                   Or NACKs on failure
                                 on missed ACKs
```

---

### 1. Producer

Your main application code. Responsible for:

- Creating the task payload (serialized as JSON)
- Pushing it into the broker queue
- Returning a response to the client immediately — **without waiting for the task to complete**

```json
// Task payload pushed to queue
{
  "task": "send_verification_email",
  "userId": 42,
  "email": "alice@example.com",
  "requestId": "a3f9c21d-...",
  "enqueuedAt": "2024-01-15T10:30:00Z"
}
```

---

### 2. Broker (The Queue)

The engine that stores tasks until a worker is ready to process them. It acts as a **buffer and coordinator** between producers and consumers.

| Broker | Best For | Key Strength |
|---|---|---|
| [Redis](https://redis.io) (BullMQ, Pub/Sub) | Simple queues, rate limiting, lightweight jobs | Speed, already in your stack |
| [RabbitMQ](https://www.rabbitmq.com) | Complex routing, priority queues | Flexible routing rules, AMQP protocol |
| [AWS SQS](https://aws.amazon.com/sqs/) | Cloud-native, serverless workflows | Fully managed, scales automatically |
| [Apache Kafka](https://kafka.apache.org) | High-throughput event streaming | Replay, audit logs, millions of events/sec |

> **Choosing a broker:** For most applications, Redis (via BullMQ in Node.js or Spring + Redis in Java) is sufficient and eliminates the need for a separate infrastructure component. Kafka is for event streaming at massive scale — not just task queues.

---

### 3. Consumer (The Worker)

A **separate process** that continuously polls the broker for new tasks, processes them, and sends an acknowledgement.

```
Worker lifecycle:
  │
  ▼
Poll broker for next task
  │
  ├── No task → wait and poll again
  │
  └── Task found
        │
        ▼
      Execute task (call API, send email, resize image)
        │
        ├── Success → ACK (acknowledge) → broker removes task ✅
        │
        └── Failure → NACK (negative acknowledge) → broker re-queues task
                        │
                        ▼
                   Retry with exponential backoff
```

> **Workers are horizontally scalable.** If your queue is backing up, spin up more worker instances. Producers and consumers scale independently.

---

## IV. Types of Background Tasks

### 1. One-Off Tasks

Triggered by a single event. Executed once (with retries on failure).

**Examples:**
- Send password reset email
- Send order confirmation
- Generate a PDF invoice
- Notify a webhook endpoint
- Resize a profile photo after upload

```
User requests password reset
         │
         ▼
Producer: push "SendPasswordResetEmail" task
         │
Worker:  dequeue → call email API → ACK ✅
```

---

### 2. Recurring Tasks (Cron Jobs)

Executed on a **fixed schedule**, independent of user actions.

**Examples:**

| Task | Schedule |
|---|---|
| Generate monthly billing reports | `0 0 1 * *` (1st of month, midnight) |
| Clean up expired sessions from DB | `0 * * * *` (every hour) |
| Send weekly digest emails | `0 9 * * MON` (Monday 9am) |
| Refresh trending topics cache | `*/5 * * * *` (every 5 minutes) |
| Database backup | `0 2 * * *` (2am daily) |

```
Cron Expression: "0 0 1 * *"
                  │ │ │ │ │
                  │ │ │ │ └── Day of week (* = any)
                  │ │ │ └──── Month (* = any)
                  │ │ └────── Day of month (1 = 1st)
                  │ └──────── Hour (0 = midnight)
                  └────────── Minute (0)
```

> **Warning:** In distributed systems with multiple app instances, cron jobs can fire on **every instance simultaneously**. Use a distributed lock (via Redis) or a dedicated scheduler to ensure a job runs on only one instance at a time.

---

### 3. Chain Tasks (Task Pipelines)

Tasks with **parent-child dependencies** — the next task only starts after the previous one succeeds.

**Example: Video upload on an LMS platform:**

```
Step 1: Upload raw video to S3           ✅
         │
         ▼
Step 2: Encode video (720p, 1080p, 4K)  ✅ (depends on Step 1)
         │
         ├──────────────────┐
         ▼                  ▼
Step 3a: Generate          Step 3b: Generate
         thumbnails                  transcript
         (depends on 2)             (depends on 2)
         │                           │
         └──────────┬────────────────┘
                    ▼
         Step 4: Notify user "Your video is ready" ✅
```

If Step 2 (encoding) fails, Steps 3a, 3b, and 4 are never triggered — no orphaned partial results.

---

### 4. Batch Tasks

Triggering **many identical tasks at once** — one event fans out into many parallel workers.

**Example: User account deletion (GDPR compliance):**

```
User requests account deletion
         │
         ▼
Producer fans out 6 tasks simultaneously:

  ├── DeleteUserPosts task
  ├── DeleteUserComments task
  ├── DeleteUserOrders task
  ├── DeleteUserPaymentMethods task (Stripe)
  ├── DeleteUserFromEmailList task
  └── DeleteUserProfilePhoto task (S3)

All run in parallel → user deleted from all systems ✅
```

> **Batch + idempotency:** If the batch is partially completed and needs to retry, already-completed sub-tasks must not execute again. Design each sub-task to be idempotent.

---

## V. Critical Design Considerations

### 1. Idempotency

Design every task so it can be **safely executed multiple times** with the same result. Tasks fail and retry — this is expected behavior, not an edge case.

```
Non-idempotent task (dangerous):
  "Charge customer $50"
  → Retry 1: charge $50 ✅
  → Retry 2: charge $50 again ❌ — double charged!

Idempotent task (safe):
  "Charge customer $50 for orderId=abc123"
  → Retry 1: charge $50, mark orderId=abc123 as charged ✅
  → Retry 2: orderId=abc123 already charged → skip ✅
```

**How to implement idempotency:**
- Use a unique `idempotencyKey` (e.g., `orderId`, `requestId`) stored in the task payload
- Check if the key has already been processed before executing
- Store processed keys in Redis or the database with a TTL

---

### 2. Exponential Backoff

When a task fails (e.g., an external API is down), the worker should retry after an **increasing delay** — not immediately. Retrying instantly hammers an already-struggling service.

```
Attempt 1 → fails → wait 1 min
Attempt 2 → fails → wait 2 min
Attempt 3 → fails → wait 4 min
Attempt 4 → fails → wait 8 min
Attempt 5 → fails → wait 16 min
...
Max attempts reached → move to Dead Letter Queue (DLQ) → alert on-call engineer
```

Add **jitter** (random variance) to prevent multiple workers from retrying at the exact same moment:

```
Wait time = base_delay × 2^attempt + random(0, 1000ms)
```

---

### 3. Visibility Timeout

A safety mechanism built into the broker. When a worker dequeues a task, the broker **hides it from other workers** for a set duration. If the worker crashes without sending an ACK, the task becomes visible again and is reprocessed.

```
Broker: task "SendEmail" visible to workers
         │
Worker A dequeues task
         │
Broker: task hidden for 30 seconds (visibility timeout)
         │
    ┌────┴────┐
  Worker A   Worker A
  finishes   crashes
  sends ACK  (no ACK received)
    │              │
    ▼              ▼ (after 30s timeout)
  Task       Task becomes visible again
  deleted ✅  → Worker B picks it up ✅
```

> Set visibility timeout slightly longer than your expected maximum task execution time. Too short → tasks are reprocessed while still running (duplicate execution). Too long → crashed tasks take too long to recover.

---

### 4. Dead Letter Queue (DLQ) *(Added)*

A separate queue where tasks are sent after exhausting all retry attempts. This prevents a permanently failing task from blocking the main queue forever.

```
Main Queue → Retry 1 → Retry 2 → Retry 3 → ... → Max retries
                                                        │
                                                        ▼
                                               Dead Letter Queue
                                                        │
                                               ├── Alert on-call engineer
                                               ├── Log for debugging
                                               └── Manual retry after fix
```

> **Every production queue should have a DLQ.** Without one, failed tasks are silently dropped or loop forever.

---

## VI. Best Practices for Backend Engineers

### 1. Keep Tasks Small and Focused

A task should do **one thing**. If it fails, only that small unit needs to be retried — not a large multi-step operation.

```
❌ One big task: "ProcessNewUser"
   → saves profile, sends email, creates Stripe customer, adds to CRM, sends Slack alert
   → if Slack alert fails, the entire task retries
   → user may get duplicate emails, duplicate Stripe customers

✅ Five small tasks:
   → "SaveUserProfile"
   → "SendVerificationEmail"
   → "CreateStripeCustomer"
   → "AddUserToCRM"
   → "SendSlackAlert"
   → if Slack alert fails, only that task retries ✅
```

---

### 2. Avoid Long-Running Tasks

A single task that runs for 30 minutes is fragile — one network hiccup and the entire operation fails and must restart from scratch.

Break massive operations into smaller chunks that checkpoint their progress:

```
❌ One task: "Export all 1 million user records to CSV"
   → runs for 45 minutes → crashes at minute 43 → restart from scratch

✅ Chunked tasks:
   → "ExportUsersBatch" (userId 1–1000)
   → "ExportUsersBatch" (userId 1001–2000)
   → ...
   → "MergeAndFinalizeExport"
   → if batch 7 fails → only batch 7 retries ✅
```

---

### 3. Monitoring — Catch Bottlenecks Before Users Do

A growing queue length is an early warning sign that workers are overwhelmed or failing silently.

**Key metrics to track:**

| Metric | What It Tells You | Alert Threshold |
|---|---|---|
| Queue length | Tasks accumulating faster than processed | > 1000 pending tasks |
| Worker throughput | Tasks processed per minute | Drop of > 20% from baseline |
| Task failure rate | % of tasks failing | > 5% failure rate |
| Task execution time | P95 processing duration | Exceeds visibility timeout |
| DLQ size | Tasks that exhausted all retries | Any entry → immediate alert |

**Tooling:**

```
Workers emit metrics → Prometheus scrapes → Grafana dashboard
                                                │
                                         ┌──────┴──────┐
                                    Queue length    Failure rate
                                    crosses 1000    exceeds 5%
                                         │              │
                                         ▼              ▼
                                    PagerDuty alert (on-call engineer) ✅
```

---

## VII. Background Tasks in Spring Boot

### A. Simple Async Tasks with `@Async`

For lightweight fire-and-forget tasks within the same application:

```java
// Enable async processing
@SpringBootApplication
@EnableAsync
public class Application { ... }

// Configure the thread pool
@Configuration
public class AsyncConfig {

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}

// Mark a method as async — returns immediately to caller
@Service
public class EmailService {

    @Async("taskExecutor")
    public CompletableFuture<Void> sendVerificationEmail(String email, String token) {
        // Runs in background thread — caller is not blocked
        resendClient.send(email, token);
        return CompletableFuture.completedFuture(null);
    }
}
```

> **Limitation:** `@Async` runs in the same JVM process. If the app restarts, in-flight tasks are lost. Use a message broker (Redis/RabbitMQ) for durability.

---

### B. Durable Task Queue with Spring + Redis (BullMQ alternative)

Use **Spring Data Redis** + a queue library for durable, persistent background tasks:

```java
@Service
public class TaskProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueue(String queueName, Object payload) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(payload);
        redisTemplate.opsForList().rightPush(queueName, json);
    }
}

// Usage in controller — enqueue and respond immediately
@PostMapping("/users")
public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
    User user = userService.create(req);

    // Push email task to queue — don't wait for it
    taskProducer.enqueue("email:verification", Map.of(
        "userId", user.getId(),
        "email", user.getEmail()
    ));

    return ResponseEntity.status(201).body(UserResponse.from(user));
}
```

---

### C. Cron Jobs with `@Scheduled`

```java
@Component
public class ScheduledTasks {

    // Runs every 5 minutes
    @Scheduled(cron = "0 */5 * * * *")
    public void refreshTrendingTopics() {
        log.info("Refreshing trending topics cache...");
        trendingService.recompute();
    }

    // Runs at 2am every day
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredSessions() {
        int deleted = sessionRepository.deleteExpiredSessions(LocalDateTime.now());
        log.info("Cleaned {} expired sessions", deleted);
    }

    // Runs on a fixed delay — 30s after the previous execution finishes
    @Scheduled(fixedDelay = 30_000)
    public void processEmailQueue() {
        emailWorker.processNext();
    }
}
```

**Enable scheduling:**

```java
@SpringBootApplication
@EnableScheduling
public class Application { ... }
```

**Prevent duplicate execution across multiple instances (distributed lock):**

```java
@Scheduled(cron = "0 0 1 * * *")  // 1am daily
public void generateMonthlyReport() {
    String lockKey = "lock:monthly-report";
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "locked", Duration.ofMinutes(10));

    if (Boolean.FALSE.equals(acquired)) {
        log.info("Monthly report already running on another instance — skipping");
        return;
    }

    try {
        reportService.generateMonthlyReport();
    } finally {
        redisTemplate.delete(lockKey);  // always release the lock
    }
}
```

---

### D. Chain Tasks

```java
@Service
public class VideoProcessingService {

    @Async
    public CompletableFuture<Void> processUpload(Long videoId) {
        return CompletableFuture
            .runAsync(() -> encodeVideo(videoId))               // Step 1
            .thenRunAsync(() -> generateThumbnails(videoId))    // Step 2a (after 1)
            .thenRunAsync(() -> generateTranscript(videoId))    // Step 2b (after 1)
            .thenRunAsync(() -> notifyUser(videoId))            // Step 3 (after all)
            .exceptionally(ex -> {
                log.error("Video processing failed for videoId={}", videoId, ex);
                notifyUserOfFailure(videoId);
                return null;
            });
    }
}
```

---

### E. Retry with Exponential Backoff (Spring Retry)

```java
@Service
public class EmailWorker {

    @Retryable(
        retryFor = { ExternalServiceException.class },
        maxAttempts = 5,
        backoff = @Backoff(delay = 60_000, multiplier = 2, random = true)
        // 1min → 2min → 4min → 8min → 16min (with jitter)
    )
    public void sendEmail(EmailTask task) {
        resendClient.send(task.getTo(), task.getSubject(), task.getBody());
        log.info("Email sent successfully to {}", task.getTo());
    }

    @Recover
    public void recover(ExternalServiceException ex, EmailTask task) {
        log.error("Email delivery failed after all retries for {}", task.getTo());
        deadLetterQueueService.push("email:dlq", task);
        alertService.notifyOnCall("Email worker exhausted retries", task);
    }
}
```

---

### Spring Boot Background Tasks — Quick Reference Checklist

| Concern | Spring Boot Mechanism |
|---|---|
| Enable async | `@EnableAsync` on main class |
| Fire-and-forget method | `@Async("executorBeanName")` |
| Configure thread pool | `ThreadPoolTaskExecutor` `@Bean` |
| Cron job (scheduled) | `@Scheduled(cron = "...")` + `@EnableScheduling` |
| Fixed delay job | `@Scheduled(fixedDelay = ms)` |
| Distributed cron lock | `redisTemplate.opsForValue().setIfAbsent()` |
| Durable task queue | Spring Data Redis + list operations |
| Chain tasks | `CompletableFuture.thenRunAsync()` |
| Retry with backoff | `spring-retry` + `@Retryable` + `@Backoff` |
| Dead letter queue | `@Recover` method + manual DLQ push |
| Graceful shutdown | `setWaitForTasksToCompleteOnShutdown(true)` |
| Monitoring | Micrometer + Prometheus + Grafana |