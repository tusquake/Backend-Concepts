# Graceful Shutdown — Complete Guide

## I. The Problem: The "Abrupt" Shutdown

A realistic scenario: a critical e-commerce payment is in progress when a server needs to restart for a new deployment.

- **The Risk:** If the server "slams the door" (shuts down instantly), the payment could be lost, or a customer could be double-charged due to race conditions.
- **The Analogy:** Graceful shutdown is like teaching a server "good manners." Instead of pushing guests out the door, the server politely finishes ongoing conversations, cleans up the room, and then leaves.

> **Real-world impact:** Abrupt shutdowns don't just affect payments — they can corrupt in-flight file uploads, break long-running background jobs, and leave database transactions dangling, causing deadlocks for future requests.

---

## II. Process Life Cycle and Signals

Every backend application runs as a **process** within an operating system (OS), and every process has a lifecycle — born, live, die. Communication between the OS and the process happens via **signals**.

| Signal | Name | Description |
|---|---|---|
| `SIGTERM` | Signal Terminate | A polite nudge. The OS asks the app to "finish up and leave." Standard signal used by Kubernetes and process managers like [PM2](https://pm2.keymetrics.io/). |
| `SIGINT` | Signal Interrupt | Triggered by a developer pressing `Ctrl + C`. Handled almost identically to `SIGTERM` in production code. |
| `SIGKILL` | Signal Kill | The "nuclear option." Cannot be caught, detected, or ignored by your app. The digital equivalent of pulling the power plug from the wall. |

### Signal Handling Flow

```
OS / Orchestrator
       │
       ▼
  SIGTERM sent
       │
       ▼
  App catches signal
       │
       ├── Stop accepting new requests
       ├── Drain existing connections
       ├── Clean up resources
       └── Exit cleanly ✅

  (If timeout exceeded)
       │
       ▼
  SIGKILL sent — forced termination ⚠️
```

> **Note:** You should **never** rely on `SIGKILL` as part of your shutdown strategy. It is a safety net of last resort — your app gets no chance to clean up when it arrives.

---

## III. Connection Draining: The Restaurant Analogy

**Connection draining** is the act of emptying the server of active requests before turning it off.

### The Three-Step Implementation

**Step 1 — Stop New Entries**
Station someone at the door to stop letting new customers in. The server stops accepting new HTTP connections immediately upon receiving the shutdown signal.

**Step 2 — Let Existing Guests Finish**
Allow customers already at their tables to finish their meals and pay their bills. In-flight requests are allowed to complete normally — responses are sent, transactions are committed.

**Step 3 — The Timeout (Hard Limit)**
You cannot wait forever. Most systems implement a **30 to 60-second timeout**. If the server hasn't finished draining by then, it is forcefully stopped to avoid blocking deployments indefinitely.

```
Signal received
      │
      ▼
[CLOSED to new traffic]  ──────────────────────────────┐
      │                                                 │
      ▼                                                 │
[Existing requests complete]                     30–60s timeout
      │                                                 │
      ▼                                                 ▼
[Clean exit ✅]                              [Forced SIGKILL ⚠️]
```

> **Kubernetes context:** When a pod is being terminated, Kubernetes sends `SIGTERM` and simultaneously removes the pod from the load balancer's endpoint list — ensuring no new traffic is routed to it while it drains.

---

## IV. Resource Cleanup and the "Reverse Order" Rule

Beyond finishing requests, the server must release system-level resources it "borrowed."

### Resources That Must Be Released

| Resource | Risk if Not Released |
|---|---|
| File handles | Memory leaks in the OS; file locks block other processes |
| Network sockets (TCP) | Port exhaustion; stale connections consume OS resources |
| Database transactions | Deadlocks, data corruption, row-level locks held indefinitely |
| Cache connections (Redis) | Orphaned connections exhaust the connection pool |
| Background job queues | Jobs may be re-queued and processed twice (duplicate execution) |

### The Golden Rule: Reverse Order of Acquisition

> Resources should be cleaned up in the **reverse order** in which they were acquired.

**Why this matters:** If you acquired resources in this order:

```
1. Database connection pool opened
2. Redis client connected
3. Background worker started
```

Then you must clean up in **reverse**:

```
3. Background worker stopped first
2. Redis client disconnected
1. Database connection pool closed last
```

Closing the database connection first while a background worker still needs it to finish a task causes cascading failures — errors, retries, and potentially duplicate side effects.

---

## V. Practical Execution Flow

The exact sequence of a graceful shutdown, step by step:

### Step 1 — Catch the Signal

Register a handler that listens for `SIGINT` or `SIGTERM` before the server starts. This is the entry point to your entire shutdown logic.

```js
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT',  () => gracefulShutdown('SIGINT'));
```

### Step 2 — HTTP Engine Stop

The server stops listening for new HTTP traffic. Existing keep-alive connections are given a deadline to finish.

```
LOG: "HTTP server closing — no new connections accepted"
```

### Step 3 — Database Closure

The application finishes all current queries, then closes the TCP connection pool cleanly.

```
LOG: "Waiting for active queries to complete..."
LOG: "Database connection pool closed"
```

### Step 4 — Background Workers

Tools like [Redis](https://redis.io/), BullMQ, or task queues are notified to finish their current job before disconnecting. No new jobs are picked up.

```
LOG: "Worker finishing current job..."
LOG: "Redis client disconnected"
```

### Step 5 — Final Exit

The process exits only after every sub-system has confirmed it is clean.

```
LOG: "All subsystems clean. Exiting with code 0." ✅
```

### Full Shutdown Sequence at a Glance

```
SIGTERM / SIGINT received
         │
         ▼
  1. HTTP server stops accepting new requests
         │
         ▼
  2. In-flight HTTP requests finish (drain)
         │
         ▼
  3. Background workers finish current job
         │
         ▼
  4. Redis / cache clients disconnect
         │
         ▼
  5. Database connection pool closes
         │
         ▼
  6. process.exit(0) ✅
```

---

## VI. Graceful Shutdown in Spring Boot

Spring Boot has **built-in graceful shutdown support** since version **2.3.0**. You don't need to wire signals manually — the framework handles it for you, but you need to configure it correctly.

### Step 1 — Enable Graceful Shutdown

In your `application.properties` or `application.yml`:

```properties
# application.properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

```yaml
# application.yml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

- `server.shutdown=graceful` — tells the embedded server (Tomcat/Netty) to stop accepting new requests and wait for active ones to finish.
- `timeout-per-shutdown-phase` — the maximum time Spring waits **per phase** before forcing shutdown. Default is `30s`.

> **Supported embedded servers:** Tomcat, Jetty, Undertow, and Netty (WebFlux) all support graceful shutdown out of the box.

---

### Step 2 — How Spring Boot Handles the Shutdown Sequence

When a `SIGTERM` is received (e.g., from Kubernetes or `kill`), Spring Boot's `SpringApplication` triggers its shutdown hooks automatically in this order:

```
SIGTERM received
       │
       ▼
  JVM Shutdown Hook triggered
       │
       ▼
  Spring ApplicationContext begins closing
       │
       ├── 1. Embedded server stops accepting new HTTP requests
       ├── 2. In-flight requests drain (up to timeout-per-shutdown-phase)
       ├── 3. @PreDestroy methods called on beans
       ├── 4. DisposableBean.destroy() called
       ├── 5. ApplicationContext closed
       └── 6. JVM exits ✅
```

Spring's `SmartLifecycle` interface controls the order of these phases — beans with a higher `phase` value are stopped first.

---

### Step 3 — Database Connection Pool (HikariCP)

Spring Boot uses **HikariCP** by default. When the context closes, HikariCP automatically:

- Waits for active connections to be returned
- Closes all idle connections in the pool
- Shuts down cleanly

No extra configuration needed for basic usage. However, for long-running transactions, set a connection timeout to avoid hanging:

```properties
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1800000
```

---

### Step 4 — Cleaning Up Beans with `@PreDestroy`

Use `@PreDestroy` to run cleanup logic when a bean is being destroyed during shutdown:

```java
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @PreDestroy
    public void onShutdown() {
        // finish any pending payment state, flush buffers, close external clients
        System.out.println("PaymentService: cleaning up before shutdown");
    }
}
```

> **Note:** `@PreDestroy` runs **after** the HTTP server has stopped accepting new requests but **before** the ApplicationContext is fully closed — a safe place to flush state.

---

### Step 5 — Background Jobs with `@Scheduled` and `ThreadPoolTaskExecutor`

If you use `@Scheduled` tasks or async executors, configure them to wait for running tasks before shutting down:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setWaitForTasksToCompleteOnShutdown(true); // ← key setting
        executor.setAwaitTerminationSeconds(30);            // ← hard limit
        return executor;
    }
}
```

- `setWaitForTasksToCompleteOnShutdown(true)` — does not interrupt running tasks on shutdown.
- `setAwaitTerminationSeconds(30)` — forces shutdown after 30 seconds even if tasks are still running.

---

### Step 6 — Actuator Health Check During Shutdown

If you use **Spring Boot Actuator**, the `/actuator/health` endpoint automatically returns `503 Service Unavailable` during shutdown — signalling load balancers to stop routing traffic.

Add Actuator to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Enable the health endpoint:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

During shutdown, Spring sets the application availability to `REFUSING_TRAFFIC`, and the health endpoint reflects this automatically:

```
GET /actuator/health  →  200 UP       (normal operation)
GET /actuator/health  →  503 OUT_OF_SERVICE  (shutdown in progress)
```

This pairs perfectly with a Kubernetes `readinessProbe` pointing to `/actuator/health/readiness`.

---

### Step 7 — Kubernetes + Spring Boot: The Full Picture

```yaml
# deployment.yaml
spec:
  containers:
    - name: my-app
      image: my-app:latest
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
      lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 5"]  # allow time for k8s to deregister pod
  terminationGracePeriodSeconds: 60            # must be > timeout-per-shutdown-phase
```

> **The `preStop` sleep trick:** Kubernetes removes the pod from the load balancer asynchronously. Adding a `preStop` sleep of 5–10 seconds ensures no new traffic arrives during the brief window before deregistration completes.

> **Critical rule:** `terminationGracePeriodSeconds` in Kubernetes must always be **greater than** `spring.lifecycle.timeout-per-shutdown-phase`. If Kubernetes kills the pod before Spring finishes draining, you've lost the benefit of graceful shutdown entirely.

---

### Spring Boot Graceful Shutdown — Quick Reference Checklist

| Configuration | Where | Value |
|---|---|---|
| Enable graceful shutdown | `application.yml` | `server.shutdown: graceful` |
| Set drain timeout | `application.yml` | `spring.lifecycle.timeout-per-shutdown-phase: 30s` |
| Async task wait | `ThreadPoolTaskExecutor` | `setWaitForTasksToCompleteOnShutdown(true)` |
| Bean cleanup | Bean class | `@PreDestroy` method |
| Health check | Actuator | `/actuator/health/readiness` |
| Kubernetes pod timeout | `deployment.yaml` | `terminationGracePeriodSeconds: 60` |
| preStop delay | `deployment.yaml` | `preStop: sleep 5` |

---

## VII. Additional Best Practices

### Set a Shutdown Timeout in Code

Don't rely solely on the orchestrator's timeout. Set your own internal deadline so your app exits predictably:

```js
const SHUTDOWN_TIMEOUT_MS = 30_000;

setTimeout(() => {
  console.error("Shutdown timeout exceeded. Forcing exit.");
  process.exit(1);
}, SHUTDOWN_TIMEOUT_MS);
```

### Distinguish Clean vs. Unclean Exits

Use exit codes to signal the reason for shutdown to your orchestrator:

| Exit Code | Meaning |
|---|---|
| `0` | Clean, graceful shutdown |
| `1` | Unclean — error or timeout forced exit |

### Health Check Integration

During draining, update your health check endpoint to return `503 Service Unavailable`. This signals load balancers to stop routing new traffic immediately, even before the TCP connection is closed.

```
GET /health  →  200 OK       (normal operation)
GET /health  →  503           (shutdown in progress — stop sending traffic)
```

### Idempotency as a Safety Net

Even with perfect graceful shutdown logic, network partitions can cause a request to be retried. Design your critical endpoints (especially payments) to be **idempotent** — processing the same request twice should produce the same result, not duplicate charges or records.