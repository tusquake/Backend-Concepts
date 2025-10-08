# Circuit Breaker Pattern Demo with Resilience4j

A comprehensive Spring Boot project to learn the Circuit Breaker pattern, state machine transitions, and sliding window algorithms.

## CIRCUIT BREAKER vs RATE LIMITER - Understanding the Difference

### CIRCUIT BREAKER - "Protect Yourself from a Failing Service"

**Purpose:**
A Circuit Breaker protects your system from cascading failures caused by a downstream dependency (like SAP OData, Payment API, or Database).
Think of it like an electrical circuit breaker - when too many faults happen, it breaks the circuit to stop further damage.

**Real-World Example:**
Your Spring Boot app calls SAP OData service.
- SAP OData is temporarily down or very slow.
- Your service keeps sending requests - they timeout, threads get blocked, CPU spikes, and your app may also crash.

**Solution: You use a Circuit Breaker.**

How it works:
1. Monitor recent SAP call failures (say, last 10 calls).
2. If more than 50% fail → Circuit opens (no more calls to SAP).
3. Your app instantly returns a fallback response.
4. After 30s, circuit half-opens → tests 1-2 calls to see if SAP recovered.
5. If success → circuit closes again.

Example outcome:
"SAP service unavailable, please try later."
**Result:** Protects your app from depending too much on a broken service.

---

### RATE LIMITER - "Protect Others (or Yourself) from Overuse"

**Purpose:**
A Rate Limiter controls the frequency of requests - used to:
- Prevent overloading your own service.
- Prevent abuse from users or clients.
- Enforce API quotas or fair usage.

**Real-World Example:**
Imagine your service exposes a `/customer/details` API endpoint.
- One client (or script) starts hitting it 1000 times per second.
- Your DB or backend starts choking.

**Solution: You use a Rate Limiter.**

How it works:
1. You allow max 100 requests per user per minute.
2. If a client exceeds that → respond with `429 Too Many Requests`.

Example outcome:
"You've exceeded your request limit. Try again after 1 minute."
**Result:** Protects your app (and other users) from overload due to excessive traffic.

---

### Key Differences Table

| Feature | Circuit Breaker | Rate Limiter |
|---------|----------------|--------------|
| **Goal** | Protect yourself from failing downstream services | Protect your service (or downstream) from too many requests |
| **Trigger** | Repeated failures/timeouts in external service | High request volume beyond threshold |
| **Response** | Open circuit → stop calling → use fallback | Reject or delay excess requests |
| **Error Type** | Functional or system failure | Traffic volume or abuse |
| **Example** | SAP OData API is down | A client floods `/api/orders` |
| **Handled by** | Resilience4j CircuitBreaker | Resilience4j RateLimiter or Bucket4j |
| **Fallback Example** | "Service temporarily unavailable" | "Too many requests, try later" |

---

### Real Enterprise Use Case

**Scenario:**
Your Spring Boot microservice calls:
- SAP OData (for product info)
- Payment Gateway API
- Database
- Exposes REST endpoints to multiple clients

You might use both patterns:
1. **Circuit Breaker** for SAP & Payment APIs → avoid calling when they're failing.
2. **Rate Limiter** on your exposed endpoints → avoid overloading your service or downstreams.

**Combined Protection:**
```java
// Circuit Breaker - Protects FROM failing SAP service
@CircuitBreaker(name = "sapOData", fallbackMethod = "getCachedProducts")
public List<Product> getProductsFromSAP() {
    return sapClient.fetchProducts(); // May fail
}

// Rate Limiter - Protects your service FROM too many client requests
@RateLimiter(name = "customerAPI")
@GetMapping("/api/customer/details")
public CustomerDetails getCustomerDetails(@PathParam String customerId) {
    return customerService.getDetails(customerId);
}
```

**Result:**
- If SAP fails → Circuit breaker prevents cascading failure
- If client sends 1000 req/sec → Rate limiter protects your backend

---

## What is Circuit Breaker Pattern? (Simple Explanation)

### Real-World Analogy: Your Home's Electrical Circuit Breaker

Imagine your home's electrical circuit breaker:

**Normal Operation (CLOSED):**
- Electricity flows normally through wires
- All appliances work fine
- Circuit breaker allows current to pass through

**Electrical Problem (OPEN):**
- Too many appliances plugged in → circuit overloads
- Circuit breaker **trips** (opens) to prevent fire
- Electricity stops flowing immediately
- Protects your home from damage

**Testing After Fix (HALF_OPEN):**
- You unplug some appliances
- Flip the breaker switch partially to test
- If current is normal → breaker stays closed
- If still overloaded → breaker trips again

### In Software Terms

Your Spring Boot app calls an external service (like a payment gateway, database, or API):

**When Service is Healthy (CLOSED):**
```
Your App → Calls → External Service (Works)
           ← Response ← (Success)
```

**When Service Keeps Failing (OPEN):**
```
Your App → STOP! → External Service
           ← Fallback Response ← "Service unavailable, try later"
```
Circuit breaker **stops** calling the failing service to:
- Prevent wasting time on doomed requests
- Stop cascading failures (your app doesn't crash too)
- Give the external service time to recover

**Testing Recovery (HALF_OPEN):**
```
Your App → Test Call → External Service
           ← If success: Resume normal operation
           ← If fails: Back to OPEN state
```

## Why Do We Need This?

### Without Circuit Breaker (BAD)

```
Customer tries to checkout → 
Your app calls payment service (down) → Waits 30 seconds → Timeout →
Tries again → Waits 30 seconds → Timeout →
App crashes → All customers affected
```

### With Circuit Breaker (GOOD)

```
Customer tries to checkout → 
Circuit detects payment service is down → 
Immediately returns: "Payment temporarily unavailable, saved your cart" →
Customer sees message in 50ms (not 30 seconds!) →
App stays healthy → Other features work fine
```

## What You'll Learn

1. **Circuit Breaker State Machine**: CLOSED → OPEN → HALF_OPEN transitions
2. **Sliding Window Statistics**: How failure rates are calculated
3. **Fallback Mechanisms**: Graceful degradation
4. **Real-time Monitoring**: Metrics and health checks

## Real-World Example: Food Delivery App

Let's say you're building a food delivery app like Uber Eats:

### Scenario: Restaurant Service Goes Down

**Without Circuit Breaker:**
```
1. Customer orders food
2. App calls restaurant service → TIMEOUT (30s wait)
3. Retry #1 → TIMEOUT (30s wait)
4. Retry #2 → TIMEOUT (30s wait)
5. Customer waited 90 seconds → Gets error
6. Thousands of customers → Thousands of timeout threads
7. YOUR APP CRASHES!
```

**With Circuit Breaker:**
```
1. First 5 customers → Restaurant service times out
2. Circuit Breaker: "Restaurant service is failing!"
3. Circuit OPENS → Stops calling restaurant service
4. Next customers get INSTANT response: 
   "Restaurant menu temporarily unavailable. 
    Browse other restaurants while we fix this!"
5. Your app stays fast and healthy
6. After 15 seconds, circuit tests if restaurant service recovered
7. If recovered → Resume normal operation
```

## The Three States Explained with Traffic Light Analogy

### GREEN LIGHT - CLOSED State
**"Everything is working, go ahead!"**

- Like a green traffic light - cars flow normally
- Your app calls external service freely
- Tracks success/failure rate in background
- If too many failures (50%) → Switches to OPEN

**Example:**
```
Calls: SUCCESS SUCCESS SUCCESS FAIL SUCCESS SUCCESS SUCCESS SUCCESS FAIL SUCCESS
Success rate: 80% → Circuit stays CLOSED (healthy)
```

### RED LIGHT - OPEN State
**"Stop! Don't waste time on this broken service!"**

- Like a red traffic light - all cars must stop
- Your app STOPS calling the external service
- Returns fallback response immediately (0ms, not 30 seconds!)
- Waits 15 seconds, then tries HALF_OPEN state
- Prevents cascading failures

**Example:**
```
Calls: SUCCESS FAIL FAIL FAIL FAIL FAIL FAIL FAIL FAIL FAIL
Success rate: 10% → Circuit OPENS (unhealthy)
Next 100 requests → Instant fallback, no actual calls made
```

### YELLOW LIGHT - HALF_OPEN State
**"Proceed with caution, testing if it's safe"**

- Like a yellow traffic light - careful, testing the way
- Allows 3 test calls to check if service recovered
- If all 3 succeed → Go back to CLOSED (green light)
- If any fail → Go back to OPEN (red light)

**Example:**
```
Test calls: SUCCESS SUCCESS SUCCESS → Circuit CLOSES (recovered)
OR
Test calls: SUCCESS FAIL → Circuit OPENS again (still broken)
```

## Project Structure

```
src/main/java/com/example/circuitbreaker/
├── CircuitBreakerDemoApplication.java  # Main application
├── controller/
│   └── CircuitBreakerController.java   # REST endpoints
└── service/
    └── ExternalService.java             # Service with circuit breaker

src/main/resources/
└── application.yml                      # Circuit breaker configuration
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

Application starts at: `http://localhost:8080`

## Learning Scenarios

### Scenario 1: Understanding Circuit Breaker States

**CLOSED State (Normal Operation)**

1. Call the service multiple times:
```bash
# Make successful calls
curl http://localhost:8080/api/call?data=request1
curl http://localhost:8080/api/call?data=request2
```

2. Check circuit breaker state:
```bash
curl http://localhost:8080/api/circuit-breaker/state
```

You'll see:
- `state: CLOSED` - Circuit is allowing requests
- Low failure rate
- Successful calls count increasing

---

**OPEN State (Circuit Trips)**

1. Enable failure simulation:
```bash
curl -X POST http://localhost:8080/api/simulate/enable-failures
```

2. Make multiple calls (at least 5 to reach minimum threshold):
```bash
# Make 10 calls quickly
for i in {1..10}; do curl http://localhost:8080/api/call?data=test$i; done
```

3. Check state again:
```bash
curl http://localhost:8080/api/circuit-breaker/state
```

**What happens:**
- After 5 failures out of 10 calls (50% threshold), circuit **OPENS**
- `state: OPEN` - All requests now fail fast
- Fallback method returns: "Service temporarily unavailable"
- No actual service calls are made (protecting the failing service)

4. Try calling again:
```bash
curl http://localhost:8080/api/call
```
Response: Fallback message immediately without trying the service

---

**HALF_OPEN State (Testing Recovery)**

1. Wait 15 seconds (configured `waitDurationInOpenState`)

2. Make a call:
```bash
curl http://localhost:8080/api/call
```

**What happens:**
- Circuit automatically transitions to `HALF_OPEN`
- Allows 3 test calls (`permittedNumberOfCallsInHalfOpenState`)
- If these succeed → transitions to CLOSED
- If these fail → returns to OPEN

3. Disable failures to allow recovery:
```bash
curl -X POST http://localhost:8080/api/simulate/disable-failures
```

4. Make 3 successful calls:
```bash
curl http://localhost:8080/api/call?data=recovery1
curl http://localhost:8080/api/call?data=recovery2
curl http://localhost:8080/api/call?data=recovery3
```

5. Check state - should be back to CLOSED:
```bash
curl http://localhost:8080/api/circuit-breaker/state
```

---

### Scenario 2: Sliding Window Algorithm

The circuit breaker uses a **count-based sliding window** of 10 calls:

1. Reset everything:
```bash
curl -X POST http://localhost:8080/api/circuit-breaker/reset
```

2. Make calls and watch the sliding window:
```bash
# First 4 calls - circuit stays CLOSED (below minimum 5 calls)
curl http://localhost:8080/api/call  # Success
curl http://localhost:8080/api/call  # Success
curl http://localhost:8080/api/call  # Success
curl http://localhost:8080/api/call  # Success

curl http://localhost:8080/api/circuit-breaker/metrics
```

Notice: Circuit doesn't calculate failure rate yet (need minimum 5 calls)

3. Continue to reach threshold:
```bash
# Enable failures
curl -X POST http://localhost:8080/api/simulate/enable-failures

# Make 6 more calls
for i in {5..10}; do 
  curl http://localhost:8080/api/call
  sleep 1
done
```

4. Watch the metrics:
```bash
curl http://localhost:8080/api/circuit-breaker/metrics
```

**Understanding the output:**
- `numberOfBufferedCalls: 10` - Sliding window is full
- `numberOfSuccessfulCalls: 4` (initial successes)
- `numberOfFailedCalls: 6` (simulated failures)
- `failureRate: 60%` - Exceeds 50% threshold → Circuit OPENS

---

### Scenario 3: Slow Call Detection

1. Reset and enable slow responses:
```bash
curl -X POST http://localhost:8080/api/circuit-breaker/reset
curl -X POST http://localhost:8080/api/simulate/enable-slow-responses
```

2. Make calls (each takes 3 seconds):
```bash
for i in {1..10}; do curl http://localhost:8080/api/call; done
```

3. Check metrics:
```bash
curl http://localhost:8080/api/circuit-breaker/metrics
```

**What you'll see:**
- `numberOfSlowCalls: 10`
- `slowCallRate: 100%` - Exceeds 50% threshold
- Circuit may OPEN based on slow calls (protecting against slow services)

---

### Scenario 4: Manual State Transitions (For Learning)

Force state transitions to understand the state machine:

```bash
# Force OPEN
curl -X POST http://localhost:8080/api/circuit-breaker/transition/OPEN

# Try calling - should get fallback immediately
curl http://localhost:8080/api/call

# Force HALF_OPEN
curl -X POST http://localhost:8080/api/circuit-breaker/transition/HALF_OPEN

# Force CLOSED
curl -X POST http://localhost:8080/api/circuit-breaker/transition/CLOSED
```

---

## Monitoring and Metrics

### Real-time Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Circuit Breaker Events
```bash
curl http://localhost:8080/actuator/circuitbreakerevents
```

Shows all state transitions, failures, successes with timestamps.

### All Circuit Breakers Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

---

## How Sliding Window Works (Simple Explanation)

### Analogy: Teacher Grading Last 10 Homework Assignments

Imagine a teacher who checks your last 10 homework assignments to decide if you're struggling:

**Count-Based Sliding Window:**
```
Your last 10 assignments: PASS PASS PASS FAIL FAIL FAIL FAIL FAIL PASS PASS
                          1    2    3    4    5    6    7    8    9    10

Failed: 5 out of 10 (50%)
Teacher thinks: "You're failing too much, need extra help!"
```

When you submit assignment #11:
```
Old: PASS PASS PASS FAIL FAIL FAIL FAIL FAIL PASS PASS
     ↑ drops out
New:      PASS PASS FAIL FAIL FAIL FAIL FAIL PASS PASS PASS
                                                        ↑ new one

Teacher always looks at recent 10 assignments (window "slides")
```

### In Circuit Breaker Terms

**Count-Based Sliding Window (Default in our project):**
- Tracks last **10 service calls** (like 10 homework assignments)
- Calculates failure rate from these 10 calls
- As new calls come in, oldest calls are removed (window slides)
- If failure rate >= 50% → Circuit OPENS

**Real Example:**
```
Call history: S S S F F F F F S S
              ↑ oldest       ↑ newest
              
Total: 10 calls in window
Failed: 6 calls (F)
Success: 4 calls (S)
Failure Rate: 6/10 = 60% 

60% > 50% threshold → Circuit OPENS!
```

**Next call comes in:**
```
Old: S S S F F F F F S S
     ↑ removed
     
New:   S S F F F F F S S S (new success)
                         ↑ added
                         
Failed: 5 calls
Success: 5 calls
Failure Rate: 5/10 = 50%

50% = 50% threshold → Circuit stays OPEN
```

### Time-Based Sliding Window (Alternative)

**Analogy: Speed Limit Camera**

Instead of counting last 10 cars, camera checks:
- "How many cars speeded in the **last 60 seconds**?"

**In Circuit Breaker:**
```yaml
slidingWindowType: TIME_BASED
slidingWindowSize: 60  # 60 seconds window
```

- Tracks all calls in last **60 seconds** (not last 10 calls)
- If in last 60 seconds, 50% failed → Circuit OPENS
- Good for high-traffic services with many calls per second

**Example:**
```
Last 60 seconds:
00:00 - SUCCESS SUCCESS SUCCESS SUCCESS SUCCESS
00:30 - FAIL FAIL FAIL FAIL FAIL FAIL FAIL
00:59 - SUCCESS SUCCESS

Total calls in 60-second window: 13
Failed: 7
Success: 6
Failure Rate: 7/13 = 54% → Circuit OPENS!
```

### Why Sliding Window is Smart

**Problem without sliding window:**
- Service fails once in the morning → Circuit opens
- Service works fine all day → Circuit still thinks it's broken!

**Solution with sliding window:**
- Only considers **recent** calls (last 10 or last 60 seconds)
- Old failures don't affect current decisions
- Adapts quickly to service recovery
- Fair assessment of current service health

**Visual Example:**
```
Time:     [old calls]  →  [sliding window: last 10 calls]  →  [future]
          FAIL FAIL FAIL    SUCCESS SUCCESS SUCCESS SUCCESS
          ignored!          only these matter!

Circuit sees: 100% success → Stays CLOSED (healthy)
(Ignores old failures that are outside the window)
```

---

## Key Configuration Parameters

| Parameter | Value | Meaning |
|-----------|-------|---------|
| `slidingWindowSize` | 10 | Number of calls to track |
| `minimumNumberOfCalls` | 5 | Min calls before calculating rate |
| `failureRateThreshold` | 50% | Failure % to open circuit |
| `waitDurationInOpenState` | 15s | Time before trying HALF_OPEN |
| `permittedCallsInHalfOpen` | 3 | Test calls in HALF_OPEN state |
| `slowCallDurationThreshold` | 2000ms | Define "slow" call |
| `slowCallRateThreshold` | 50% | Slow call % to open circuit |

---

## Understanding the Output

When you call `/api/circuit-breaker/state`, you get:

```json
{
  "state": "CLOSED",
  "metrics": {
    "failureRate": "30.00%",
    "numberOfSuccessfulCalls": 7,
    "numberOfFailedCalls": 3,
    "numberOfBufferedCalls": 10
  },
  "config": {
    "slidingWindowSize": 10,
    "failureRateThreshold": "50.0%",
    "minimumNumberOfCalls": 5
  }
}
```

**Reading the metrics:**
- If `failureRate` < threshold → Circuit stays CLOSED
- If `failureRate` >= threshold AND `numberOfBufferedCalls` >= `minimumNumberOfCalls` → Circuit OPENS

---

## Common Scenarios in Production (Real-World Examples)

### Scenario 1: Database Connection Pool Exhausted

**The Problem:**
```
Your app has 10 database connections available.
Sudden traffic spike: 1000 customers at once!
First 10 requests: grab all connections
Next 990 requests: waiting... waiting... timeout after 30 seconds
Your app becomes extremely slow → Customers leave → Revenue lost
```

**Circuit Breaker Solution:**
```
1. First 10 requests → Some timeout (slow database)
2. Circuit Breaker detects: "50% of calls timing out!"
3. Circuit OPENS → Stops sending requests to database
4. Next 990 requests → Get instant fallback:
   "Showing cached data from 5 minutes ago"
5. Customers see slightly old data but FAST (50ms vs 30 seconds)
6. Database pressure reduces → Recovers
7. Circuit tests recovery → HALF_OPEN → CLOSED
8. Normal operation resumes
```

**Real Example: E-commerce Product Catalog**
```java
@CircuitBreaker(name = "productDB", fallbackMethod = "getCachedProducts")
public List<Product> getProducts() {
    return database.queryProducts(); // May fail
}

// Fallback: Return cached data instead of failing
private List<Product> getCachedProducts(Throwable t) {
    return cache.getProducts(); // 5-minute old data, but fast!
}
```

**Result:** Customers can browse products even when database is slow!

---

### Scenario 2: Payment Gateway Down

**The Problem:**
```
Customer: "I want to buy this $100 item"
Your app: Calls payment gateway → No response → Waits 30 seconds
Customer: Frustrated, closes tab → Sale lost
```

**Circuit Breaker Solution:**
```
1. First few customers → Payment gateway times out
2. Circuit OPENS
3. Next customers get instant response:
   "Payment service temporarily unavailable.
    We've saved your cart.
    You'll get an email when we can process payment."
4. Customer's cart is saved to database
5. When payment service recovers → Send email to customers
6. Customer completes purchase later
```

**Code Example:**
```java
@CircuitBreaker(name = "paymentGateway", fallbackMethod = "saveOrderForLater")
public PaymentResponse processPayment(Order order) {
    return paymentGateway.charge(order); // External API
}

private PaymentResponse saveOrderForLater(Order order, Throwable t) {
    // Save order to process later
    orderRepository.saveAsPending(order);
    emailService.notifyCustomer(order, "We'll process your payment soon");
    return PaymentResponse.pending("Order saved, we'll notify you");
}
```

---

### Scenario 3: Third-Party Weather API (Slow Responses)

**The Problem:**
```
Travel app needs weather data for 100 cities
Weather API is slow: 5 seconds per request
Total time: 100 × 5 = 500 seconds (8+ minutes!)
Users leave your app → Bad experience
```

**Circuit Breaker Solution:**
```
1. First few requests → Weather API responds in 5 seconds (slow!)
2. Circuit Breaker detects: "90% of calls are SLOW"
3. Circuit OPENS
4. Fallback: Show yesterday's weather (from cache)
5. Users see data in 50ms instead of 5 seconds
6. Weather API recovers → Circuit tests → Resumes
```

**Code Example:**
```java
@CircuitBreaker(
        name = "weatherAPI",
        fallbackMethod = "getLastKnownWeather"
)
public Weather getCurrentWeather(String city) {
    return weatherAPI.fetch(city); // Slow external API
}

private Weather getLastKnownWeather(String city, Throwable t) {
    Weather cached = cache.get(city);
    cached.setNote("Last updated: 2 hours ago");
    return cached; // Old data, but instant!
}
```

---

### Scenario 4: Microservice Chain Failure Prevention

**The Problem: Cascading Failures**
```
                  ┌─────────┐
User Request →    │Service A│
                  └────┬────┘
                       ↓
                  ┌────┴────┐
                  │Service B│ ← CRASHES!
                  └────┬────┘
                       ↓
                  ┌────┴────┐
                  │Service C│
                  └─────────┘

Service B crashes →
Service A keeps calling B → Times out →
Service A also crashes →
Entire system down!
```

**Circuit Breaker Solution:**
```
                  ┌─────────┐
User Request →    │Service A│ ← Has Circuit Breaker
                  └────┬────┘
                       ↓ (Circuit OPENS)
                  ┌────┴────┐
                  │Service B│ ← CRASHES!
                  └─────────┘
                       
Service A detects B is down →
Stops calling Service B →
Returns fallback data →
Service A stays healthy
System partially works (better than total failure!)
```

---

### Scenario 5: Black Friday / High Traffic Events

**The Problem:**
```
Normal day: 1,000 requests/second → All fine
Black Friday: 100,000 requests/second →
Your servers can't handle it →
Everything slows down →
Complete system failure
```

**Circuit Breaker Solution:**
```
1. Identify non-critical services:
   - Recommendations engine
   - User reviews loading
   - Related products

2. Circuit breaker protects these:
   - If they slow down → Circuit OPENS
   - Fallback: Don't show recommendations (not critical!)
   - Critical services (checkout, payment) get more resources

3. Result:
   - Customers can still BUY (critical)
   - They don't see recommendations (nice-to-have)
   - System stays up
```

**Code Example:**
```java
@CircuitBreaker(name = "recommendations", fallbackMethod = "skipRecommendations")
public List<Product> getRecommendations(User user) {
    return mlService.recommend(user); // CPU-intensive
}

private List<Product> skipRecommendations(User user, Throwable t) {
    return List.of(); // Return empty list, checkout still works!
}
```

---

## Key Takeaway

**Circuit Breaker = Fail Fast + Graceful Degradation**

Instead of:
```
Wait 30 seconds → Crash → All features down
```

You get:
```
Detect failure in 50ms → Return fallback → Core features work
```

**Better to have:**
- A shopping site where you can't see reviews (minor issue)
- Than a shopping site that's completely down (major issue)

---

## Testing Checklist

- [ ] Observe CLOSED state with successful calls
- [ ] Trigger circuit to OPEN by simulating failures
- [ ] Watch automatic transition to HALF_OPEN
- [ ] See recovery back to CLOSED state
- [ ] Test slow call detection
- [ ] Monitor sliding window metrics
- [ ] Verify fallback mechanism
- [ ] Check actuator endpoints