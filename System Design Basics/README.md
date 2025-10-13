# Spring Boot Important Concepts
# System Design Concepts & Interview Guide

## Core System Design Concepts with Real-World Analogies

### 1. Scalability

**Real-world analogy:** Think of a restaurant expanding its operations.
- **Vertical Scaling (Scale Up):** Upgrading your single chef with a faster stove and better tools - making one server more powerful
- **Horizontal Scaling (Scale Out):** Hiring more chefs to work in parallel - adding more servers to distribute the work

**Key Terms:**

**Load Balancing:** Like a restaurant host distributing customers evenly across tables so no waiter gets overwhelmed. The load balancer ensures no single server gets too much traffic.

**Sharding:** Splitting a massive phone book into volumes (A-M, N-Z) so people can search faster. Each shard contains a portion of the data.

**Replication:** Having backup copies of important documents in multiple safes across different buildings. If one building burns down, you still have copies.

---

### 2. Caching & Performance

**Real-world analogy:** Your brain's short-term memory vs. going to the library. Frequently accessed information stays in short-term memory (cache), while rarely used information requires a trip to the library (database).

**Key Terms:**

**CDN (Content Delivery Network):** Like having McDonald's franchises in every city instead of one central kitchen. Users get content from the nearest server, reducing latency.

**Cache Invalidation:** Like updating a cheat sheet when the original textbook changes. The hardest problem in computer science! Common strategies:
- **Write-through:** Update cache and database together (like editing both your notes and textbook simultaneously)
- **Write-back:** Update cache first, database later (like taking notes in class, organizing them later)

**Read-through/Write-through:** The application only talks to the cache, which talks to the database - like ordering food through a waiter who goes to the kitchen.

**TTL (Time To Live):** Like milk expiration dates - data expires after a certain time and must be refreshed.

**Indexes:** Like a book's index - helps you jump directly to the right page instead of reading every page.

---

### 3. Messaging & Events

**Real-world analogy:** Postal service vs. phone call

**Key Terms:**

**Message Queue (Kafka, RabbitMQ):** Like a post office. You drop off a letter (message), and it gets delivered eventually. The sender doesn't wait for delivery confirmation.
- **Asynchronous communication:** Send and forget
- **Decoupling:** Sender and receiver don't need to be available at the same time

**Event-Driven Architecture:** Like a notification bell in a restaurant kitchen. When an order is ready, the bell rings (event), and waiters respond. Multiple services can react to the same event.

**At-least-once delivery:** Like registered mail - the postal service keeps trying until confirmed delivered, but you might receive duplicates.

**Exactly-once delivery:** The holy grail - ensuring a message is delivered once and only once (very hard to achieve).

**Pub-Sub Model:** Like a newspaper subscription. Publishers (newspapers) send content, subscribers (readers) receive it. One publisher can have many subscribers.

---

### 4. Concurrency & Consistency

**Real-world analogy:** Multiple bank tellers accessing the same account

**Key Terms:**

**ACID Properties:**
- **Atomicity:** All or nothing - like buying a combo meal. You either get burger + fries + drink, or nothing.
- **Consistency:** Rules are never broken - your bank account can't go negative if that's the rule.
- **Isolation:** Transactions don't interfere - like having privacy curtains at voting booths.
- **Durability:** Once confirmed, it's permanent - like writing in permanent ink.

**Optimistic Locking:** Assume no conflicts will occur. Like editing a Google Doc - you edit freely, and only get warned if someone else changed the same section.

**Pessimistic Locking:** Assume conflicts will happen. Like checking out a library book - nobody else can edit it while you have it.

**Idempotency:** Doing the same action multiple times gives the same result. Like pressing an elevator button repeatedly - the elevator still comes once.

**Distributed Transactions:** Coordinating actions across multiple systems - like booking a flight + hotel + car together. Either all succeed or all fail.

**Two-Phase Commit (2PC):** Like a wedding. Phase 1: "Do you take this person?" (prepare). Phase 2: "I now pronounce you..." (commit).

---

### 5. Distributed Systems

**Real-world analogy:** Running a multi-national corporation

**Key Terms:**

**Microservices:** Like specialized shops in a mall. Each shop (service) does one thing well. The bookstore doesn't sell shoes.

**Service Discovery:** Like a mall directory. Services register themselves, and others can find them when needed.

**API Gateway:** Like a hotel concierge. Single point of contact that routes your requests to the right service and handles authentication.

**Circuit Breaker:** Like a home electrical breaker. If a service fails repeatedly, stop sending requests (open circuit) to prevent cascading failures. Try again after some time.

**Rate Limiting:** Like a nightclub bouncer controlling crowd entry. Prevents any single user from overwhelming the system.

**Saga Pattern:** Long-running distributed transactions. Like planning a vacation - book flight, hotel, car. If car rental fails, cancel hotel and flight (compensating transactions).

**CAP Theorem:** You can only pick 2 of 3:
- **Consistency:** Everyone sees the same data
- **Availability:** System always responds
- **Partition Tolerance:** Works even if network splits

Real-world: During a network split, choose between:
- Returning potentially stale data (Availability)
- Returning an error (Consistency)

---

### 6. API Design

**Real-world analogy:** Restaurant menu and ordering system

**Key Terms:**

**REST:** Like ordering from a menu using standard verbs:
- GET: "Show me item #5" (read)
- POST: "Add a new item" (create)
- PUT: "Replace item #5 with this" (update)
- DELETE: "Remove item #5" (delete)

**GraphQL:** Like a buffet where you specify exactly what you want on your plate, instead of fixed combo meals.

**WebSocket:** Like a phone call - continuous two-way communication. REST is like exchanging letters.

**gRPC:** Like using a specialized courier service with strict protocols (Protocol Buffers). Faster than REST but less human-readable.

**Pagination:** Like reading a book page by page instead of loading the entire book at once.

**Authentication:** Proving who you are (showing ID).

**Authorization:** Proving what you're allowed to do (membership card for VIP lounge).

**JWT (JSON Web Token):** Like an all-access wristband at a festival. Once you get it (login), you show it at each attraction without re-authenticating.

**OAuth:** Like using "Sign in with Google" - you authorize one service to access another on your behalf.

**CSRF (Cross-Site Request Forgery):** Like someone tricking you into signing a document while you're distracted.

---

### 7. Data Storage Patterns

**Real-world analogy:** Different types of filing systems

**Key Terms:**

**SQL vs NoSQL:**
- **SQL:** Like a filing cabinet with strict folders and labels. Everything has a place, structured and organized.
- **NoSQL:** Like a storage unit where you throw things in boxes. More flexible, less structured.

**Database Sharding:** Splitting data across multiple databases. Like having separate file cabinets for A-M and N-Z customers.

**Master-Slave Replication:** One main database (master) accepts writes, copies (slaves) handle reads. Like a professor (master) teaching, while TAs (slaves) answer student questions.

**Multi-Master Replication:** Multiple databases accept writes. Like having multiple cashiers at a bank who can all open accounts.

**Event Sourcing:** Storing every change as an event, not just the final state. Like keeping your entire bank statement history vs. just your current balance.

**CQRS (Command Query Responsibility Segregation):** Separate paths for reading and writing. Like having different lines at a post office - one for mailing letters, one for picking up packages.

---

### 8. System Reliability

**Real-world analogy:** Disaster preparedness

**Key Terms:**

**Availability:** Percentage of time system is operational. 99.9% = 43 minutes downtime per month.

**Fault Tolerance:** System continues working despite failures. Like a car's spare tire.

**Graceful Degradation:** System provides reduced functionality during issues. Like a website showing cached content when the database is down.

**Redundancy:** Having backups. Like having a spare key hidden outside your house.

**Disaster Recovery:** Plan for catastrophic failures. Like having insurance and off-site backups.

**Health Checks:** Regular checks to ensure services are alive. Like a doctor's checkup.

**Observability (Monitoring, Logging, Tracing):**
- **Monitoring:** Dashboard showing system health (like car dashboard)
- **Logging:** Detailed records of what happened (like a ship's log)
- **Tracing:** Following a request through the entire system (like tracking a package)

---

## Common Interview Scenario Questions

### Question 1: Design a URL Shortener (like bit.ly)

**Requirements Clarification:**
- How many URLs will be shortened per day?
- How long should shortened URLs be?
- Can users customize their short URLs?
- How long should we keep the mappings?
- Do we need analytics (click tracking)?

**Key Considerations:**
- Need to generate unique short codes (base62 encoding)
- Use caching for frequently accessed URLs
- Use database sharding for horizontal scaling
- Consider CDN for global availability
- Handle collisions in short URL generation

**Components:**
- API Gateway
- Application servers
- Database (SQL or NoSQL) for URL mappings
- Redis cache for popular URLs
- Analytics service for tracking clicks

---

### Question 2: Design a Distributed Wallet System

**Requirements Clarification:**
- What types of transactions? (wallet-to-wallet, external payments)
- Do transactions need to be atomic?
- How many concurrent transactions per second?
- Strong consistency or eventual consistency?
- Multi-currency support?
- Transaction history requirements?

**Key Considerations:**
- **ACID compliance** for financial transactions
- **Idempotency** - handling duplicate requests
- **Two-phase commit** or **Saga pattern** for distributed transactions
- **Pessimistic locking** to prevent race conditions
- Database sharding by user ID
- Separate read replicas for balance queries
- Message queue for async processing (notifications, analytics)

**Architecture:**
```
User Request → API Gateway → Transaction Service → Database
                ↓                     ↓
           Auth Service          Message Queue
                                      ↓
                            Notification/Analytics Services
```

**Consistency Model:**
- Strong consistency for balance updates (can't have negative balance)
- Eventual consistency for transaction history display
- Use distributed locks or database transactions with proper isolation levels

---

### Question 3: Design a Rate Limiter

**Requirements Clarification:**
- Rate limit per user? Per IP? Per API key?
- What's the time window? (requests per second/minute/hour)
- Distributed system or single server?
- What to do when limit exceeded? (reject, queue, throttle)

**Key Algorithms:**

1. **Token Bucket:** Like a bucket that fills with tokens over time. Each request consumes a token. If bucket is empty, request is denied.

2. **Leaky Bucket:** Like a bucket with a hole. Requests fill the bucket, which drains at a constant rate. If bucket overflows, requests are rejected.

3. **Fixed Window Counter:** Count requests in fixed time windows (e.g., every minute). Simple but can allow 2x traffic at window boundaries.

4. **Sliding Window Log:** Keep timestamps of all requests. Remove old timestamps and count remaining. Accurate but memory-intensive.

5. **Sliding Window Counter:** Hybrid approach - more accurate than fixed window, less memory than log.

**Implementation:**
- Use Redis for distributed rate limiting
- Store counters with expiration
- Consider race conditions in distributed systems
- Return meaningful error messages with retry-after headers

---

### Question 4: Design a Notification System

**Requirements Clarification:**
- Types of notifications? (push, email, SMS, in-app)
- Real-time or can be delayed?
- How many notifications per day?
- User preferences? (opt-out, frequency limits)
- Priority levels?

**Key Components:**
- Message Queue (Kafka/RabbitMQ) for reliability
- Separate workers for each notification type
- Template management service
- User preferences database
- Retry mechanism for failures
- Rate limiting to prevent spam

**Architecture:**
```
Event Source → Message Queue → Notification Service → Provider (FCM, SendGrid, Twilio)
                                      ↓
                              User Preferences DB
```

**Challenges:**
- Handling millions of notifications
- Deduplication (don't send same notification twice)
- Prioritization (urgent alerts vs. promotional)
- Tracking delivery status
- Handling provider failures (circuit breaker pattern)

---

### Question 5: Design an E-commerce Cart System

**Requirements Clarification:**
- Guest carts or only logged-in users?
- How long should cart items persist?
- Real-time inventory updates?
- Concurrent access handling (multiple tabs)?
- Reserved items during checkout?

**Key Considerations:**

**Cart Storage:**
- Logged-in users: Database with caching
- Guest users: Browser storage + server-side backup

**Inventory Management:**
- Check availability when adding to cart
- Soft reservation during checkout (with timeout)
- Handle race conditions (multiple users buying last item)

**Consistency:**
- Eventual consistency for cart display
- Strong consistency for checkout/payment
- Optimistic locking for inventory updates

**Scaling:**
- Cache frequently accessed products
- Shard user data by user ID
- Use CDN for product images
- Async processing for cart merging (guest → logged-in)

---

### Question 6: Design a Search Autocomplete System

**Requirements Clarification:**
- How many queries per second?
- How fast should suggestions appear?
- Personalized suggestions?
- Language support?
- Update frequency for trending searches?

**Key Components:**
- Trie data structure for efficient prefix matching
- Redis cache for popular queries
- Analytics pipeline for trending searches
- Partitioning by first letter or geography

**Architecture:**
```
User Input → API Gateway → Autocomplete Service → Trie (in-memory/Redis)
                                ↓
                         Analytics Service (Kafka)
                                ↓
                         Trending Calculator (batch job)
```

**Optimizations:**
- Limit suggestions (top 5-10)
- Cache popular prefixes
- Use CDN for static suggestion data
- Debounce requests (wait for user to stop typing)
- Pre-compute suggestions for common queries

---

### Question 7: Design a Video Streaming Platform (like Netflix)

**Requirements Clarification:**
- Number of concurrent users?
- Video quality options?
- Upload vs. streaming focus?
- Offline downloads?
- Content recommendation?

**Key Components:**

**Video Processing:**
- Transcode videos to multiple formats (360p, 720p, 1080p, 4K)
- Generate thumbnails
- Extract metadata
- Adaptive bitrate streaming (adjust quality based on connection)

**Storage:**
- Blob storage (S3) for video files
- CDN for global distribution
- Metadata database (SQL)

**Streaming:**
- Use protocols like HLS or DASH
- Chunk videos into small segments
- CDN edge servers for low latency
- Handle seek operations efficiently

**Architecture:**
```
Upload → Processing Pipeline → Storage (S3)
                                  ↓
User Request → CDN → Origin Server (if not in CDN)
```

**Challenges:**
- Bandwidth costs (CDN is expensive)
- Buffering prevention
- Copyright protection (DRM)
- Global availability
- Recommendation algorithm (separate service)

---

### Question 8: Design a Ride-Sharing System (like Uber)

**Requirements Clarification:**
- Real-time matching or queue-based?
- How to handle surge pricing?
- Driver-rider distance threshold?
- Multiple riders (carpooling)?
- Payment integration?

**Key Components:**

**Location Service:**
- Real-time driver location tracking (WebSockets)
- Geospatial indexing (QuadTree or Geohash)
- Efficient nearest driver queries

**Matching Service:**
- Find nearby available drivers
- Calculate ETA
- Handle concurrent booking attempts
- Optimize for distance + time

**Pricing Service:**
- Dynamic pricing based on demand/supply
- Distance + time calculation
- Surge multiplier

**Architecture:**
```
Rider App → API Gateway → Matching Service → Location Service
                              ↓                    ↓
Driver App ← Notification ← Trip Service ← QuadTree DB
```

**Challenges:**
- Race conditions (multiple riders booking same driver)
- Handling high write load (location updates)
- Real-time updates to both rider and driver
- Dealing with GPS inaccuracies
- Fault tolerance (what if driver app crashes?)

---

### Question 9: Design a Distributed Cache System

**Requirements Clarification:**
- Size of cache (memory constraints)?
- Read vs. write ratio?
- Eviction policy?
- Consistency requirements?
- Single datacenter or distributed?

**Key Concepts:**

**Eviction Policies:**
- **LRU (Least Recently Used):** Remove items not accessed recently
- **LFU (Least Frequently Used):** Remove items accessed least often
- **FIFO:** First in, first out
- **TTL:** Time-based expiration

**Distribution Strategies:**
- **Consistent Hashing:** Distribute keys across nodes, minimize reallocation when nodes added/removed
- **Replication:** Multiple copies for high availability
- **Sharding:** Partition data across nodes

**Architecture:**
```
Client → Cache Proxy → Cache Nodes (Consistent Hash Ring)
                           ↓
                      Database (on miss)
```

**Challenges:**
- Cache stampede (many requests for expired key simultaneously)
- Cache penetration (querying non-existent keys)
- Cache avalanche (many keys expire simultaneously)
- Handling node failures
- Keeping cache and database in sync

---

### Question 10: Design a Social Media News Feed

**Requirements Clarification:**
- Real-time updates or eventual consistency?
- How many followers can a user have?
- Ranking algorithm (chronological, algorithmic)?
- Types of content (text, images, videos)?
- Pagination strategy?

**Key Approaches:**

**Fan-Out on Write (Push):**
- When user posts, write to all followers' feeds immediately
- Fast reads, slow writes
- Good for users with few followers
- Pre-computed feeds

**Fan-Out on Read (Pull):**
- When user opens feed, fetch posts from followed users
- Fast writes, slow reads
- Good for users with many followers
- Fresh content

**Hybrid Approach:**
- Use push for normal users
- Use pull for celebrities (millions of followers)
- Cache recently accessed feeds

**Architecture:**
```
Post Creation → Post Service → Message Queue
                                    ↓
                            Fan-Out Service
                                    ↓
                  Feed Storage (User ID → Posts)
                                    ↓
User Request → Feed Service → Ranking Algorithm → Response
```

**Challenges:**
- Handling celebrities with millions of followers
- Real-time updates (WebSockets for live feed)
- Ranking posts (engagement, recency, relevance)
- Pagination (cursor-based vs. offset)
- Filtering inappropriate content

---

## Key Trade-offs to Discuss in Interviews

### 1. Consistency vs. Availability
- Financial systems: Choose consistency (can't have wrong balance)
- Social media: Choose availability (okay if feed is slightly stale)

### 2. Latency vs. Throughput
- Gaming: Low latency (fast response)
- Batch processing: High throughput (process many items)

### 3. Read Optimization vs. Write Optimization
- Blogs: Optimize reads (more people read than write)
- Logging: Optimize writes (lots of logs generated)

### 4. Complexity vs. Performance
- Simple solution that works vs. over-engineered optimization

### 5. Cost vs. Performance
- More servers = better performance but higher cost
- Caching reduces database load but adds complexity

---

## Interview Tips

1. **Always Ask Clarifying Questions:** Never jump straight to design. Understand requirements, scale, and constraints.

2. **Start High-Level:** Draw boxes for major components, then drill into specifics.

3. **Think About Scale:**
    - 100 users? Simple server + database
    - 1 million users? Need load balancing, caching
    - 100 million users? Need sharding, CDN, multiple data centers

4. **Discuss Trade-offs:** No perfect solution. Explain why you chose one approach over another.

5. **Consider Failure Scenarios:** What happens if a server crashes? Database goes down? Network partitions?

6. **Use Numbers:** Back-of-the-envelope calculations
    - 1M requests/day = ~12 requests/second
    - 1TB of data with 1KB per record = 1 billion records

7. **Think End-to-End:** From user request to database and back. Don't forget authentication, monitoring, logging.

8. **Be Ready to Deep Dive:** Interviewer might ask about specific components in detail.

---

## Common Mistakes to Avoid

1. **Not asking questions** - jumping straight to solution
2. **Over-engineering** - adding unnecessary complexity
3. **Ignoring constraints** - not considering scale, budget, timeline
4. **Forgetting non-functional requirements** - security, monitoring, logging
5. **Not discussing alternatives** - showing only one approach
6. **Poor time management** - spending too much time on one component
7. **Unclear communication** - not explaining your thought process
8. **Ignoring the interviewer's hints** - they're trying to guide you

---