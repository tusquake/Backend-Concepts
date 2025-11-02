# Secure Password Storage in Databases

A comprehensive guide to understanding and implementing secure password storage practices.

---

## Table of Contents

- [Why Password Security Matters](#why-password-security-matters)
- [Wrong Approaches](#wrong-approaches)
- [Correct Implementation](#correct-implementation)
- [Core Concepts](#core-concepts)
- [Implementation Guide](#implementation-guide)
- [Code Example](#code-example)
- [Best Practices](#best-practices)

---

## Why Password Security Matters

Passwords must remain secure even if the database is compromised. Users often reuse passwords across multiple platforms, making a single breach potentially catastrophic.

---

## Wrong Approaches

### 1. Plain Text Storage

NEVER store passwords as plain text.

**Vulnerabilities:**
- SQL injection exposes all passwords instantly
- Internal threats (employees, DBAs) can export passwords
- No recovery once exposed

### 2. Fast Hashing (MD5, SHA-1, SHA-256)

Using fast hash functions is insufficient.

**Why it fails:**
- Modern GPUs compute billions of hashes per second
- Rainbow tables enable instant password lookups
- Same password always produces same hash

**Example of rainbow table attack:**
```
Hash: 5f4dcc3b5aa765d61d8327deb882cf99
Lookup in table → Password: "password"
```

---

## Correct Implementation

### Three Pillars of Secure Password Storage

1. **Slow, Adaptive Hashing**
2. **Salting**
3. **Peppering** (optional but recommended)

---

## Core Concepts

### 1. Slow Hashing

Use computationally expensive hash functions that are intentionally slow.

**Recommended algorithms:**

**bcrypt**
- Industry standard for years
- Configurable cost factor
- Adjustable as hardware improves

**scrypt**
- Memory-hard algorithm
- Resists GPU attacks by consuming significant RAM
- Makes parallel cracking inefficient

**Argon2** (RECOMMENDED)
- Winner of Password Hashing Competition (2015)
- Most modern and secure option
- Configurable for memory, CPU, and parallelism
- OWASP top recommendation
- Use Argon2id variant when available

### 2. Salting

A salt is a unique, random string generated for each user and combined with the password before hashing.

**How it works:**
```
Alice: password = "123456", salt = "abc_salt"
Stored: hash("123456abc_salt")

Bob: password = "123456", salt = "xyz_salt"  
Stored: hash("123456xyz_salt")
```

**Salt requirements:**
- Must be cryptographically random
- Minimum 16 bytes length
- Unique per user
- Stored alongside the hash in database

**Random generation methods:**
- Node.js: `crypto.randomBytes(16)`
- Java: `SecureRandom`
- Go: `crypto/rand`
- Python: `os.urandom()`

**Why salting works:**
- Same password produces different hashes for different users
- Rainbow tables become useless (would need separate table per salt)
- Prevents pattern detection across users

**Important:** Modern libraries (bcrypt, Argon2, PBKDF2) automatically generate and embed salts in the hash output.

### 3. Peppering

A pepper is a secret value combined with password and salt, but stored separately from the database.

**Key differences from salt:**
- Salt: unique per user, stored in database
- Pepper: application-wide secret, stored outside database

**Storage locations:**
- Environment variables
- Secrets management systems (HashiCorp Vault, AWS Secrets Manager, Google Secret Manager)
- Hardware Security Modules (HSM)

**How it works:**
```
Final hash = hash(password + salt + pepper)
```

**Protection provided:**
Even if attacker steals the database (with hashes and salts), they cannot verify password guesses without the pepper.

**Two patterns:**
1. Global pepper: One secret for entire application
2. Per-user pepper: Different secret per user in external secure store

---

## Implementation Guide

### Registration Flow

1. User provides password
2. Generate cryptographically secure salt
3. Retrieve application pepper from secure storage
4. Hash: `hash(password + salt + pepper)` using slow algorithm
5. Store hash and salt in database (never store pepper in database)

### Login Flow

1. User provides email and password
2. Retrieve stored hash and salt from database using email
3. Retrieve application pepper from secure storage
4. Compute: `hash(provided_password + salt + pepper)`
5. Use secure comparison function (e.g., `bcrypt.compare()`)
6. Grant access if hashes match

---

## Code Examples

### Node.js Implementation

Using bcrypt (handles salts automatically):

```javascript
const bcrypt = require('bcrypt');

// Registration
async function registerUser(email, password) {
  const pepper = process.env.PASSWORD_PEPPER;
  const pepperedPassword = password + pepper;
  
  const saltRounds = 12;
  const hash = await bcrypt.hash(pepperedPassword, saltRounds);
  
  // Store email and hash in database
  await db.users.create({ email, password_hash: hash });
}

// Login
async function loginUser(email, password) {
  const user = await db.users.findOne({ email });
  if (!user) return false;
  
  const pepper = process.env.PASSWORD_PEPPER;
  const pepperedPassword = password + pepper;
  
  const isValid = await bcrypt.compare(pepperedPassword, user.password_hash);
  return isValid;
}
```

### Spring Boot Implementation

Using BCryptPasswordEncoder with custom pepper:

**Dependencies (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Configuration:**
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength parameter
    }
}
```

**Service Implementation:**
```java
@Service
public class UserService {
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${security.password.pepper}")
    private String pepper;
    
    // Registration
    public User registerUser(String email, String password) {
        String pepperedPassword = password + pepper;
        String hashedPassword = passwordEncoder.encode(pepperedPassword);
        
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hashedPassword);
        
        return userRepository.save(user);
    }
    
    // Login
    public boolean authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            return false;
        }
        
        User user = userOptional.get();
        String pepperedPassword = password + pepper;
        
        return passwordEncoder.matches(pepperedPassword, user.getPasswordHash());
    }
}
```

**Entity:**
```java
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    // Getters and setters
}
```

**application.properties:**
```properties
security.password.pepper=${PASSWORD_PEPPER:defaultPepperChangeInProduction}
```

**Alternative: Using Argon2 in Spring Boot**

Add dependency:
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

Configuration:
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public Argon2PasswordEncoder passwordEncoder() {
        // saltLength, hashLength, parallelism, memory, iterations
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}
```

The rest of the service implementation remains the same, just replace `BCryptPasswordEncoder` with `Argon2PasswordEncoder`.

---

## Best Practices

### Do

- Use Argon2id or bcrypt for password hashing
- Generate unique salt for every user
- Store pepper in environment variables or secrets manager
- Use secure comparison functions to prevent timing attacks
- Consider OAuth/OpenID Connect for delegated authentication

### Do Not

- Store passwords in plain text
- Use fast hashes (MD5, SHA-1, SHA-256)
- Use predictable salts (usernames, timestamps)
- Store pepper in the database
- Implement custom cryptography

### Alternative Approach: Delegated Authentication

Use OAuth 2.0 and OpenID Connect:
- "Sign in with Google"
- "Sign in with GitHub"
- "Sign in with Facebook"

**Benefits:**
- No password storage
- Security handled by experts
- Reduced liability

**Trade-offs:**
- Third-party dependency
- Service availability concerns
- May not suit all applications

**Hybrid approach:**
- Primary: OAuth providers
- Fallback: Email OTP (passwordless)

---

## Summary

### Non-Negotiable Rules

1. NEVER store passwords in plain text
2. DO NOT use old, fast hashes (MD5, SHA-1)
3. ALWAYS use modern, slow, adaptive hashing (Argon2 or bcrypt)
4. ALWAYS use unique salt per user (automatic in modern libraries)
5. CONSIDER using secret pepper stored outside database
6. OR use trusted third-party authentication providers

Protecting user passwords is a fundamental responsibility. Correct implementation builds trust; failures can be immediate, public, and irreversible.