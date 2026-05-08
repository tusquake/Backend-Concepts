# HTTP — Complete Guide for Backend Engineers

## I. Core Principles of HTTP

HTTP is the foundation of all web communication. Before diving into methods, headers, and caching, three fundamental principles govern how HTTP behaves.

---

### 1. Statelessness

HTTP has no memory of past interactions. Every request is treated as if it is the first one — the server retains nothing about previous exchanges.

```
❌ What HTTP does NOT do:

  Request 1: "Hi, I'm Alice, here's my password"
  Request 2: "Show me my orders"
             → Server: "Who are you?" ← has no memory of Request 1

✅ What HTTP actually does:

  Request 1: POST /login → { username, password }
             ← Server: { token: "eyJ..." }

  Request 2: GET /orders
             Authorization: Bearer eyJ...   ← client carries its own identity
             ← Server: { orders: [...] }
```

Every request must be **self-contained**, carrying all context it needs (auth tokens, session info, preferences).

**Why this is a feature, not a bug:**

```
Stateless servers:
  ├── No session state to synchronize between instances
  ├── Any server instance can handle any request
  └── Horizontal scaling is trivially easy

Stateful servers (e.g., WebSocket sessions):
  ├── Client must always reconnect to the same instance
  ├── Sticky sessions or shared state stores required
  └── Scaling becomes significantly more complex
```

---

### 2. The Client-Server Model

Communication is always **client-initiated**. The server never pushes data unless the client first opened the connection or made a request.

```
Client (Browser / Mobile App)              Server
────────────────────────────               ──────
                                           waiting...
"GET /api/posts"  ───────────────────────► receives request
                                           processes it
{"posts": [...]} ◄───────────────────────  sends response
                                           waiting...

The server never speaks first.
(WebSockets and SSE are explicit exceptions — the client still initiates.)
```

---

### 3. TCP Reliability

HTTP runs on top of **TCP (Transmission Control Protocol)**, which guarantees reliable, ordered delivery of data.

```
TCP guarantees:
  ├── Every packet arrives (retransmitted if lost)
  ├── Packets arrive in order (reordered if needed)
  ├── No silent data corruption (checksums)
  └── Congestion control (won't flood the network)

Without TCP:
  A 500KB JSON response split across many packets
  could arrive partially, out of order, or not at all.
  TCP ensures the full response arrives correctly. ✅
```

> HTTP/3 moves to QUIC (UDP-based) but reimplements reliability at the QUIC layer — the reliability guarantee remains, just without TCP's overhead.

---

## II. HTTP Versions — The Evolution of Performance

Each version of HTTP was introduced to solve specific performance bottlenecks in the previous one.

```
HTTP 1.0      HTTP 1.1         HTTP 2.0              HTTP 3.0
────────      ────────         ────────              ────────
One TCP       Persistent       Multiplexing          QUIC (UDP)
connection    connections      + binary framing      No TCP
per request   + chunked        + header              head-of-line
              encoding         compression           blocking
```

---

### HTTP 1.0 — One Connection Per Request

```
Client                          Server
  │── TCP Handshake ──────────►  │
  │── GET /index.html ─────────► │
  │◄─ 200 OK (HTML) ────────────  │
  │── TCP Close ────────────────► │

  │── TCP Handshake ──────────►  │   ← new connection for every resource
  │── GET /style.css ──────────► │
  │◄─ 200 OK (CSS) ─────────────  │
  │── TCP Close ────────────────► │

  │── TCP Handshake ──────────►  │   ← and again for every image, script...
  ...

  Loading a page with 20 resources = 20 TCP handshakes. ❌
```

---

### HTTP 1.1 — Persistent Connections

```
Client                          Server
  │── TCP Handshake ──────────►  │   ← ONE connection
  │── GET /index.html ─────────► │
  │◄─ 200 OK (HTML) ────────────  │
  │── GET /style.css ──────────► │   ← reuse same connection ✅
  │◄─ 200 OK (CSS) ─────────────  │
  │── GET /app.js ─────────────► │   ← and again ✅
  │◄─ 200 OK (JS) ──────────────  │

  Problem remaining: Head-of-line blocking.
  Request 2 must wait for Request 1's response to complete.
  One slow response blocks everything behind it. ❌
```

Additional HTTP 1.1 improvements:
- **Chunked transfer encoding:** Send a response in pieces before the full size is known (useful for streaming)
- **Range requests:** Download a specific byte range of a file (used for video seeking, resumable downloads)

---

### HTTP 2.0 — Multiplexing

```
HTTP 1.1 (sequential):            HTTP 2.0 (multiplexed):
──────────────────────            ────────────────────────
[Req 1 ────────────]              [Req 1 ──]
            [Req 2 ────────────]  [Req 2 ──────]    ← all in parallel
                        [Req 3]   [Req 3 ────]       over ONE connection
                                  ✅ No head-of-line blocking at HTTP layer
```

HTTP 2.0 improvements:
- **Multiplexing:** Multiple requests and responses in parallel on one connection, interleaved as binary frames
- **Binary framing:** Data transmitted as binary (not text), more efficient to parse
- **Header compression (HPACK):** Repeated headers (like `Authorization`) compressed across requests — significant saving on chatty APIs
- **Server push:** Server can proactively send resources the client hasn't requested yet

---

### HTTP 3.0 — QUIC over UDP

```
HTTP 1.1 / 2.0 (TCP):            HTTP 3.0 (QUIC / UDP):
──────────────────────            ──────────────────────
TCP head-of-line blocking:        Independent streams:
  Packet loss on stream 1           Packet loss on stream 1
  → ALL streams stall ❌            → only stream 1 waits
                                    → streams 2, 3, 4 continue ✅

Connection establishment:         0-RTT resumption:
  TCP: SYN → SYN-ACK → ACK         Reconnect with no handshake
  TLS: 2 more round trips           for known servers ✅
  Total: ~3 round trips ❌          Total: 0 round trips ✅
```

> HTTP/3 is particularly impactful on mobile networks where packet loss is common. When TCP drops a packet, the entire stream stalls. QUIC isolates the loss to a single stream.

---

## III. Anatomy of HTTP Messages

Every HTTP exchange consists of a **request** from the client and a **response** from the server. Both follow a defined structure.

---

### Request Structure

```
POST /api/orders HTTP/1.1                 ← Method + URL + Version
Host: api.example.com                     ┐
Content-Type: application/json            │ Headers
Authorization: Bearer eyJ...             │ (key: value pairs)
Content-Length: 82                        ┘
                                          ← blank line separates headers from body
{                                         ┐
  "items": [{"productId": 1, "qty": 2}], │ Body (optional)
  "shippingAddressId": 7                  │
}                                         ┘
```

---

### Response Structure

```
HTTP/1.1 201 Created                      ← Version + Status Code + Reason
Content-Type: application/json            ┐
Location: /api/orders/789                 │ Headers
Cache-Control: no-store                   │
X-Request-Id: a3f9c21d                    ┘
                                          ← blank line
{                                         ┐
  "orderId": 789,                         │ Body
  "status": "pending"                     │
}                                         ┘
```

---

### HTTP Headers — Metadata for Every Message

Headers are key-value pairs that annotate a request or response with context, instructions, and security policies.

```
Think of headers as labels on a parcel:
  ├── What's inside (Content-Type)
  ├── How big it is (Content-Length)
  ├── Who sent it (Authorization)
  ├── How long to keep it (Cache-Control)
  └── Security rules to enforce (Strict-Transport-Security)
```

**Header categories:**

```
Request Headers — client tells server about itself:
  User-Agent: Mozilla/5.0 (Chrome/120)   ← browser/client identity
  Authorization: Bearer eyJ...            ← auth credential
  Accept: application/json               ← preferred response format
  Accept-Language: en-US, hi;q=0.9      ← preferred language

Representation Headers — describe the body:
  Content-Type: application/json         ← body format
  Content-Length: 82                     ← body size in bytes
  Content-Encoding: gzip                 ← body is compressed

Security Headers — enforce browser security policies:
  Strict-Transport-Security             → force HTTPS for future requests
  Content-Security-Policy               → restrict script/resource sources
  X-Frame-Options: DENY                 → prevent clickjacking
  X-Content-Type-Options: nosniff       → prevent MIME sniffing
```

**Content Negotiation:**

```
Client specifies what it can accept:
  Accept: application/json, application/xml;q=0.8, */*;q=0.5
           ↑ prefer JSON    ↑ XML is OK (lower priority)   ↑ anything else last

Server responds in the best matching format
and confirms with:
  Content-Type: application/json
```

---

## IV. HTTP Methods and Idempotency

Each HTTP method carries a semantic contract about what the request does to server state.

---

### Idempotency — The Key Concept

```
Idempotent: Calling the same request N times has the same effect as calling it once.

  GET /api/orders/789    → always returns the same order (no side effects) ✅
  DELETE /api/orders/789 → first call deletes it; subsequent calls: "not found"
                           but the server state is the same (deleted) ✅
  PUT /api/orders/789    → replaces the order with the same data each time ✅

Non-idempotent:
  POST /api/orders       → each call creates a new order ❌
                           10 calls = 10 orders
```

---

### Method Reference

| Method | Purpose | Has Body | Idempotent | Safe |
|---|---|---|---|---|
| `GET` | Retrieve a resource | ❌ | ✅ | ✅ |
| `POST` | Create a new resource | ✅ | ❌ | ❌ |
| `PUT` | Replace a resource entirely | ✅ | ✅ | ❌ |
| `PATCH` | Partially update a resource | ✅ | ❌ | ❌ |
| `DELETE` | Remove a resource | ❌ | ✅ | ❌ |
| `HEAD` | Same as GET but no body returned | ❌ | ✅ | ✅ |
| `OPTIONS` | Ask server what methods are allowed | ❌ | ✅ | ✅ |

> **Safe** means the request has no side effects on server state. All safe requests are idempotent, but not all idempotent requests are safe.

---

### PUT vs. PATCH

```
Current resource: /api/users/42
{
  "name": "Alice",
  "email": "alice@example.com",
  "role": "admin"
}

PUT /api/users/42 — complete replacement:
  Body: { "name": "Alice Chen" }
  Result: { "name": "Alice Chen" }   ← email and role are gone ❌
  Use PUT when replacing the entire resource.

PATCH /api/users/42 — partial update:
  Body: { "name": "Alice Chen" }
  Result: { "name": "Alice Chen", "email": "alice@example.com", "role": "admin" } ✅
  Use PATCH when updating specific fields only.
```

---

## V. CORS and the OPTIONS Preflight

CORS (Cross-Origin Resource Sharing) is a **browser-enforced** security mechanism that controls how scripts on one domain can interact with resources on a different domain.

```
Same-origin (no CORS needed):
  Page:    https://app.example.com
  API call: https://app.example.com/api/data   ← same origin ✅

Cross-origin (CORS applies):
  Page:    https://myapp.com
  API call: https://api.example.com/data       ← different origin ⚠️
```

> CORS is enforced by the **browser** — not by the server, not by the network. A curl command or Postman request is never subject to CORS. Only browser-based JavaScript is.

---

### Simple Requests

A simple request uses GET, POST, or HEAD with only basic headers. The browser sends it directly and checks the response headers.

```
Browser                              Server (api.example.com)
   │                                        │
   │  GET /api/posts                        │
   │  Origin: https://myapp.com ───────────►│
   │                                        │  Check: is myapp.com allowed?
   │◄─ 200 OK ──────────────────────────────│
   │   Access-Control-Allow-Origin:         │
   │   https://myapp.com           ← ✅     │
   │   (or * for any origin)                │

If Access-Control-Allow-Origin is missing → browser blocks the response ❌
The request was sent — only the response is blocked.
```

---

### Preflight Requests (OPTIONS)

For "non-simple" requests (using PUT, DELETE, PATCH, or custom headers like `Authorization`), the browser sends an **OPTIONS request first** to ask for permission before sending the real request.

```
Browser                              Server (api.example.com)
   │                                        │
   │  OPTIONS /api/orders                   │
   │  Origin: https://myapp.com             │
   │  Access-Control-Request-Method: DELETE │
   │  Access-Control-Request-Headers:       │
   │    Authorization ──────────────────────►│
   │                                        │  Check permissions
   │◄── 204 No Content ─────────────────────│
   │    Access-Control-Allow-Origin:        │
   │      https://myapp.com                 │
   │    Access-Control-Allow-Methods:       │
   │      GET, POST, DELETE                 │
   │    Access-Control-Allow-Headers:       │
   │      Authorization                     │
   │    Access-Control-Max-Age: 3600 ← cache preflight for 1 hour
   │                                        │
   │  DELETE /api/orders/789                │
   │  Authorization: Bearer eyJ... ────────►│   ← actual request sent ✅
   │◄── 200 OK ──────────────────────────────│
```

> `Access-Control-Max-Age` tells the browser to cache the preflight result. Without it, a preflight OPTIONS request fires before **every** DELETE/PUT/PATCH — doubling your API call count.

---

## VI. Status Codes

Status codes tell the client exactly what happened to their request — success, failure, or something in between.

```
1xx — Informational   (processing, not done yet)
2xx — Success         (request fulfilled)
3xx — Redirection     (go somewhere else)
4xx — Client Error    (you did something wrong)
5xx — Server Error    (we did something wrong)
```

**The codes you'll actually use:**

| Code | Name | When to use |
|---|---|---|
| `200` | OK | Successful GET, PUT, PATCH |
| `201` | Created | Successful POST — resource created |
| `204` | No Content | Successful DELETE — no body to return |
| `301` | Moved Permanently | Resource URL changed forever (SEO-safe redirect) |
| `304` | Not Modified | Cached version is still valid — no body sent |
| `400` | Bad Request | Malformed request, validation failure |
| `401` | Unauthorized | No credentials provided or invalid token |
| `403` | Forbidden | Credentials valid but permission denied |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Resource state conflict (e.g., duplicate entry) |
| `422` | Unprocessable Entity | Valid JSON but semantically invalid |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Unhandled exception on the server |
| `502` | Bad Gateway | Upstream server returned an invalid response |
| `503` | Service Unavailable | Server is down or overloaded |

---

### 401 vs. 403 — The Common Confusion

```
401 Unauthorized:  "I don't know who you are."
  → No token provided, or token is expired/invalid
  → Client should re-authenticate and retry

403 Forbidden:     "I know exactly who you are — you just can't do this."
  → Valid token, but the user lacks the required role/permission
  → Re-authenticating will not help
```

---

## VII. HTTP Caching

Caching allows clients and intermediaries to reuse previous responses, reducing bandwidth and improving response times.

---

### Cache-Control

The primary header for controlling caching behavior:

```
Cache-Control: max-age=3600
  → Client may cache this response for 3600 seconds (1 hour)
  → During this time, no network request is made at all ✅

Cache-Control: no-cache
  → Cache the response, but always revalidate with the server before using it

Cache-Control: no-store
  → Never cache this response (use for sensitive data: banking, auth tokens)

Cache-Control: public
  → CDNs and shared caches may cache this (suitable for public assets)

Cache-Control: private
  → Only the end client may cache this (not CDNs — user-specific data)
```

---

### ETags — Conditional Requests

An ETag is a fingerprint (hash) of a resource's current version. It enables the client to ask: "has this changed since I last fetched it?"

```
First request:
  Client: GET /api/products/42
  Server: 200 OK
          ETag: "a3f9c21d"          ← hash of the resource
          { "name": "Widget", "price": 9.99 }

  Client stores: resource + ETag "a3f9c21d"

Second request (1 hour later):
  Client: GET /api/products/42
          If-None-Match: "a3f9c21d"  ← "only send body if ETag changed"

  Server: resource unchanged → same ETag
          304 Not Modified            ← no body sent ✅ (saves bandwidth)

  Server: resource updated → new ETag
          200 OK
          ETag: "b7c91a22"
          { "name": "Widget", "price": 12.99 }
```

**Real-world caching strategy:**

```
Static assets (JS, CSS, images):
  Cache-Control: public, max-age=31536000, immutable
  → Cache for 1 year. Use content-hash filenames (app.a3f9c21d.js)
  → Deploy new version = new filename = cache busted automatically ✅

API responses (user-specific):
  Cache-Control: private, max-age=60
  → Cache for 60 seconds in client only (not CDN)

Sensitive data (auth, payments):
  Cache-Control: no-store
  → Never cache under any circumstances
```

---

## VIII. Advanced Data Handling

---

### Compression

Large responses (JSON payloads, HTML pages) can be compressed before sending, dramatically reducing bytes transferred over the network.

```
Client advertises support:
  Accept-Encoding: gzip, br         ← supports Gzip and Brotli

Server compresses and confirms:
  Content-Encoding: gzip
  [compressed body bytes]

Typical compression ratios:
  Original JSON:    240 KB
  After Gzip:        48 KB  (80% smaller) ✅
  After Brotli:      38 KB  (84% smaller) ✅

Brotli (br) compresses better than Gzip but requires more CPU.
Gzip has broader compatibility. Use Brotli for static assets, Gzip as fallback.
```

---

### Multipart Requests — File Uploads

When uploading files, a multipart request sends multiple pieces of data (the file bytes + metadata) in a single request, each separated by a boundary delimiter.

```
POST /api/uploads HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWx

------WebKitFormBoundary7MA4YWx
Content-Disposition: form-data; name="description"

Profile photo upload
------WebKitFormBoundary7MA4YWx
Content-Disposition: form-data; name="file"; filename="photo.jpg"
Content-Type: image/jpeg

[raw binary bytes of the JPEG file]
------WebKitFormBoundary7MA4YWx--
```

> For large file uploads to cloud storage, prefer the **pre-signed URL** pattern: have your server generate a temporary S3 upload URL, then let the client upload directly to S3 — bypassing your server entirely.

---

### Chunked / Streamed Responses

Instead of waiting for the full response to be ready before sending, the server can stream it in chunks — ideal for large files, real-time logs, or AI-generated text.

```
HTTP/1.1 200 OK
Content-Type: text/event-stream     ← Server-Sent Events (SSE)
Transfer-Encoding: chunked          ← no Content-Length needed

4\r\n                               ← chunk size in hex
Wiki\r\n
6\r\n
pedia \r\n
E\r\n
in\r\n\r\nchunks.\r\n
0\r\n                               ← zero-length chunk = end of stream
\r\n
```

**Use cases:**

```
Chunked responses:
  ├── Streaming large file downloads
  ├── Real-time log tailing
  ├── AI chat responses (token by token streaming)
  └── Live dashboards via Server-Sent Events (SSE)
```

---

## IX. HTTPS, SSL, and TLS

HTTPS is HTTP transmitted over an **encrypted channel**. The encryption is provided by TLS (Transport Layer Security) — SSL is its outdated predecessor and should no longer be used.

```
HTTP  → plain text — anyone on the network can read it ❌
HTTPS → encrypted  — intercepted data is unreadable ✅
```

---

### What TLS Provides

```
Encryption:      Data is encrypted in transit.
                 A man-in-the-middle sees only ciphertext.

Authentication:  The server's certificate proves it is who it claims to be.
                 Prevents DNS spoofing attacks (connecting to a fake server).

Integrity:       Data cannot be tampered with in transit.
                 Any modification breaks the cryptographic signature.
```

---

### The TLS Handshake

```
Client                                   Server
  │                                         │
  │── ClientHello ────────────────────────►  │
  │   (TLS version, supported cipher suites) │
  │                                         │
  │◄─ ServerHello ──────────────────────────  │
  │   (chosen cipher suite)                  │
  │◄─ Certificate ──────────────────────────  │
  │   (server's public key + CA signature)   │
  │                                         │
  │  [Client verifies certificate with       │
  │   trusted Certificate Authority (CA)]    │
  │                                         │
  │── Key Exchange ───────────────────────►  │
  │   (generate shared session key)          │
  │                                         │
  │  [Both sides derive the same             │
  │   symmetric encryption key]              │
  │                                         │
  │◄─────── Encrypted HTTP traffic ─────────►│  ✅
```

---

### Certificates and Certificate Authorities (CAs)

```
A TLS certificate contains:
  ├── The server's domain name (api.example.com)
  ├── The server's public key
  ├── Expiry date
  └── Signature from a trusted CA (e.g., Let's Encrypt, DigiCert)

The CA signature is the "stamp of approval" that proves
the certificate is legitimate — not forged by an attacker.

Your browser ships with a list of trusted CAs.
If the certificate's CA is on that list → trusted ✅
If not → browser shows security warning ❌
```

> **Let's Encrypt** provides free, automated TLS certificates valid for 90 days. Tools like Certbot handle automatic renewal. There is no reason to run HTTP-only in production.

---

## X. Quick Reference Checklist

| Concern | HTTP Mechanism |
|---|---|
| Carry auth on every request | `Authorization` header (stateless by design) |
| Horizontal scaling | Statelessness — no server-side session needed |
| Describe request body format | `Content-Type: application/json` |
| Specify accepted response format | `Accept: application/json` |
| Enforce HTTPS permanently | `Strict-Transport-Security` header |
| Prevent XSS via resource loading | `Content-Security-Policy` header |
| Allow cross-origin browser calls | `Access-Control-Allow-Origin` header |
| Avoid preflight on every call | `Access-Control-Max-Age` to cache OPTIONS |
| Partial update (preserve other fields) | `PATCH` not `PUT` |
| Cache static assets aggressively | `Cache-Control: public, max-age=31536000, immutable` |
| Never cache sensitive responses | `Cache-Control: no-store` |
| Validate cache freshness | `ETag` + `If-None-Match` → `304 Not Modified` |
| Reduce response payload size | `Accept-Encoding: gzip, br` + `Content-Encoding: gzip` |
| Upload files | `multipart/form-data` or pre-signed S3 URL |
| Stream large responses | `Transfer-Encoding: chunked` + `text/event-stream` |
| Encrypt all traffic | TLS certificate (free via Let's Encrypt + Certbot) |