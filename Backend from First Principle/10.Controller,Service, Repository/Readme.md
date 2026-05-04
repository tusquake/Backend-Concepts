# Layered Architecture, Middlewares & Request Context — Complete Guide

## I. The Layered Architecture

Separating responsibilities across distinct layers is an established design pattern for **scalability and easier debugging**. While not a hard requirement, it is the industry standard for backend systems.

```
Incoming Request
       │
       ▼
  [ Controller / Handler ]   ← Entry point: binding, validation, response
       │
       ▼
  [    Service Layer    ]    ← Business logic: orchestration, processing
       │
       ▼
  [  Repository Layer   ]    ← Database: queries, inserts, fetches
       │
       ▼
  [ Database / Storage  ]    ← PostgreSQL, Redis, MongoDB, etc.
```

---

### A. Handlers / Controllers

The **entry point** for a request after it has been routed.

#### Responsibilities

**1. Receive the Request & Response**
The controller receives the `Request` and `Response` objects provided by the language runtime — headers, body, query params, path variables are all accessible here.

**2. Binding / Deserialization**
Deserializes the incoming JSON payload into native data structures (e.g., a `struct` in Go, a `class` in Java/Python). This converts raw bytes into something the rest of the app can work with.

```json
// Raw incoming JSON
{ "name": "Alice", "age": 28, "email": "alice@example.com" }
```

```java
// Bound into a Java class (DTO)
public class UserRequest {
    private String name;
    private int age;
    private String email;
}
```

**3. Validations & Transformations**
Ensures data is in the expected format and applies defaults where needed — e.g., if no `sort` order is provided, default to `"asc"`. See the Validation guide for a deep dive.

**4. Response Control**
Decides the appropriate HTTP status code and sends the final response back to the client.

| Scenario | Status Code |
|---|---|
| Resource created | `201 Created` |
| Successful read | `200 OK` |
| Validation failed | `400 Bad Request` |
| Unauthorized | `401 Unauthorized` |
| Server error | `500 Internal Server Error` |

> **Rule:** Controllers should be thin. They coordinate — they do not contain business logic. If your controller is making decisions about *how* to process data, that logic belongs in the service layer.

---

### B. Services

The **business logic** layer — the brain of the application.

#### Responsibilities

**1. HTTP-Agnostic**
A service function should look like a standard function that takes data and returns a result. It should have no knowledge of HTTP — no `Request`, no `Response`, no status codes. This makes it independently testable.

```java
// ✅ Good — service knows nothing about HTTP
public UserResponse createUser(UserRequest request) { ... }

// ❌ Bad — service is coupled to HTTP concerns
public ResponseEntity<UserResponse> createUser(HttpServletRequest req) { ... }
```

**2. Orchestration**
Calls multiple repository methods, triggers external API calls, sends emails, dispatches webhooks, or processes notifications — in the right order, with the right error handling.

```
Service: createOrder()
    │
    ├── repository.insertOrder()
    ├── paymentGateway.charge()
    ├── emailService.sendConfirmation()
    └── notificationService.pushAlert()
```

**3. Contains the "Actual Processing"**
All meaningful decisions live here — pricing rules, permission checks, data enrichment, workflow branching.

> **Note:** Services should be stateless where possible. State should live in the database or cache, not in the service object itself. This allows horizontal scaling without inconsistency.

---

### C. Repositories

The **database interaction** layer — the only layer that talks to storage.

#### Responsibilities

**1. Single Responsibility**
Constructs and executes database queries — `INSERT`, `SELECT`, `UPDATE`, `DELETE`. Nothing else.

**2. One Method, One Kind of Data**
Each repository method should return exactly one kind of result. Avoid overloading a single method to handle multiple query shapes.

```java
// ✅ Good — one method, one responsibility
UserRepository.getById(Long id)
UserRepository.getAll()
UserRepository.getByEmail(String email)

// ❌ Bad — one method doing too much
UserRepository.get(Long id, String email, boolean fetchAll)
```

> **Note:** Repositories should never contain business logic. A condition like "only fetch active users if the caller is an admin" belongs in the service, not the repository. The repository executes queries; the service decides *which* query to call.

---

## II. Middlewares

Middlewares are **optional functions executed in the middle of the request-response cycle** — between the raw HTTP request arriving and the controller handling it.

### The `next()` Function

Unlike standard handlers, middlewares receive a special `next` parameter. Calling `next()` passes execution to the next middleware in the chain, or to the controller if no more middlewares remain.

```
Request
   │
   ▼
[CORS Middleware]      → next()
   │
   ▼
[Auth Middleware]      → next()
   │
   ▼
[Rate Limiter]         → next()
   │
   ▼
[Controller / Handler]
   │
   ▼
[Error Handler]        ← catches any error thrown above
   │
   ▼
Response
```

If a middleware does **not** call `next()` (e.g., the auth check fails), the request is terminated there and a response is returned immediately — the controller is never reached.

---

### Critical Ordering

The order in which middlewares are registered **matters significantly**. A poorly ordered chain can create security holes or swallow errors silently.

| Order | Middleware | Why Here |
|---|---|---|
| 1st | **CORS** | Reject unauthorized origins as early as possible — before any processing |
| 2nd | **Rate Limiting** | Block abusive IPs before auth or business logic runs |
| 3rd | **Authentication** | Verify identity before any data is accessed |
| 4th | **Request Context** | Populate shared state (user ID, request ID) for downstream use |
| Last | **Global Error Handler** | Must be last to catch errors thrown by any preceding layer |

> **Why CORS first?** A cross-origin request from a disallowed domain should be rejected immediately — there's no point running auth, rate limiting, or business logic for a request that will be refused anyway.

---

### Common Middleware Use Cases

#### 1. CORS (Cross-Origin Resource Sharing)
Handles cross-origin requests by validating the `Origin` header and attaching the appropriate `Access-Control-Allow-*` response headers.

```
Request Origin: https://evil-site.com
CORS check: not in allowlist → 403 Forbidden — request terminated ✅
```

#### 2. Authentication
Verifies tokens (JWT, Session ID, API Key) and populates the **Request Context** with user metadata for downstream use.

```
Authorization: Bearer <token>
        │
        ▼
Middleware verifies signature + expiry
        │
        ▼
Populates context: { userId: 42, role: "admin" }
        │
        ▼
Controller retrieves userId from context — no re-verification needed
```

#### 3. Rate Limiting
Protects the server from too many requests from a single IP or client. Typically backed by Redis for distributed counting.

```
IP: 192.168.1.1 — 101st request in 60 seconds
Rate limiter: limit exceeded → 429 Too Many Requests ✅
```

#### 4. Global Error Handling
Catches unhandled errors thrown from **anywhere** in the app — controllers, services, repositories — and transforms them into clean, user-friendly JSON responses instead of raw stack traces.

```json
// Without global error handler — raw 500 leak
Internal server error: NullPointerException at UserService.java:42

// With global error handler — clean structured response
{
  "error": "Something went wrong. Please try again later.",
  "requestId": "a3f9c21d-..."
}
```

> **Note:** Never expose stack traces or internal error messages to the client in production. They reveal implementation details that attackers can exploit.

---

## III. Request Context

A **Request Context** is a shared, key-value storage state that is **scoped strictly to a single request** — it is created when the request arrives and destroyed when the response is sent.

### Why Use It?

Without a context, sharing data between middlewares and handlers requires passing it explicitly through function parameters — creating **tight coupling** across unrelated layers.

```
// ❌ Without context — userId must be threaded through every function call
authenticate(req) → getUserId() → passToController(userId) → passToService(userId) → passToRepo(userId)

// ✅ With context — any layer reads it directly
authenticate(req) → context.set("userId", 42)
...
repository.insert(context.get("userId"))   // retrieved anywhere, no coupling
```

---

### Practical Use Cases

#### 1. User ID / Roles (Authorization)

An authentication middleware verifies the token, extracts the user ID and roles, and stores them in the context. Any downstream handler or service can retrieve them without re-verifying.

```
Auth Middleware:
  → Verifies JWT
  → context.set("userId", 42)
  → context.set("role", "admin")

Controller:
  → userId = context.get("userId")   // no DB call, no re-auth
  → Creates record associated with userId
```

#### 2. Request ID (Distributed Tracing)

A unique UUID is generated at the very start of the request and stored in the context. It is then:
- Included in every log line for that request
- Passed as a header to downstream microservices
- Returned in error responses so clients can reference it in support tickets

```
Request arrives → UUID generated: "a3f9c21d-84b2-4e1f-9c3d-f72a1b0e8d45"
       │
       ├── LOG: [a3f9c21d] Auth check passed
       ├── LOG: [a3f9c21d] Order created for userId 42
       ├── LOG: [a3f9c21d] Payment charged successfully
       └── LOG: [a3f9c21d] Response sent: 201
```

This makes it trivial to trace the entire journey of a single request across logs and services.

#### 3. Cancellations & Deadlines

The context can carry **cancellation signals** or **timeouts** that propagate to downstream services. If a client disconnects mid-request, the context is cancelled — all downstream operations (DB queries, external API calls) receive the signal and abort, freeing up resources immediately.

```
Client disconnects after 2s
       │
       ▼
Context cancelled
       │
       ├── In-flight DB query → aborted
       ├── External API call  → aborted
       └── Background task    → aborted
                                     ↓
                              No wasted resources ✅
```

---

## IV. Putting It All Together

A complete request lifecycle through all three concepts:

```
POST /orders
       │
       ▼
[CORS Middleware]          Validates origin header
       │
       ▼
[Rate Limiter]             Checks request count for this IP
       │
       ▼
[Auth Middleware]          Verifies JWT → sets userId=42, role="customer" in context
       │
       ▼
[Request ID Middleware]    Generates UUID → sets requestId="a3f9c21d" in context
       │
       ▼
[Controller]               Binds JSON body → validates fields → calls service
       │
       ▼
[Service]                  Applies business rules → calls repository + payment API
       │
       ▼
[Repository]               Executes INSERT query → returns saved order
       │
       ▼
[Controller]               Returns 201 Created with order payload
       │
       ▼
[Global Error Handler]     (Not triggered — request succeeded)
       │
       ▼
Response sent to client ✅
```

---

## V. Spring Boot Implementation

### A. Controller Layer

```java
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {

        // Transformation — apply defaults
        if (request.getSortOrder() == null) {
            request.setSortOrder("asc");
        }

        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

### B. Service Layer

```java
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final EmailService emailService;

    // HTTP-agnostic — takes data, returns data
    public OrderResponse createOrder(OrderRequest request) {
        Order order = orderRepository.save(request.toEntity());
        paymentGateway.charge(order.getId(), request.getAmount());
        emailService.sendConfirmation(order);
        return OrderResponse.from(order);
    }
}
```

---

### C. Repository Layer

```java
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // One method, one kind of data
    Optional<Order> findById(Long id);
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(String status);
}
```

> Spring Data JPA generates the SQL automatically from method names — no boilerplate query code needed.

---

### D. Middlewares — Using Filters and Interceptors

Spring Boot provides two mechanisms for middleware-like behavior:

| Mechanism | Scope | Use For |
|---|---|---|
| `OncePerRequestFilter` | Servlet level | Auth, CORS, rate limiting, request ID |
| `HandlerInterceptor` | MVC level | Logging, role checks, response decoration |

**Authentication Filter:**

```java
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        if (token == null || !isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;  // does not call chain.doFilter() — request terminated
        }

        Long userId = extractUserId(token);
        request.setAttribute("userId", userId);  // store in request context

        chain.doFilter(request, response);  // equivalent of next()
    }
}
```

**CORS & ordering via `SecurityFilterChain` (Spring Security):**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())          // 1st — CORS
        .csrf(csrf -> csrf.disable())
        .addFilterBefore(authFilter,              // 2nd — Auth
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

### E. Request Context — Using `HttpServletRequest` Attributes

```java
// In Auth Filter — set value
request.setAttribute("userId", 42L);
request.setAttribute("requestId", UUID.randomUUID().toString());

// In Controller — retrieve value
@PostMapping
public ResponseEntity<?> create(HttpServletRequest request,
                                @Valid @RequestBody OrderRequest body) {

    Long userId    = (Long)   request.getAttribute("userId");
    String reqId   = (String) request.getAttribute("requestId");

    log.info("[{}] Creating order for user {}", reqId, userId);
    return ResponseEntity.status(201).body(orderService.createOrder(userId, body));
}
```

**For service-layer access without passing `HttpServletRequest` everywhere — use `RequestContextHolder`:**

```java
import org.springframework.web.context.request.*;

public Long getCurrentUserId() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return (Long) attrs.getRequest().getAttribute("userId");
}
```

---

### F. Global Error Handling

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception ex,
                                                          HttpServletRequest req) {
        String requestId = (String) req.getAttribute("requestId");

        // Log full stack trace internally
        log.error("[{}] Unhandled error: {}", requestId, ex.getMessage(), ex);

        // Return clean message to client — no stack trace exposed
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Something went wrong. Please try again later.",
                "requestId", requestId
            ));
    }
}
```

---

### Spring Boot Architecture — Quick Reference Checklist

| Concern | Spring Boot Mechanism |
|---|---|
| Controller / Handler | `@RestController` + `@RequestMapping` |
| Request body binding | `@RequestBody` |
| Validation | `@Valid` + Bean Validation annotations |
| Service layer | `@Service` |
| Repository layer | `JpaRepository` / `CrudRepository` |
| Middleware (pre-controller) | `OncePerRequestFilter` |
| Middleware (MVC level) | `HandlerInterceptor` |
| Middleware ordering | `@Order` annotation or `SecurityFilterChain` |
| Request context (per-request) | `request.setAttribute()` / `getAttribute()` |
| Request context (global access) | `RequestContextHolder` |
| Global error handling | `@RestControllerAdvice` + `@ExceptionHandler` |