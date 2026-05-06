# REST API Design — Complete Guide

## I. The Philosophy and History of REST

### Origins

The web traces back to **Tim Berners-Lee's** invention of **URI, HTTP, and HTML in 1990**. As the web grew exponentially, a scalability crisis emerged — there was no agreed-upon standard for how systems should communicate.

In his **2000 PhD dissertation**, **Roy Fielding** proposed **REST (Representational State Transfer)** — an architectural style, not a protocol — to solve these scalability and interoperability problems.

> **Key distinction:** REST is not a library or a framework. It is a set of constraints. Any API that follows these constraints can be called "RESTful."

---

### The 6 Constraints of REST

| # | Constraint | Description |
|---|---|---|
| 1 | **Client-Server** | Separation of concerns between the UI (Client) and data/logic (Server). Each can evolve independently. |
| 2 | **Uniform Interface** | A standardized way for all components to communicate — consistent URLs, methods, and response formats. |
| 3 | **Layered System** | Hierarchical layers (e.g., load balancers, caches, gateways) that interact only with the immediate layer below. Clients don't know or care how many layers exist. |
| 4 | **Cache** | Server responses must be explicitly labeled as cacheable or not, improving efficiency and reducing server load. |
| 5 | **Stateless** | Each request must contain **all necessary information**. The server stores no client context between requests. |
| 6 | **Code on Demand** *(Optional)* | Servers can temporarily extend client functionality by sending executable code (e.g., JavaScript). |

### Why Statelessness Matters

The stateless constraint is what makes REST APIs **horizontally scalable**. If the server held session state, every request from a client would need to reach the *same* server. With statelessness, any server in the cluster can handle any request.

```
❌ Stateful — session tied to Server A
Client → Server A (has session) ✅
Client → Server B (no session)  ❌ fails

✅ Stateless — all context in the request (JWT, params)
Client → Server A ✅
Client → Server B ✅
Client → Server C ✅
```

---

## II. Anatomy of a Production-Grade URL

A well-structured API URL communicates intent, ownership, and hierarchy at a glance.

```
https://api.example.com/v1/organizations/42/projects/harry-potter
  │          │           │       │         │      │
  │          │           │       │         │      └── Resource slug (hyphenated)
  │          │           │       │         └───────── Parent resource ID
  │          │           │       └─────────────────── Plural noun resource
  │          │           └─────────────────────────── Version prefix
  │          └─────────────────────────────────────── API subdomain
  └────────────────────────────────────────────────── Protocol
```

### Rules for URL Design

**1. Subdomain**
Always serve APIs from a dedicated subdomain: `api.example.com`, not `example.com/api`.

**2. Versioning**
Always include a version prefix in the path. This allows breaking changes without impacting existing clients.

```
✅ api.example.com/v1/books
✅ api.example.com/v2/books   ← breaking change deployed safely
❌ api.example.com/books      ← no version = no safe upgrade path
```

**3. Plural Nouns**
Resources are things (nouns), not actions (verbs). Always use the plural form.

```
✅ /books
✅ /organizations
❌ /book
❌ /getOrganizations
❌ /fetchAllUsers
```

**4. Slugs and IDs — Use Hyphens**
Use hyphens instead of underscores or spaces for readability and SEO compatibility.

```
✅ /books/harry-potter
❌ /books/harry_potter
❌ /books/harry%20potter
```

**5. Hierarchy with Forward Slash**
The `/` represents a **parent-child relationship**. Nest only as deep as necessary — overly deep nesting becomes hard to read.

```
✅ /organizations/1/projects        ← projects belonging to org 1
✅ /organizations/1/projects/42     ← specific project within org 1
❌ /organizations/1/projects/42/tasks/7/comments/3/replies   ← too deep
```

> **Practical rule:** If nesting goes beyond 2–3 levels, consider flattening. `/comments/3/replies` is often cleaner than the deeply nested alternative.

---

## III. HTTP Methods and Idempotency

**Idempotency** means performing an action **multiple times has the same effect as performing it once**. This is critical for safe retries — e.g., when a network timeout causes a client to resend a request.

### Method Reference Table

| Method | Idempotent? | Purpose | Example |
|---|---|---|---|
| `GET` | ✅ Yes | Retrieve data — no side effects | `GET /books` |
| `PUT` | ✅ Yes | Completely replace a resource | `PUT /books/1` |
| `PATCH` | ✅ Yes | Update partial fields of a resource | `PATCH /books/1` |
| `DELETE` | ✅ Yes | Remove a resource; repeated calls yield same state (deleted) | `DELETE /books/1` |
| `POST` | ❌ No | Create a new resource or perform a custom action | `POST /books` |

### PUT vs PATCH

```json
// Existing resource
{ "title": "Dune", "author": "Frank Herbert", "status": "active" }

// PUT — replaces the entire resource (missing fields are nulled/removed)
PUT /books/1
{ "title": "Dune Part Two" }
→ Result: { "title": "Dune Part Two", "author": null, "status": null }

// PATCH — updates only the fields provided
PATCH /books/1
{ "title": "Dune Part Two" }
→ Result: { "title": "Dune Part Two", "author": "Frank Herbert", "status": "active" }
```

### Custom Actions

For operations that don't map cleanly to standard CRUD (e.g., "Archive", "Send Email", "Publish"), use a `POST` request with the action name appended to the URL:

```
POST /organizations/1/archive
POST /invoices/42/send
POST /articles/7/publish
POST /users/99/deactivate
```

> **Why POST?** Custom actions are not idempotent — sending an email twice sends two emails. POST correctly signals this.

---

## IV. Advanced List API Patterns

A professional `GET /resources` endpoint should support three pillars: **Pagination**, **Sorting**, and **Filtering**. Without them, list endpoints become unusable at scale.

### 1. Pagination

Prevents overwhelming the client and server by returning only a portion of the data at a time.

**Request Parameters:**

| Param | Type | Description |
|---|---|---|
| `page` | `integer` | Which page to return (1-indexed) |
| `limit` | `integer` | Number of records per page |

**Response Metadata:**

Always include pagination metadata so the frontend can build UI controls (next/prev buttons, page counts):

```json
{
  "data": [ ... ],
  "pagination": {
    "total": 284,
    "page": 3,
    "limit": 20,
    "total_pages": 15
  }
}
```

```
GET /organizations?page=3&limit=20
```

> **Note:** Return `total_pages` so clients never have to compute it themselves. Return `total` so clients can show "284 results found."

---

### 2. Sorting

**Request Parameters:**

| Param | Type | Example Values |
|---|---|---|
| `sort_by` | `string` | `created_at`, `name`, `status` |
| `sort_order` | `string` | `asc`, `desc` |

**Safe Defaults:**
Always default to a logical sort — typically `created_at desc` (newest first). Never return results in an undefined/random order.

```
GET /organizations?sort_by=created_at&sort_order=desc
GET /books?sort_by=name&sort_order=asc
```

> **Security note:** Validate `sort_by` against an allowlist of permitted fields. Passing raw `sort_by` values directly into a SQL `ORDER BY` clause is a SQL injection risk.

---

### 3. Filtering

Allow clients to narrow down results using query parameters.

```
GET /organizations?status=active
GET /books?author=frank-herbert&status=published
GET /orders?created_after=2024-01-01&created_before=2024-12-31
```

**Combining all three pillars:**

```
GET /organizations?status=active&sort_by=created_at&sort_order=desc&page=1&limit=20
```

---

## V. Best Practices for API Designers

### 1. Interactive Documentation

Always maintain a **Swagger / OpenAPI** playground. It serves as both documentation and a live testing environment — reducing the need for back-and-forth between frontend and backend teams.

> Tools: Swagger UI, Redoc, Scalar, Stoplight

---

### 2. Consistency

Apply the same conventions across **every** resource in the API. Inconsistency is the fastest way to frustrate API consumers.

| Convention | Rule |
|---|---|
| JSON keys | `camelCase` (e.g., `createdAt`, not `created_at`) |
| Route naming | Always plural nouns |
| Date format | ISO 8601 — `YYYY-MM-DDTHH:MM:SSZ` |
| Error format | Always the same structure across all endpoints |

---

### 3. Sane Defaults

Don't force the client to provide obvious information. Apply sensible defaults server-side.

```json
// Client sends — minimal payload
POST /organizations
{ "name": "Acme Corp" }

// Server applies defaults automatically
{
  "id": 1,
  "name": "Acme Corp",
  "status": "active",        ← defaulted
  "created_at": "2024-01-15" ← defaulted
}
```

---

### 4. No Abbreviations

Use full, descriptive names. Clarity is more valuable than saving a few characters.

```
✅ description    ❌ desc
✅ quantity       ❌ qty
✅ organization   ❌ org
✅ created_at     ❌ crtd
```

---

### 5. Empty States — Never Return 404 for Empty Lists

If a List API has no results, return an **empty array** with `200 OK` — not a `404 Not Found`. The resource (the list endpoint) exists; it simply has no items.

```json
// ✅ Correct — list exists, just empty
GET /organizations?status=archived
→ 200 OK
{ "data": [], "pagination": { "total": 0, "page": 1, "total_pages": 0 } }

// ❌ Wrong — confuses "endpoint not found" with "no results"
GET /organizations?status=archived
→ 404 Not Found
```

> Reserve `404` for when the **resource itself** doesn't exist (e.g., `GET /organizations/9999` where org 9999 does not exist).

---

### 6. HTTP Status Code Reference *(Added)*

| Code | Name | When to Use |
|---|---|---|
| `200` | OK | Successful GET, PATCH, DELETE |
| `201` | Created | Successful POST that created a resource |
| `204` | No Content | Successful DELETE with no body to return |
| `400` | Bad Request | Validation failure — client sent invalid data |
| `401` | Unauthorized | Missing or invalid authentication token |
| `403` | Forbidden | Authenticated but not permitted to access this resource |
| `404` | Not Found | Specific resource does not exist |
| `409` | Conflict | Duplicate resource (e.g., email already registered) |
| `422` | Unprocessable Entity | Syntactically valid but semantically wrong |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Unexpected server-side failure |

---

## VI. REST in Spring Boot

### A. Controller Setup

```java
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    // GET /api/v1/organizations
    @GetMapping
    public ResponseEntity<PagedResponse<Organization>> list(
            @RequestParam(defaultValue = "1")   int page,
            @RequestParam(defaultValue = "20")  int limit,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false)     String status) {

        return ResponseEntity.ok(service.list(page, limit, sortBy, sortOrder, status));
    }

    // GET /api/v1/organizations/42
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // POST /api/v1/organizations
    @PostMapping
    public ResponseEntity<Organization> create(@Valid @RequestBody CreateOrgRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    // PATCH /api/v1/organizations/42
    @PatchMapping("/{id}")
    public ResponseEntity<Organization> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateOrgRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    // DELETE /api/v1/organizations/42
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();  // 204 No Content
    }

    // POST /api/v1/organizations/42/archive  ← Custom Action
    @PostMapping("/{id}/archive")
    public ResponseEntity<Organization> archive(@PathVariable Long id) {
        return ResponseEntity.ok(service.archive(id));
    }
}
```

---

### B. Pagination Response Wrapper

```java
public class PagedResponse<T> {

    private List<T> data;
    private PaginationMeta pagination;

    @Data
    @AllArgsConstructor
    public static class PaginationMeta {
        private long total;
        private int page;
        private int limit;
        private int totalPages;
    }
}
```

**Response:**

```json
{
  "data": [ { "id": 1, "name": "Acme Corp", "status": "active" } ],
  "pagination": {
    "total": 284,
    "page": 1,
    "limit": 20,
    "totalPages": 15
  }
}
```

---

### C. Sorting — Allowlist Validation

Always validate `sort_by` against permitted fields to prevent SQL injection:

```java
private static final Set<String> ALLOWED_SORT_FIELDS =
    Set.of("created_at", "name", "status");

public PagedResponse<Organization> list(int page, int limit,
                                         String sortBy, String sortOrder,
                                         String status) {
    if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
        throw new BadRequestException("Invalid sort field: " + sortBy);
    }

    Sort sort = sortOrder.equalsIgnoreCase("asc")
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page - 1, limit, sort);
    Page<Organization> result = repository.findAll(pageable);

    return new PagedResponse<>(
        result.getContent(),
        new PaginationMeta(result.getTotalElements(), page, limit,
                           result.getTotalPages())
    );
}
```

---

### D. Filtering with Spring Data JPA Specifications

```java
import org.springframework.data.jpa.domain.Specification;

public class OrganizationSpec {

    public static Specification<Organization> hasStatus(String status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }
}

// In service
Page<Organization> result = repository.findAll(
    OrganizationSpec.hasStatus(status), pageable
);
```

---

### E. OpenAPI / Swagger Documentation

Add the dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

Swagger UI is then available at: `http://localhost:8080/swagger-ui.html`

Annotate your endpoints for richer docs:

```java
@Operation(summary = "List all organizations",
           description = "Supports pagination, sorting, and filtering by status")
@ApiResponse(responseCode = "200", description = "Paginated list of organizations")
@GetMapping
public ResponseEntity<PagedResponse<Organization>> list(...) { ... }
```

---

### Spring Boot REST — Quick Reference Checklist

| Concern | Spring Boot Mechanism |
|---|---|
| Route + version prefix | `@RequestMapping("/api/v1/resource")` |
| GET list with pagination | `@GetMapping` + `Pageable` / `PageRequest` |
| GET single resource | `@GetMapping("/{id}")` + `@PathVariable` |
| POST create | `@PostMapping` + `@RequestBody` + `@Valid` → `201 Created` |
| PATCH partial update | `@PatchMapping("/{id}")` → `200 OK` |
| DELETE | `@DeleteMapping("/{id}")` → `204 No Content` |
| Custom action | `@PostMapping("/{id}/action-name")` |
| Sorting validation | Allowlist `Set<String>` before passing to `Sort.by()` |
| Filtering | Spring Data JPA `Specification` |
| Pagination metadata | Custom `PagedResponse<T>` wrapper |
| Interactive docs | `springdoc-openapi` → `/swagger-ui.html` |
| 404 for missing resource | `throw new ResponseStatusException(HttpStatus.NOT_FOUND)` |
| Empty list | Return `200 OK` with `[]` — never `404` |