# Backend Validation & Transformation — Complete Guide

## I. Architectural Context

Before diving into validations, it's important to understand where they fit in a typical backend stack:

| Layer | Responsibility |
|---|---|
| **Repository Layer** | Persistent storage — SQL, Redis, etc. |
| **Service Layer** | Business logic, email notifications, webhooks |
| **Controller Layer** | HTTP entry point — where validations and transformations live |

> **Rule:** Validations and transformations must occur at the **controller layer** — after the route is matched but *before* the service layer is touched.

---

## II. Why Backend Validation Matters

Backend validation is a **security and stability requirement**, not a "nice-to-have."

### The PostgreSQL Example

- A database column (e.g., `name`) expects a `TEXT` type.
- A client sends `0` (a number) instead of a string.
- Without validation, the error isn't caught until the repository executes the SQL.
- **Result:** The database call fails → server returns `500 Internal Server Error`.
- **Fix:** Entry-point validation catches this early → returns `400 Bad Request`.

**Benefits of early validation:**
- Saves server and database resources
- Provides clear, actionable error feedback to the client
- Prevents leaking internal system details via 500 errors

---

## III. Types of Validation

### 1. Type Validation

The most fundamental layer — ensures a field is the correct primitive type.

- `string`, `number`, `boolean`, `array`
- **Recursive checks:** An array field can be validated to ensure *every element* inside is also of a specific type (e.g., an array of strings, not a mixed array).

```json
// ✅ Valid
{ "tags": ["javascript", "node", "backend"] }

// ❌ Invalid
{ "tags": ["javascript", 42, true] }
```

---

### 2. Syntactic Validation

Checks whether a string follows a specific **pattern or structure**.

| Field | Rule |
|---|---|
| Email | Must contain `@` and a valid domain suffix |
| Phone number | Must include a country code and correct digit count |
| Date | Must match `YYYY-MM-DD` format strictly |

```json
// ✅ Valid email
{ "email": "user@example.com" }

// ❌ Invalid email
{ "email": "userexample.com" }
```

---

### 3. Semantic Validation

Checks whether the data makes **logical sense in the real world**, beyond just format.

| Field | Rule |
|---|---|
| Date of Birth | Must not be a future date (e.g., 2026 is syntactically valid but semantically wrong) |
| Age | Must fall within a logical human range (e.g., 0–120) |
| Price / Quantity | Must not be negative |

```json
// ✅ Syntactically correct, but semantically invalid
{ "date_of_birth": "2026-01-01" }  // Future date — rejected

// ❌ Out of range
{ "age": 430 }  // Exceeds human lifespan — rejected
```

> **Note:** Semantic validation also applies to business rules — e.g., a `discount_percentage` field should not exceed `100`, or a `checkout_quantity` should not exceed available stock.

---

### 4. Complex (Dependent) Validation

Validations that depend on the **state of other fields** in the same request.

- **Integrity checks:** `password` and `password_confirmation` must be identical.
- **Conditional requirements:** If `married: true`, then `partner_name` is required. If `married: false`, `partner_name` is optional.

```json
// ✅ Valid
{ "married": true, "partner_name": "Jane Doe" }

// ❌ Invalid — partner_name required when married is true
{ "married": true }

// ✅ Valid — partner_name optional when married is false
{ "married": false }
```

> **Note:** Another common example is payment forms — if `payment_method: "card"`, then `card_number`, `cvv`, and `expiry` become required; if `payment_method: "cash"`, they are not.

---

## IV. Transformation & Type Casting

Transformation is the process of **modifying or normalizing data** before it reaches the service layer.

### The Query Parameter Problem

All query parameters arrive as **strings** by default:

```
GET /articles?page=2&limit=10
// page = "2" (string), limit = "10" (string)
```

If your service logic expects a number, it will fail without transformation. Always **cast** query params to the appropriate type at the controller level.

### Common Transformations

| Input | Transformation | Output |
|---|---|---|
| `"2"` (query param) | Cast to integer | `2` |
| `"USER@EXAMPLE.COM"` | Normalize to lowercase | `"user@example.com"` |
| `"9876543210"` (phone) | Add country code prefix | `"+919876543210"` |
| `" hello "` (string) | Trim whitespace | `"hello"` |

> **Note:** Transformation also includes **sanitization** — stripping HTML tags or script content from string fields to prevent XSS attacks before data is stored or rendered.

---

## V. Frontend vs. Backend — The Security Gap

| | Frontend Validation | Backend Validation |
|---|---|---|
| **Purpose** | User Experience (UX) | Security & Data Integrity |
| **Can be bypassed?** | ✅ Yes, easily | ❌ No — it's the last line of defense |
| **Tools to bypass** | Postman, Insomnia, curl, browser DevTools | — |

### The Threat Model

A malicious user can **completely bypass your UI** and hit your API directly:

```bash
# A user can do this, entirely ignoring your frontend forms
curl -X POST https://yourapi.com/users \
  -H "Content-Type: application/json" \
  -d '{ "age": 999, "email": "notanemail", "role": "admin" }'
```

### The Rule

> **Your backend must be as strict as possible, assuming that all client-side validation has already been ignored or bypassed.**

> **Mass Assignment Protection:** Never blindly pass raw request bodies into your database layer. Explicitly whitelist the fields your service is allowed to accept. A malicious user could inject fields like `{ "role": "admin" }` or `{ "is_verified": true }` that should never be user-controlled.

---

## VI. Validation & Transformation in Spring Boot

Spring Boot provides a **first-class validation ecosystem** via the **Bean Validation API (JSR-380)** backed by **Hibernate Validator**. Validation lives in the controller layer through annotations — no manual if-checks needed.

### Setup — Add the Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### Step 1 — Type & Syntactic Validation with Annotations

Annotate your **DTO (Data Transfer Object)** fields directly. Spring validates them before your controller method even runs.

```java
import jakarta.validation.constraints.*;

public class UserRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @Email(message = "Must be a valid email address")
    @NotNull
    private String email;

    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 120, message = "Age must not exceed 120")
    private int age;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in YYYY-MM-DD format")
    private String dateOfBirth;

    @NotEmpty(message = "Tags must not be empty")
    private List<@NotBlank String> tags;  // recursive — each element validated too
}
```

| Annotation | Validation Type | What It Checks |
|---|---|---|
| `@NotNull` | Type | Field must not be null |
| `@NotBlank` | Type + Syntactic | String must not be null or whitespace |
| `@Email` | Syntactic | Valid email format |
| `@Pattern` | Syntactic | Matches a regex |
| `@Min` / `@Max` | Semantic | Numeric range check |
| `@Size` | Semantic | String or collection length |
| `@Past` / `@Future` | Semantic | Date must be in the past/future |

---

### Step 2 — Activate Validation in the Controller

Use `@Valid` on the `@RequestBody` parameter to trigger validation:

```java
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public ResponseEntity<String> createUser(@Valid @RequestBody UserRequest request) {
        // Reaches here only if all validations pass
        return ResponseEntity.ok("User created");
    }
}
```

> If validation fails, Spring automatically throws a `MethodArgumentNotValidException` — **before** your service layer is ever called.

---

### Step 3 — Semantic Validation with `@Past` / `@Future`

```java
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

public class UserRequest {

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;   // 2026-01-01 → rejected ✅
}
```

---

### Step 4 — Complex (Dependent) Validation with Custom Validators

For cross-field validation (e.g., `password` must match `confirmPassword`), create a **custom constraint annotation**:

**1. Define the annotation:**

```java
import jakarta.validation.*;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**2. Implement the validator:**

```java
public class PasswordMatchValidator
        implements ConstraintValidator<PasswordMatch, RegistrationRequest> {

    @Override
    public boolean isValid(RegistrationRequest request, ConstraintValidatorContext ctx) {
        return request.getPassword() != null &&
               request.getPassword().equals(request.getConfirmPassword());
    }
}
```

**3. Apply it to the DTO:**

```java
@PasswordMatch
public class RegistrationRequest {
    private String password;
    private String confirmPassword;
}
```

---

### Step 5 — Query Parameter Validation & Transformation

For `@RequestParam` and `@PathVariable`, add `@Validated` at the class level:

```java
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/articles")
@Validated  // ← enables constraint validation on method parameters
public class ArticleController {

    @GetMapping
    public ResponseEntity<?> list(
        @RequestParam @Min(1) int page,
        @RequestParam @Min(1) @Max(100) int limit
    ) {
        // page and limit are already integers — Spring auto-casts from query string
        return ResponseEntity.ok(...);
    }
}
```

> Spring Boot automatically **type-casts** query parameters (e.g., `?page=2` → `int page = 2`). You get transformation for free — no manual parsing needed.

**Normalization example** — transform data in the controller before passing to the service:

```java
@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest request) {
    request.setEmail(request.getEmail().toLowerCase().trim());  // normalize
    userService.create(request);
    return ResponseEntity.status(201).build();
}
```

---

### Step 6 — Return Structured Validation Errors with `@ExceptionHandler`

By default, Spring returns a verbose error payload. Override it with a clean, structured response using `@RestControllerAdvice`:

```java
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> Map.of(
                "field", err.getField(),
                "message", err.getDefaultMessage()
            ))
            .toList();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("errors", errors));
    }
}
```

**Response returned to the client:**

```json
{
  "errors": [
    { "field": "email", "message": "Must be a valid email address" },
    { "field": "age",   "message": "Age must not exceed 120" }
  ]
}
```

---

### Spring Boot Validation — Quick Reference Checklist

| Task | How |
|---|---|
| Enable validation | Add `spring-boot-starter-validation` dependency |
| Validate request body | `@Valid` on `@RequestBody` parameter |
| Validate query params | `@Validated` on class + constraints on `@RequestParam` |
| Type validation | `@NotNull`, `@NotBlank`, `@NotEmpty` |
| Syntactic validation | `@Email`, `@Pattern(regexp = "...")` |
| Semantic validation | `@Min`, `@Max`, `@Size`, `@Past`, `@Future` |
| Cross-field validation | Custom `@Constraint` + `ConstraintValidator` |
| Auto type-cast query params | Built-in — Spring handles `String → int/long/boolean` |
| Normalize data | Manual transform in controller before service call |
| Structured error response | `@RestControllerAdvice` + `@ExceptionHandler` |

---

## VII. Additional Best Practices

### Return Structured Validation Errors

Don't just return a generic `400 Bad Request`. Return a structured error body so clients can pinpoint exactly what failed:

```json
{
  "errors": [
    { "field": "email", "message": "Must be a valid email address" },
    { "field": "age", "message": "Must be between 0 and 120" }
  ]
}
```

### Use a Validation Library

Don't write validation logic from scratch. Use proven libraries:

| Language / Runtime | Library |
|---|---|
| Node.js | `zod`, `joi`, `yup`, `class-validator` |
| Python | `pydantic`, `marshmallow` |
| Go | `validator` |
| Java | `javax.validation` (Bean Validation) |

### Validate at Every Trust Boundary

Even in microservice architectures, each service should validate its own inputs — **never trust data just because it came from another internal service.**