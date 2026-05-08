# Logging, Monitoring & Observability — Complete Guide

## I. Clearing Up the Confusion: The Core Trio

These three terms are often used interchangeably in the industry, but they solve different problems.

```
Logging:        Records discrete events with metadata
                → "What exactly happened?"
                  e.g., "User ID 42 failed login at 10:31:05Z"

Monitoring:     Continuously checks system health and performance
                → "Is there a problem right now?"
                  e.g., "CPU usage is at 94% — alert triggered"

Observability:  Determines internal system state from external outputs
                → "Why is this happening?"
                  e.g., "Trace shows DB query in Service B is taking 4s per request"
```

| | Logging | Monitoring | Observability |
|---|---|---|---|
| Core question | What happened? | Is something wrong? | Why is it wrong? |
| Output type | Discrete event records | Aggregated metrics / alerts | Correlated logs + metrics + traces |
| When it fires | On each event | Continuously / on threshold | On investigation |
| Primary audience | Developers debugging | DevOps / on-call engineers | Both, working together |

> **Rule of thumb:** Logging tells you *what*, monitoring tells you *when*, and observability tells you *why*. A production system needs all three.

---

## II. Why We Need Them: The Slow API Example

### The Problem — A Performance Regression With No Visibility

Your API was responding in 120ms last week. This week it's at 1.8 seconds. Users are complaining. Without observability, the investigation looks like this:

```
User reports: "The app feels slow"
         │
         ▼
  Check server — CPU fine ✅
         │
         ▼
  Check DB — queries look fine ✅
         │
         ▼
  SSH into prod and grep logs...
  No obvious errors ✅
         │
         ▼
  ??? — "It works on my machine" ❌
  Hours lost. Issue unresolved. ❌
```

---

### The Solution — Full Observability Stack

```
User reports: "The app feels slow"
         │
         ▼
  Grafana dashboard shows p95 response
  time spike starting Tuesday 3pm     ← Monitoring ✅
         │
         ▼
  Filter logs by timestamp + endpoint
  "DB connection pool exhausted"       ← Logging ✅
         │
         ▼
  Follow the trace for a slow request:
  Handler (2ms) → Service (5ms) → Repository (1.8s) ← Trace ✅
         │
         ▼
  Root cause: N+1 query introduced
  in Tuesday's deployment             ← Fixed in 20 minutes ✅
```

The same investigation that took hours with no observability takes **minutes** when all three pillars are in place.

---

## III. The Three Pillars of Observability

A system is "observable" when it implements all three of these components together.

```
┌──────────────────────────────────────────────────────────────────┐
│                        OBSERVABILITY                             │
│                                                                  │
│  ┌────────────┐     ┌────────────┐     ┌────────────────────┐   │
│  │    LOGS    │     │  METRICS   │     │      TRACES        │   │
│  │            │     │            │     │                    │   │
│  │ "What      │     │ "How many  │     │ "Where in the      │   │
│  │ happened   │     │ times and  │     │ chain did it       │   │
│  │ and when?" │     │ how fast?" │     │ slow down?"        │   │
│  └────────────┘     └────────────┘     └────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

### 1. Logs

A chronological record of discrete application events. Every meaningful action in your system should produce a log entry.

```
2024-01-15T10:30:00Z  INFO  User registered  { userId: 42, email: "alice@example.com" }
2024-01-15T10:30:01Z  INFO  Verification email enqueued  { userId: 42, taskId: "abc-123" }
2024-01-15T10:30:15Z  WARN  Email delivery delayed  { taskId: "abc-123", attempt: 1 }
2024-01-15T10:31:15Z  ERROR Email delivery failed  { taskId: "abc-123", reason: "SMTP timeout" }
```

---

### 2. Metrics

Aggregated numerical data tracked over time. Unlike logs (discrete events), metrics are **compressed summaries** used to spot patterns and trigger alerts.

```
request_count{endpoint="/api/users", method="POST"}  →  1,204 requests
error_rate{service="email-worker"}                   →  3.2%
response_time_p95{endpoint="/api/orders"}            →  1,840ms ← ⚠️ alert threshold: 500ms
db_connection_pool_available{pool="main"}            →  0 ← ⚠️ exhausted!
```

---

### 3. Traces

A record of a **single request's journey** across all system components — the most powerful tool for pinpointing failure in a distributed system.

```
Request: GET /api/order/789  (total: 1,840ms)
│
├── AuthMiddleware              12ms ✅
│
├── OrderController             3ms  ✅
│
├── OrderService                8ms  ✅
│
├── OrderRepository ──────── 1,802ms ❌ ← bottleneck
│     └── SELECT * FROM orders
│         WHERE userId = 42       ← missing index!
│
└── Response serialization      15ms  ✅
```

> **Without a trace,** you know the request is slow. **With a trace,** you know exactly which function in which service is responsible — down to the SQL query.

---

## IV. Logging in Depth

### Logging Levels

Use levels deliberately to filter noise in production and get detail when debugging.

| Level | When to Use | Example |
|---|---|---|
| `DEBUG` | Detailed troubleshooting, local dev only | `"Entering calculateDiscount() with params {...}"` |
| `INFO` | Normal application events | `"Order #789 created successfully"` |
| `WARN` | Non-critical issue, system still works | `"Retry attempt 2/5 for email task abc-123"` |
| `ERROR` | Operation failed, needs attention | `"Payment gateway timeout for orderId=789"` |
| `FATAL` | System crash, immediate action required | `"Database connection pool exhausted — shutting down"` |

```
Production:  Set level to INFO → DEBUG logs suppressed → less noise, less cost
Development: Set level to DEBUG → full visibility for troubleshooting
```

---

### Structured vs. Unstructured Logging

```
Unstructured (plain text) — good for local development:
  "User 42 logged in from 192.168.1.1 at 10:30:00"
  Easy to read in the terminal. Hard to query at scale.

Structured (JSON) — required for production:
  {
    "level": "INFO",
    "message": "User logged in",
    "userId": 42,
    "ip": "192.168.1.1",
    "timestamp": "2024-01-15T10:30:00Z",
    "traceId": "a3f9c21d-..."
  }
  Easily parsed and queried by tools like Loki and New Relic.
```

| | Unstructured | Structured (JSON) |
|---|---|---|
| Human readability | ✅ Easy to read | ❌ Verbose |
| Machine queryability | ❌ Regex hacks | ✅ Native filtering |
| Cost at scale | ❌ Bloated storage | ✅ Index and compress efficiently |
| Best for | Local development | Production / staging |

> **Best practice:** Use a logging library (not `System.out.println` or `console.log`) so you can switch between formats via configuration, not code changes.

---

### What to Include in Every Log Entry

```json
{
  "level": "ERROR",
  "message": "Payment processing failed",
  "timestamp": "2024-01-15T10:30:00Z",
  "traceId": "a3f9c21d-8f3b-...",    ← links to the full trace
  "spanId": "b7c91a22-...",
  "service": "payment-service",
  "userId": 42,
  "orderId": 789,
  "errorCode": "GATEWAY_TIMEOUT",
  "durationMs": 5003
}
```

> **Never log sensitive data:** passwords, credit card numbers, auth tokens, or PII. Log IDs and codes that can be cross-referenced internally.

---

## V. Monitoring in Depth

### Key Metrics to Track

| Category | Metric | Alert Threshold |
|---|---|---|
| Availability | HTTP 5xx error rate | > 1% of requests |
| Latency | p95 response time | > 500ms |
| Throughput | Requests per second | Drop > 30% from baseline |
| Saturation | CPU usage | Sustained > 80% |
| Saturation | Memory usage | > 90% |
| Queue health | Queue length | > 1,000 pending tasks |
| Queue health | DLQ size | Any entry → immediate alert |
| DB | Connection pool available | 0 connections remaining |
| External | Third-party API error rate | > 5% |

---

### Alert Routing Workflow

```
Metric crosses threshold
         │
         ▼
  Prometheus fires alert rule
         │
         ▼
  AlertManager routes by severity:

  ┌─────────────────────────────────────────┐
  │  Severity: CRITICAL (p95 > 2s)          │
  │  → PagerDuty → on-call engineer's phone │
  └─────────────────────────────────────────┘

  ┌─────────────────────────────────────────┐
  │  Severity: WARNING (queue > 500 tasks)  │
  │  → Slack #ops-alerts channel            │
  └─────────────────────────────────────────┘

  ┌─────────────────────────────────────────┐
  │  Severity: INFO (deployment completed)  │
  │  → Slack #deployments channel           │
  └─────────────────────────────────────────┘
```

---

### Avoiding Alert Fatigue

An alert that fires too often is ignored. An alert that never fires provides false confidence.

```
❌ Bad alert:  "CPU > 70% for 1 second"
   → Fires constantly for minor spikes
   → Engineers start ignoring all alerts

✅ Good alert: "CPU > 80% sustained for 5 minutes"
   → Only fires when there's a real, persistent problem
   → Engineers trust and act on it

❌ Bad alert:  "Any 5xx error"
   → A single transient error pages someone at 3am

✅ Good alert: "5xx error rate > 1% over a 5-minute window"
   → Filters noise, catches real degradation
```

> **Rule of thumb:** Every alert should be actionable. If an engineer receives it and has nothing to do, the threshold is wrong.

---

## VI. Distributed Tracing in Depth

### How a Trace Is Structured

A **trace** is composed of **spans**. Each span represents one unit of work (a function call, a DB query, an external API call) and records its start time, duration, and outcome.

```
Trace ID: a3f9c21d-8f3b-4a2e-b901-7c5d8e2f1a09

Span 1: HTTP Request Handler         0ms ──────────────────────── 1,840ms
  │
  Span 2: Auth Middleware             0ms ── 12ms
  │
  Span 3: OrderService.getOrder()    15ms ── 23ms
  │
  Span 4: OrderRepository.findById() 24ms ───────────────────── 1,826ms  ← slow!
  │         └── DB Query: SELECT * FROM orders WHERE id = 789
  │
  Span 5: Response Serialization   1,827ms ── 1,840ms
```

> **Trace propagation:** For traces to work across services, each service must forward the `traceId` and `spanId` in request headers. This is why a standard like OpenTelemetry matters — it defines the propagation format so all services speak the same language.

---

### Instrumentation and OpenTelemetry

**Instrumentation** is the code added to your application to capture telemetry data (logs, metrics, traces). Done manually, this means vendor-specific SDKs everywhere — locking you into a single provider.

**OpenTelemetry (OTel)** solves this with a vendor-neutral standard:

```
Your Application
      │
      │  (instrumented once with OpenTelemetry SDK)
      │
      ▼
OpenTelemetry Collector
      │
      ├──────────────────────────────────────────┐
      ▼                                          ▼
New Relic / Datadog                     Self-hosted Stack
(Commercial)                       (Prometheus + Loki + Jaeger)

Swap the export target — never rewrite instrumentation code.
```

> **Key benefit:** Instrument once, export anywhere. Switching from New Relic to a self-hosted Grafana stack requires changing a config file — not your application code.

---

## VII. Tools and the Real-World Workflow

### The Grafana Open-Source Stack

```
┌─────────────────────────────────────────────────────────────────────┐
│                        GRAFANA STACK                                │
│                                                                     │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐  ┌───────────┐  │
│  │ Prometheus │   │   Loki     │   │  Grafana   │  │  Jaeger   │  │
│  │            │   │            │   │            │  │           │  │
│  │  Metrics   │   │   Logs     │   │ Dashboards │  │  Traces   │  │
│  │  Storage   │   │  Storage   │   │ & Alerts   │  │ Storage   │  │
│  └────────────┘   └────────────┘   └────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

| Tool | Role | Best For |
|---|---|---|
| [Prometheus](https://prometheus.io) | Metrics collection & storage | Scraping metrics from services, alerting rules |
| [Loki](https://grafana.com/oss/loki/) | Log aggregation | Querying logs without indexing every field |
| [Grafana](https://grafana.com) | Dashboards & alert routing | Visualizing all three pillars in one place |
| [Jaeger](https://www.jaegertracing.io) | Distributed trace storage | Searching and visualizing traces |

---

### Commercial Alternatives

| Tool | Strength |
|---|---|
| [New Relic](https://newrelic.com) | Full-stack observability in one platform, generous free tier |
| [Datadog](https://www.datadoghq.com) | Deep integrations, APM, infrastructure monitoring |
| [Honeycomb](https://www.honeycomb.io) | Best-in-class for trace-driven debugging |
| [Elastic (ELK Stack)](https://www.elastic.co) | Log search at massive scale |

> **Choosing a stack:** Start with a commercial tool (New Relic or Datadog) to get value fast. Migrate to the self-hosted Grafana stack if costs become a concern at scale.

---

### The Real-World Investigation Workflow

```
Step 1: Alert fires
        └── Slack: "p95 response time > 1s on /api/orders [5 min window]"
                   │
                   ▼

Step 2: Open Grafana dashboard
        └── Chart shows latency spike starting Tuesday 3pm, correlates with a deployment
                   │
                   ▼

Step 3: Filter Loki logs by timeframe + service
        └── ERROR: "DB connection pool exhausted" repeated 47 times
                   │
                   ▼

Step 4: Find a trace for a slow request in Jaeger
        └── Span 4 (OrderRepository.findById) → 1,802ms
            └── SQL: SELECT * FROM orders JOIN order_items ... WHERE userId = 42
                      → Missing index on order_items.userId ← root cause ✅
                   │
                   ▼

Step 5: Fix, deploy, verify metrics return to baseline ✅
```

---

## VIII. Logging, Monitoring & Observability in Spring Boot

### A. Structured Logging with Logback + JSON

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- logback-spring.xml — JSON in production, plain text locally -->
<springProfile name="prod">
    <appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
</springProfile>

<springProfile name="local">
    <appender name="PLAIN_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
</springProfile>
```

```java
// Include structured context fields in every log
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public Order createOrder(Long userId, CreateOrderRequest req) {
        log.info("Creating order",
            StructuredArguments.keyValue("userId", userId),
            StructuredArguments.keyValue("itemCount", req.getItems().size())
        );

        Order order = orderRepository.save(req.toEntity(userId));

        log.info("Order created",
            StructuredArguments.keyValue("orderId", order.getId()),
            StructuredArguments.keyValue("totalAmount", order.getTotal())
        );

        return order;
    }
}
```

---

### B. Metrics with Micrometer + Prometheus

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml — expose the /actuator/prometheus scrape endpoint
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, info
  metrics:
    export:
      prometheus:
        enabled: true
```

```java
// Custom business metrics
@Service
public class OrderService {

    private final MeterRegistry meterRegistry;
    private final Counter orderCreatedCounter;
    private final Timer orderProcessingTimer;

    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCreatedCounter = Counter.builder("orders.created.total")
            .description("Total orders created")
            .register(meterRegistry);
        this.orderProcessingTimer = Timer.builder("orders.processing.duration")
            .description("Order processing duration")
            .register(meterRegistry);
    }

    public Order createOrder(Long userId, CreateOrderRequest req) {
        return orderProcessingTimer.recordCallable(() -> {
            Order order = orderRepository.save(req.toEntity(userId));
            orderCreatedCounter.increment();
            return order;
        });
    }
}
```

---

### C. Distributed Tracing with OpenTelemetry

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
```

```yaml
# application.yml
otel:
  service:
    name: order-service
  exporter:
    otlp:
      endpoint: http://jaeger:4317   # or New Relic / Datadog OTLP endpoint
  traces:
    sampler: parentbased_traceidratio
    sampler:
      arg: 0.1   # sample 10% of traces in production
```

```java
// Traces propagate automatically across HTTP calls via Spring's RestTemplate/WebClient.
// Add custom spans for business-critical sections:
@Service
public class PaymentService {

    private final Tracer tracer;

    public PaymentResult charge(Order order) {
        Span span = tracer.spanBuilder("payment.charge")
            .setAttribute("order.id", order.getId())
            .setAttribute("amount", order.getTotal().toString())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            return stripeClient.charge(order);
        } catch (StripeException ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
```

---

### D. Correlating Logs with Traces (MDC)

To link a log entry back to its trace in Jaeger, inject the `traceId` into every log line automatically:

```java
// Filter: injects traceId into MDC for every request
@Component
public class TraceIdMdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String traceId = Span.current().getSpanContext().getTraceId();
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

```json
// Every log line now includes the traceId:
{
  "level": "ERROR",
  "message": "Payment failed",
  "traceId": "a3f9c21d8f3b4a2eb9017c5d8e2f1a09",   ← paste into Jaeger to find the full trace
  "orderId": 789,
  "timestamp": "2024-01-15T10:30:05Z"
}
```

---

### Spring Boot Observability — Quick Reference Checklist

| Concern | Spring Boot Mechanism |
|---|---|
| Structured JSON logging | `logstash-logback-encoder` + `LogstashEncoder` |
| Log levels by environment | `logback-spring.xml` + `<springProfile>` |
| MDC trace correlation | `Filter` + `MDC.put("traceId", ...)` |
| Expose metrics endpoint | `spring-boot-starter-actuator` + `management.endpoints` |
| JVM + HTTP metrics (auto) | `micrometer-registry-prometheus` |
| Custom business metrics | `Counter`, `Timer`, `Gauge` via `MeterRegistry` |
| Distributed tracing (auto) | `opentelemetry-spring-boot-starter` |
| Custom spans | `Tracer.spanBuilder()` + `span.makeCurrent()` |
| Export traces to Jaeger | `otel.exporter.otlp.endpoint` config |
| Export traces to New Relic | OTLP endpoint + New Relic license key header |
| Alert routing | Prometheus AlertManager + Grafana → Slack / PagerDuty |
| Full observability dashboard | Grafana (Prometheus + Loki + Jaeger data sources) |

---

## IX. Best Practices for Backend Engineers

### 1. Log at the Boundaries, Not the Interior

```
❌ Over-logging (noise):
   "Entering validateEmail()"
   "Email string is not null"
   "Calling regex pattern"
   "Regex returned true"
   "Exiting validateEmail()"

✅ Log at meaningful boundaries:
   "Order created" { orderId: 789 }          ← state change ✅
   "Payment failed" { orderId: 789, reason }  ← error ✅
   "User session expired" { userId: 42 }      ← security event ✅
```

---

### 2. Treat Logs as Data, Not Debug Output

Production logs are queried by tooling — not read line by line by humans.

```
❌ "Processing order for user"
   → Ungroupable. No IDs. Useless in Loki query.

✅ { "event": "order.processing.started", "orderId": 789, "userId": 42 }
   → Query: {service="order-service"} | json | orderId=`789`
   → Instantly find all events for order 789 across services ✅
```

---

### 3. Sample Traces in Production

Tracing every request at high traffic generates enormous data volume and cost. Use sampling:

```
Sampling strategies:

Head-based (decided at request start):
  → Sample 10% of all requests randomly
  → Simple, low overhead
  → May miss rare slow requests

Tail-based (decided after request completes):
  → Always sample requests > 1s or with errors
  → Sample 1% of fast, successful requests
  → More useful, higher collector overhead
```

---

### 4. The On-Call Runbook

Every alert should link to a runbook — a document that tells the on-call engineer exactly what to do.

```
Alert: "DLQ size > 0 on email:dlq queue"

Runbook:
  1. Open Grafana → email-worker dashboard
  2. Check worker failure rate — is it elevated?
  3. Open Loki → filter: {job="email-worker"} |= "ERROR"
  4. Find the failing task payload in the DLQ
  5. If transient (SMTP timeout): manually re-enqueue tasks
  6. If systematic (bad payload format): fix code → redeploy → re-enqueue
  7. Escalate to email-team Slack channel if unresolved in 30 min
```

> **Observability without runbooks is incomplete.** Knowing *what* is wrong is only half the job — the team needs to know *what to do about it*.

---

### 5. Observability Is a Team Responsibility

```
Developers write:                    DevOps / Platform manages:
  ├── Log statements                   ├── Prometheus scrape config
  ├── Custom metric counters           ├── Loki log ingestion pipeline
  ├── Custom trace spans               ├── Grafana dashboards
  └── MDC correlation context          ├── Alert thresholds and routing
                                       └── Retention and storage costs

Neither can do it alone.
Observability is a collaborative contract between both teams.
```