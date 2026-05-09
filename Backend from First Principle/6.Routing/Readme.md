# HTTP Routing — Complete Guide

## I. What Is Routing?

Routing is the process of **mapping an incoming URL (and HTTP method) to specific server-side logic** — a handler function that knows how to respond to that request.

### HTTP Methods vs. Routes

```
HTTP Method (The "What"):   Expresses the intent or action
Route / Path (The "Where"): Expresses the resource being acted upon

Combined, they form a unique key that the server uses to find the right handler:

  GET    /api/books   →  "Give me a list of books"
  POST   /api/books   →  "Add a new book"
  DELETE /api/books   →  "Delete all books"
         ──────────
         Same URL — completely different handlers
```

| | HTTP Method | Route |
|---|---|---|
| Answers | What action? | Which resource? |
| Examples | GET, POST, PUT, DELETE | `/api/users`, `/api/users/:id` |
| Changes behavior | Intent (read vs. write) | Target (all users vs. one user) |
| Combined | `GET /api/users` | Unique handler key |

> **Rule of thumb:** The HTTP method is the verb; the route is the noun. A server concatenates both to identify a unique handler — `GET /api/books` and `POST /api/books` never clash, even though they share the same path.

---

## II. Static vs. Dynamic Routes

### Static Routes

Paths that are **constant strings** — they never change.

```
GET  /api/books          → returns all books
GET  /api/health         → returns server health status
POST /api/auth/login     → authenticates a user
```

### Dynamic Routes (Path Parameters)

Paths with **variable slots** that allow a single route pattern to match many specific inputs.

```
Pattern:   /api/users/:id
Matches:   /api/users/1
           /api/users/42
           /api/users/8f3c-...

Pattern:   /api/orders/:orderId/items/:itemId
Matches:   /api/orders/99/items/7
```

```
How the server parses it:

  Request: GET /api/users/42
                           ──
                           │
  Route pattern: /api/users/:id
                            ───
                             └── id = "42"  ← extracted and passed to handler
```

| Route Type | Pattern | Use Case |
|---|---|---|
| Static | `/api/books` | List all resources |
| Path Parameter | `/api/books/:id` | Fetch one specific resource |
| Multiple Parameters | `/api/users/:userId/posts/:postId` | Nested resource lookup |

---

## III. Query Parameters

Query parameters are **key-value pairs appended after a `?`** in the URL. They carry metadata without changing the route path itself.

```
URL anatomy:

  /api/search?query=laptop&page=2&limit=20&sort=price_asc
  ───────────┬────────────────────────────────────────────
      path   └── query string (key=value pairs, separated by &)
```

### Why Query Parameters?

GET requests have **no body**. Query parameters are how you pass filters, pagination, and sorting options without breaking the RESTful route structure.

```
❌ Doing it wrong (path parameters for optional filters):
   /api/books/fiction/asc/page2        ← fragile, order-dependent, hard to extend

✅ Doing it right (query parameters for optional filters):
   /api/books?genre=fiction&sort=asc&page=2
```

### Common Applications

| Use Case | Example |
|---|---|
| Pagination | `?page=2&limit=20` |
| Filtering | `?status=active&role=admin` |
| Sorting | `?sort=created_at&order=desc` |
| Search | `?query=laptop&category=electronics` |
| Date range | `?from=2024-01-01&to=2024-03-31` |

```
Path parameters    → identify a specific resource  (required, part of the noun)
Query parameters   → modify or filter the response (optional, adjectives/adverbs)

  GET /api/users/42/orders?status=shipped&page=1
              ──           ────────────────────
              │                     │
        "User 42's orders"    "only shipped, page 1"
```

---

## IV. Nested Routing

Nested routes express **hierarchical relationships between resources**.

```
Flat:
  GET /posts/456       ← which user's post? ambiguous

Nested:
  GET /api/users/123/posts/456
           ─────────────────
           "User 123 → their posts → post 456"
```

### Semantic Chain

```
GET /api/users/123/posts/456/comments/789

  Look in:   users
  Find:      user with id=123
  Look in:   their posts
  Find:      post with id=456
  Look in:   its comments
  Find:      comment with id=789
```

Each segment narrows scope — the path reads like a breadcrumb trail through your data model.

### Guidelines

```
✅ Good nesting — reflects a real ownership relationship:
   /api/users/:userId/orders
   /api/courses/:courseId/lessons/:lessonId

❌ Over-nesting — more than 2–3 levels deep is hard to use:
   /api/companies/:companyId/departments/:deptId/teams/:teamId/members/:memberId/tasks/:taskId
   → flatten it: /api/tasks/:taskId?memberId=...
```

---

## V. Versioning and Deprecation

Route versioning lets you introduce **breaking changes without breaking existing clients**.

```
Unversioned (dangerous):
  GET /api/products         ← if you change the response shape, every client breaks

Versioned (safe):
  GET /v1/products          ← old clients keep working
  GET /v2/products          ← new response shape for new clients
```

### The Versioning Workflow

```
1. V1 released                  → all clients use /v1/products

2. Breaking change needed       → new response shape required
   (e.g., mobile app redesign,  → release /v2/products alongside V1
    field renamed, type changed)

3. Migration window             → clients notified, given time to upgrade
   (typically weeks–months)

4. V1 deprecated                → return deprecation warning headers
   Header: Sunset: Sat, 31 Dec 2024 23:59:59 GMT

5. V1 removed                   → 410 Gone or redirect to V2
```

### Version Placement Options

| Strategy | Example | Notes |
|---|---|---|
| URL prefix *(most common)* | `/v1/users` | Explicit, easy to test in browser |
| Header | `Accept: application/vnd.api+json;version=1` | Cleaner URLs, harder to test |
| Query param | `/users?version=1` | Simple, but pollutes query string |

> **Recommendation:** URL prefix versioning (`/v1/`, `/v2/`) is the most widely used and easiest to reason about. Only route the parts of your API that actually changed — shared, stable routes don't need separate versioned copies.

---

## VI. Catch-All Routes

A catch-all route acts as a **final safety net** — if no defined route matches the incoming request, the catch-all handler fires.

```
Route matching order (top to bottom):

  GET /api/users       → matched ✅ → UserListHandler
  GET /api/users/:id   → matched ✅ → UserDetailHandler
  POST /api/auth/login → matched ✅ → AuthHandler
  ...
  /*                   → nothing matched → CatchAllHandler → 404 Not Found
```

### Why It Matters

Without a catch-all, an unmatched route either crashes the server or returns a raw, unformatted error. A catch-all lets you return a consistent, user-friendly error response.

```
Typical catch-all behavior:

  Client: GET /api/typo-in-path
                │
                ▼
  No route matched
                │
                ▼
  Catch-all fires → return structured 404 response:
  {
    "error": "Not Found",
    "message": "No route matched GET /api/typo-in-path",
    "status": 404
  }
```

---

## VII. Routing in Spring Boot

### A. Basic Route Definitions

```java
@RestController
@RequestMapping("/api/v1")
public class BookController {

    // Static route
    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookService.findAll();
    }

    // Dynamic route — path parameter
    @GetMapping("/books/{id}")
    public Book getBook(@PathVariable Long id) {
        return bookService.findById(id);
    }

    // Query parameters
    @GetMapping("/books/search")
    public Page<Book> searchBooks(
        @RequestParam String query,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "title") String sort
    ) {
        return bookService.search(query, page, limit, sort);
    }

    // POST with request body
    @PostMapping("/books")
    public ResponseEntity<Book> createBook(@Valid @RequestBody CreateBookRequest req) {
        Book book = bookService.create(req);
        return ResponseEntity.status(201).body(book);
    }
}
```

---

### B. Path Parameters and Nested Routes

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    // Nested: /api/v1/users/{userId}/orders
    @GetMapping("/{userId}/orders")
    public List<Order> getUserOrders(
        @PathVariable Long userId,
        @RequestParam(required = false) String status  // optional filter
    ) {
        return orderService.findByUser(userId, status);
    }

    // Nested: /api/v1/users/{userId}/orders/{orderId}
    @GetMapping("/{userId}/orders/{orderId}")
    public Order getUserOrder(
        @PathVariable Long userId,
        @PathVariable Long orderId
    ) {
        return orderService.findByUserAndId(userId, orderId);
    }
}
```

---

### C. API Versioning

```java
// V1 Controller
@RestController
@RequestMapping("/api/v1/products")
public class ProductControllerV1 {

    @GetMapping("/{id}")
    public ProductResponseV1 getProduct(@PathVariable Long id) {
        return productService.getV1(id);
    }
}

// V2 Controller — new response shape, old clients unaffected
@RestController
@RequestMapping("/api/v2/products")
public class ProductControllerV2 {

    @GetMapping("/{id}")
    public ProductResponseV2 getProduct(@PathVariable Long id) {
        return productService.getV2(id);  // richer/restructured response
    }
}
```

**Sunset header for deprecation:**

```java
@GetMapping("/{id}")
public ResponseEntity<ProductResponseV1> getProduct(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header("Sunset", "Sat, 31 Dec 2024 23:59:59 GMT")
        .header("Deprecation", "true")
        .body(productService.getV1(id));
}
```

---

### D. Catch-All / Global 404 Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Fires when no route matches
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(
            "Not Found",
            "No route matched " + ex.getHttpMethod() + " " + ex.getRequestURL(),
            404
        ));
    }

    // Fires when the route matches but the method doesn't (e.g., POST to a GET-only route)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException ex
    ) {
        return ResponseEntity.status(405).body(new ErrorResponse(
            "Method Not Allowed",
            ex.getMethod() + " is not supported for this route",
            405
        ));
    }
}
```

Enable `NoHandlerFoundException` in `application.properties`:

```properties
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false
```

---

### E. Complete Route Structure Example

```java
// A well-organized, versioned API surface:

GET    /api/v1/users                    → list all users (paginated)
POST   /api/v1/users                    → create a new user
GET    /api/v1/users/:id                → get one user
PUT    /api/v1/users/:id                → replace a user
PATCH  /api/v1/users/:id                → partial update a user
DELETE /api/v1/users/:id                → delete a user

GET    /api/v1/users/:userId/posts      → list a user's posts
POST   /api/v1/users/:userId/posts      → create a post for a user
GET    /api/v1/users/:userId/posts/:id  → get one specific post

GET    /api/v1/posts/search?query=...   → search posts (query param)
GET    /api/v1/posts?page=2&limit=20    → paginated post list

/*                                      → 404 catch-all
```

---

## VIII. Quick Reference Checklist

| Concern | Pattern / Mechanism |
|---|---|
| Unique handler key | HTTP method + route path combined |
| Static route | Constant path string: `/api/books` |
| Path parameter | Variable slot in path: `/api/books/:id` |
| Multiple path params | `/api/users/:userId/posts/:postId` |
| Optional filtering/sorting | Query parameters: `?sort=asc&page=2` |
| Nested resource | Hierarchical path: `/api/users/:id/orders` |
| Breaking API changes | URL versioning: `/v1/`, `/v2/` |
| Deprecation signal | `Sunset` response header + migration window |
| Unmatched requests | Catch-all wildcard `/*` → structured 404 |
| Method mismatch | 405 Method Not Allowed handler |
| Spring static route | `@GetMapping("/path")` |
| Spring path param | `@PathVariable` annotation |
| Spring query param | `@RequestParam` annotation |
| Spring global 404 | `@RestControllerAdvice` + `NoHandlerFoundException` |