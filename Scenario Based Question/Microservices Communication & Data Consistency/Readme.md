# Microservices Communication & Data Consistency Guide

## Table of Contents
- [Overview](#overview)
- [Q1: Handling Service Failures in Request Chain](#q1-handling-service-failures-in-request-chain)
  - [The Problem](#the-problem)
  - [Solution 1: Circuit Breaker Pattern](#solution-1-circuit-breaker-pattern)
  - [Solution 2: Retry Mechanism](#solution-2-retry-mechanism)
  - [Solution 3: Timeout Configuration](#solution-3-timeout-configuration)
  - [Solution 4: Fallback Mechanism](#solution-4-fallback-mechanism)
  - [Solution 5: Asynchronous Processing](#solution-5-asynchronous-processing)
- [Q2: Distributed Transaction Management](#q2-distributed-transaction-management)
  - [The Problem](#the-problem-1)
  - [Solution 1: Saga Pattern (Choreography)](#solution-1-saga-pattern-choreography)
  - [Solution 2: Saga Pattern (Orchestration)](#solution-2-saga-pattern-orchestration)
  - [Solution 3: Two-Phase Commit (2PC)](#solution-3-two-phase-commit-2pc)
  - [Solution 4: Outbox Pattern](#solution-4-outbox-pattern)
- [Complete Implementation Examples](#complete-implementation-examples)
- [Comparison of Approaches](#comparison-of-approaches)
- [Best Practices](#best-practices)
- [Common Interview Questions](#common-interview-questions)

---

## Overview

This guide covers critical microservices patterns for handling:
1. **Service failures** in synchronous communication chains
2. **Data consistency** across distributed services
3. **Transaction management** in distributed systems

These patterns are essential for building resilient, production-ready microservices architectures.

---

## Q1: Handling Service Failures in Request Chain

### The Problem

**Scenario**: You have multiple microservices communicating synchronously via REST. A service in the middle of a request chain fails.

**Example**:
```
User → Order Service → Payment Service → Inventory Service
                            ↓ (FAILS HERE)
                          500 Error
```

**Questions to Address**:
- How do you handle failure recovery?
- How do you prevent cascading failures?
- How do you ensure data consistency?

---

### Solution 1: Circuit Breaker Pattern

**What it does**: Prevents repeated calls to a failing service, allowing it time to recover.

**Real-World Analogy**: 
Like an electrical circuit breaker in your home - when there's an overload, it trips to prevent damage. Once the problem is fixed, you can reset it.

**How it works**:
1. **Closed State** (Normal): Requests pass through
2. **Open State** (Service failing): After X failures, stop sending requests for Y seconds
3. **Half-Open State** (Testing): Send a test request to check if service recovered

**Implementation using Resilience4j**:

```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        // Call to Payment Service
        return restTemplate.postForObject(
            "http://payment-service/api/payment", 
            request, 
            PaymentResponse.class
        );
    }
    
    // Fallback method when circuit is open
    public PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        return new PaymentResponse("PENDING", "Payment service temporarily unavailable");
    }
}
```

**Configuration (application.yml)**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        registerHealthIndicator: true
        slidingWindowSize: 10              # Monitor last 10 requests
        failureRateThreshold: 50           # Open circuit if 50% fail
        waitDurationInOpenState: 30s       # Wait 30 seconds before retry
        permittedNumberOfCallsInHalfOpenState: 3  # Test with 3 requests
```

**When Circuit Opens**:
- After 5 out of 10 requests fail
- Stop calling Payment Service for 30 seconds
- After 30 seconds, try 3 test requests
- If successful, close circuit; if not, reopen

---

### Solution 2: Retry Mechanism

**What it does**: Automatically retries failed requests for temporary failures (network glitches, temporary service unavailability).

**When to use**: For transient failures that might resolve quickly (timeouts, 503 errors).

**When NOT to use**: For business errors (400 Bad Request, 401 Unauthorized) - retrying won't help.

**Implementation**:

```java
@Service
public class OrderService {
    
    @Retry(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject(
            "http://payment-service/api/payment", 
            request, 
            PaymentResponse.class
        );
    }
    
    public PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        log.error("All retry attempts failed: {}", ex.getMessage());
        return new PaymentResponse("FAILED", "Unable to process payment");
    }
}
```

**Configuration**:

```yaml
resilience4j:
  retry:
    instances:
      paymentService:
        maxAttempts: 3                    # Try 3 times total
        waitDuration: 2s                  # Wait 2 seconds between attempts
        retryExceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
          - com.example.BusinessException  # Don't retry business errors
```

**Retry Strategy**:
```
Attempt 1: Immediate
Attempt 2: After 2 seconds
Attempt 3: After 4 seconds (exponential backoff can be configured)
```

---

### Solution 3: Timeout Configuration

**What it does**: Prevents requests from hanging indefinitely when a service is unresponsive.

**The Problem Without Timeouts**:
```
Order Service calls Payment Service
↓
Payment Service is frozen (takes 5 minutes to respond)
↓
Order Service threads are blocked
↓
All threads get exhausted
↓
Order Service becomes unresponsive
```

**Implementation**:

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        
        factory.setConnectTimeout(5000);      // 5 seconds to establish connection
        factory.setReadTimeout(10000);        // 10 seconds to read response
        
        return new RestTemplate(factory);
    }
}
```

**Best Practice Timeout Values**:
- **Connect Timeout**: 3-5 seconds (time to establish connection)
- **Read Timeout**: 10-30 seconds (time to receive response)
- Adjust based on your service's expected response time

---

### Solution 4: Fallback Mechanism

**What it does**: Provides alternative responses when a service fails.

**Types of Fallbacks**:

**A. Default Response**:
```java
@CircuitBreaker(name = "inventoryService", fallbackMethod = "getDefaultInventory")
public InventoryResponse checkInventory(String productId) {
    return restTemplate.getForObject(
        "http://inventory-service/api/inventory/" + productId,
        InventoryResponse.class
    );
}

public InventoryResponse getDefaultInventory(String productId, Exception ex) {
    // Return safe default
    return new InventoryResponse(productId, 0, "OUT_OF_STOCK");
}
```

**B. Cached Response** (Better for read operations):
```java
@Service
public class ProductService {
    
    @Autowired
    private RedisTemplate<String, ProductInfo> redisTemplate;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFromCache")
    public ProductInfo getProduct(String productId) {
        ProductInfo product = restTemplate.getForObject(
            "http://product-service/api/products/" + productId,
            ProductInfo.class
        );
        
        // Cache successful response
        redisTemplate.opsForValue().set("product:" + productId, product, 1, TimeUnit.HOURS);
        
        return product;
    }
    
    public ProductInfo getProductFromCache(String productId, Exception ex) {
        log.warn("Product service unavailable, serving from cache");
        
        ProductInfo cached = redisTemplate.opsForValue().get("product:" + productId);
        
        if (cached != null) {
            return cached;
        }
        
        throw new ProductNotFoundException("Product not available");
    }
}
```

**C. Graceful Degradation**:
```java
@CircuitBreaker(name = "recommendationService", fallbackMethod = "getBasicRecommendations")
public List<Product> getPersonalizedRecommendations(String userId) {
    // Call AI-powered recommendation service
    return restTemplate.getForObject(
        "http://recommendation-service/api/personalized/" + userId,
        List.class
    );
}

public List<Product> getBasicRecommendations(String userId, Exception ex) {
    // Fallback to simple rule-based recommendations
    return productRepository.findTopSellingProducts(10);
}
```

---

### Solution 5: Asynchronous Processing

**What it does**: Decouples services using message queues instead of synchronous REST calls.

**Synchronous Problem**:
```
Order Service → (waits) → Payment Service → (waits) → Inventory Service
      ↓ Blocked              ↓ Blocked              ↓ Processing
   5 seconds             3 seconds              2 seconds
Total: 10 seconds (user waits)
```

**Asynchronous Solution**:
```
Order Service → Publish Event → Message Queue
      ↓ Returns immediately
   User gets confirmation
   
Message Queue → Payment Service (processes async)
             → Inventory Service (processes async)
```

**Implementation with Kafka**:

```java
// Order Service - Publisher
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public OrderResponse createOrder(OrderRequest request) {
        // 1. Save order in PENDING state
        Order order = orderRepository.save(new Order(request, "PENDING"));
        
        // 2. Publish event to Kafka
        OrderEvent event = new OrderEvent(order.getId(), request);
        kafkaTemplate.send("order-created", event);
        
        // 3. Return immediately
        return new OrderResponse(order.getId(), "PENDING", 
            "Your order is being processed");
    }
}

// Payment Service - Consumer
@Service
public class PaymentConsumer {
    
    @KafkaListener(topics = "order-created", groupId = "payment-group")
    public void processPayment(OrderEvent event) {
        try {
            // Process payment
            PaymentResult result = paymentGateway.charge(event.getAmount());
            
            // Publish result
            kafkaTemplate.send("payment-completed", 
                new PaymentEvent(event.getOrderId(), result));
                
        } catch (Exception ex) {
            // Publish failure event
            kafkaTemplate.send("payment-failed", 
                new PaymentEvent(event.getOrderId(), ex.getMessage()));
        }
    }
}

// Inventory Service - Consumer
@Service
public class InventoryConsumer {
    
    @KafkaListener(topics = "payment-completed", groupId = "inventory-group")
    public void reserveInventory(PaymentEvent event) {
        try {
            // Reserve inventory
            inventoryService.reserve(event.getOrderId());
            
            // Publish success
            kafkaTemplate.send("inventory-reserved", 
                new InventoryEvent(event.getOrderId(), "RESERVED"));
                
        } catch (Exception ex) {
            // Publish failure (triggers compensation)
            kafkaTemplate.send("inventory-failed", 
                new InventoryEvent(event.getOrderId(), "FAILED"));
        }
    }
}
```

**Benefits**:
- **Non-blocking**: User gets immediate response
- **Resilient**: If a service is down, messages wait in queue
- **Scalable**: Add more consumers to handle load
- **Decoupled**: Services don't need to know about each other

**Trade-offs**:
- **Eventual Consistency**: Not immediate
- **Complexity**: Need to handle message ordering, duplicates
- **Monitoring**: Harder to trace requests across services

---

## Q2: Distributed Transaction Management

### The Problem

**Scenario**: In an e-commerce application, an order service calls payment and inventory services. How do you ensure that the order is placed only if BOTH payment and inventory updates are successful?

**Challenge**:
```
Order Service:   Create Order ✓
Payment Service: Charge Card ✓
Inventory Service: Reserve Stock ✗ (FAILS - Out of Stock)

Problem: Money charged but no product! User is angry!
```

**Traditional Database Solution (DOESN'T WORK in Microservices)**:
```java
// This works in monolith with single database
@Transactional
public void createOrder() {
    orderRepository.save(order);        // Same DB
    paymentRepository.save(payment);    // Same DB
    inventoryRepository.save(inventory); // Same DB
    // If any fails, ALL rollback automatically
}
```

**In Microservices** (Each service has its own database):
```
Order DB     Payment DB     Inventory DB
   ↓             ↓               ↓
Can't use @Transactional across different databases!
```

---

### Solution 1: Saga Pattern (Choreography)

**What it is**: Each service publishes events, and other services react to them. If something fails, compensating transactions undo the changes.

**How it works**:

**Happy Path** (Everything succeeds):
```
1. Order Service:    Create Order (PENDING) → Publish "OrderCreated"
2. Payment Service:  Charge Card → Publish "PaymentCompleted"
3. Inventory Service: Reserve Stock → Publish "InventoryReserved"
4. Order Service:    Update Order (CONFIRMED)
```

**Failure Path** (Inventory fails):
```
1. Order Service:    Create Order (PENDING) → Publish "OrderCreated"
2. Payment Service:  Charge Card → Publish "PaymentCompleted"
3. Inventory Service: Out of Stock → Publish "InventoryFailed"
4. Payment Service:  Refund Card (Compensation)
5. Order Service:    Update Order (CANCELLED)
```

**Implementation**:

```java
// 1. Order Service
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public OrderResponse createOrder(OrderRequest request) {
        // Create order in PENDING state
        Order order = new Order(request);
        order.setStatus("PENDING");
        orderRepository.save(order);
        
        // Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            request.getUserId(),
            request.getProductId(),
            request.getAmount()
        );
        kafkaTemplate.send("order-created", event);
        
        return new OrderResponse(order.getId(), "PENDING");
    }
    
    // Listen for success
    @KafkaListener(topics = "inventory-reserved")
    public void onInventoryReserved(InventoryReservedEvent event) {
        Order order = orderRepository.findById(event.getOrderId());
        order.setStatus("CONFIRMED");
        orderRepository.save(order);
        
        // Notify user
        notificationService.sendEmail(order.getUserId(), "Order Confirmed!");
    }
    
    // Listen for failures (Compensation)
    @KafkaListener(topics = "inventory-failed")
    public void onInventoryFailed(InventoryFailedEvent event) {
        Order order = orderRepository.findById(event.getOrderId());
        order.setStatus("CANCELLED");
        order.setCancellationReason("Out of stock");
        orderRepository.save(order);
        
        // Notify user
        notificationService.sendEmail(order.getUserId(), "Order Cancelled - Refund Initiated");
    }
}

// 2. Payment Service
@Service
public class PaymentService {
    
    @KafkaListener(topics = "order-created")
    public void processPayment(OrderCreatedEvent event) {
        try {
            // Charge payment
            PaymentTransaction txn = paymentGateway.charge(
                event.getUserId(),
                event.getAmount()
            );
            
            // Save transaction
            paymentRepository.save(txn);
            
            // Publish success
            PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                event.getOrderId(),
                txn.getId(),
                event.getProductId(),
                event.getAmount()
            );
            kafkaTemplate.send("payment-completed", paymentEvent);
            
        } catch (PaymentFailedException ex) {
            // Publish failure
            PaymentFailedEvent failEvent = new PaymentFailedEvent(
                event.getOrderId(),
                ex.getMessage()
            );
            kafkaTemplate.send("payment-failed", failEvent);
        }
    }
    
    // Compensation: Refund if inventory fails
    @KafkaListener(topics = "inventory-failed")
    public void refundPayment(InventoryFailedEvent event) {
        PaymentTransaction txn = paymentRepository.findByOrderId(event.getOrderId());
        
        if (txn != null && txn.getStatus().equals("COMPLETED")) {
            // Issue refund
            paymentGateway.refund(txn.getId());
            txn.setStatus("REFUNDED");
            paymentRepository.save(txn);
            
            log.info("Refunded payment for order: {}", event.getOrderId());
        }
    }
}

// 3. Inventory Service
@Service
public class InventoryService {
    
    @KafkaListener(topics = "payment-completed")
    public void reserveInventory(PaymentCompletedEvent event) {
        try {
            // Check stock
            Inventory inventory = inventoryRepository.findByProductId(event.getProductId());
            
            if (inventory.getQuantity() > 0) {
                // Reserve stock
                inventory.setQuantity(inventory.getQuantity() - 1);
                inventoryRepository.save(inventory);
                
                // Publish success
                InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    event.getOrderId(),
                    event.getProductId()
                );
                kafkaTemplate.send("inventory-reserved", reservedEvent);
                
            } else {
                // Out of stock
                throw new OutOfStockException("Product not available");
            }
            
        } catch (Exception ex) {
            // Publish failure (triggers compensation)
            InventoryFailedEvent failEvent = new InventoryFailedEvent(
                event.getOrderId(),
                event.getProductId(),
                ex.getMessage()
            );
            kafkaTemplate.send("inventory-failed", failEvent);
        }
    }
}
```

**Pros**:
- Simple to implement
- No central coordinator
- Services are loosely coupled

**Cons**:
- Hard to debug (distributed trace needed)
- Risk of event loss
- Complex error handling

---

### Solution 2: Saga Pattern (Orchestration)

**What it is**: A central orchestrator service manages the entire transaction flow.

**How it works**:

```
              Saga Orchestrator
                    |
      +-------------+-------------+
      |             |             |
Order Service  Payment Service  Inventory Service
```

**Implementation**:

```java
// Saga Orchestrator
@Service
public class OrderSagaOrchestrator {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public OrderResult executeOrderSaga(OrderRequest request) {
        String orderId = null;
        String paymentId = null;
        
        try {
            // Step 1: Create Order
            orderId = createOrder(request);
            
            // Step 2: Process Payment
            paymentId = processPayment(orderId, request.getAmount());
            
            // Step 3: Reserve Inventory
            reserveInventory(orderId, request.getProductId());
            
            // All successful
            confirmOrder(orderId);
            return new OrderResult(orderId, "SUCCESS");
            
        } catch (PaymentFailedException ex) {
            // Payment failed - cancel order
            cancelOrder(orderId);
            return new OrderResult(null, "PAYMENT_FAILED");
            
        } catch (InventoryException ex) {
            // Inventory failed - refund and cancel
            if (paymentId != null) {
                refundPayment(paymentId);
            }
            cancelOrder(orderId);
            return new OrderResult(null, "OUT_OF_STOCK");
            
        } catch (Exception ex) {
            // Unknown error - rollback everything
            if (paymentId != null) {
                refundPayment(paymentId);
            }
            if (orderId != null) {
                cancelOrder(orderId);
            }
            return new OrderResult(null, "ERROR");
        }
    }
    
    private String createOrder(OrderRequest request) {
        OrderResponse response = restTemplate.postForObject(
            "http://order-service/api/orders",
            request,
            OrderResponse.class
        );
        return response.getOrderId();
    }
    
    private String processPayment(String orderId, BigDecimal amount) {
        PaymentRequest paymentReq = new PaymentRequest(orderId, amount);
        PaymentResponse response = restTemplate.postForObject(
            "http://payment-service/api/payments",
            paymentReq,
            PaymentResponse.class
        );
        
        if (!response.isSuccess()) {
            throw new PaymentFailedException("Payment declined");
        }
        
        return response.getPaymentId();
    }
    
    private void reserveInventory(String orderId, String productId) {
        InventoryRequest invReq = new InventoryRequest(orderId, productId);
        InventoryResponse response = restTemplate.postForObject(
            "http://inventory-service/api/inventory/reserve",
            invReq,
            InventoryResponse.class
        );
        
        if (!response.isSuccess()) {
            throw new InventoryException("Out of stock");
        }
    }
    
    // Compensation methods
    private void refundPayment(String paymentId) {
        restTemplate.postForObject(
            "http://payment-service/api/payments/" + paymentId + "/refund",
            null,
            Void.class
        );
    }
    
    private void cancelOrder(String orderId) {
        restTemplate.postForObject(
            "http://order-service/api/orders/" + orderId + "/cancel",
            null,
            Void.class
        );
    }
    
    private void confirmOrder(String orderId) {
        restTemplate.postForObject(
            "http://order-service/api/orders/" + orderId + "/confirm",
            null,
            Void.class
        );
    }
}
```

**Pros**:
- Easier to understand and debug
- Clear transaction flow
- Centralized error handling

**Cons**:
- Single point of failure (orchestrator)
- Orchestrator becomes complex
- Tight coupling to orchestrator

---

### Solution 3: Two-Phase Commit (2PC)

**What it is**: A distributed transaction protocol with a coordinator.

**Phases**:

**Phase 1 - Prepare** (Voting):
```
Coordinator: "Can everyone commit?"
Order Service:     "YES, I'm ready"
Payment Service:   "YES, I'm ready"
Inventory Service: "YES, I'm ready"
```

**Phase 2 - Commit**:
```
Coordinator: "Everyone commit now!"
Order Service:     Commits
Payment Service:   Commits
Inventory Service: Commits
```

**If anyone says NO in Phase 1**:
```
Coordinator: "Everyone rollback!"
All services: Rollback their changes
```

**Implementation** (Using XA Transactions):

```java
// Configuration
@Configuration
public class XAConfig {
    
    @Bean
    public JtaTransactionManager transactionManager() {
        UserTransactionManager utm = new UserTransactionManager();
        UserTransactionImp uti = new UserTransactionImp();
        
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setTransactionManager(utm);
        jtaTransactionManager.setUserTransaction(uti);
        
        return jtaTransactionManager;
    }
}

// Service with 2PC
@Service
public class OrderService {
    
    @Autowired
    private JtaTransactionManager transactionManager;
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(OrderRequest request) {
        // All these participate in XA transaction
        orderRepository.save(order);              // DB 1
        paymentService.processPayment(payment);   // DB 2
        inventoryService.reserve(product);        // DB 3
        
        // If any fails, ALL rollback automatically
    }
}
```

**Pros**:
- Strong consistency (ACID)
- Automatic rollback

**Cons**:
- Blocking (services wait for coordinator)
- Poor performance
- Single point of failure
- Not recommended for microservices

---

### Solution 4: Outbox Pattern

**What it is**: Ensures reliable event publishing even if the message broker is down.

**The Problem**:
```java
// This can fail!
@Transactional
public void createOrder(OrderRequest request) {
    orderRepository.save(order);              // DB commit ✓
    kafkaTemplate.send("order-created", event); // Kafka fails ✗
    // Order saved but event not published!
}
```

**The Solution** (Outbox Pattern):

```java
// 1. Save order AND event in same transaction
@Transactional
public void createOrder(OrderRequest request) {
    // Save order
    Order order = orderRepository.save(new Order(request));
    
    // Save event in outbox table (same DB, same transaction)
    OutboxEvent event = new OutboxEvent(
        "OrderCreated",
        order.getId(),
        new OrderCreatedEvent(order)
    );
    outboxRepository.save(event);
    
    // Both saved atomically!
}

// 2. Background job publishes events from outbox
@Scheduled(fixedDelay = 5000) // Every 5 seconds
public void publishOutboxEvents() {
    List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");
    
    for (OutboxEvent event : pendingEvents) {
        try {
            // Publish to Kafka
            kafkaTemplate.send(event.getEventType(), event.getPayload());
            
            // Mark as published
            event.setStatus("PUBLISHED");
            outboxRepository.save(event);
            
        } catch (Exception ex) {
            log.error("Failed to publish event: {}", event.getId());
            // Will retry in next cycle
        }
    }
}
```

**Database Schema**:
```sql
CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, PUBLISHED, FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL
);

CREATE INDEX idx_status ON outbox_events(status);
```

**Pros**:
- Guaranteed event delivery
- Atomic (DB + Event)
- No message loss

**Cons**:
- Additional complexity
- Delayed event publishing
- Need cleanup of old events

---

## Complete Implementation Examples

### Example 1: E-Commerce Order Flow with Saga

```java
// Domain Events
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private String productId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    // getters, setters, constructor
}

public class PaymentCompletedEvent {
    private String orderId;
    private String paymentId;
    private String productId;
    private BigDecimal amount;
    // getters, setters, constructor
}

public class InventoryReservedEvent {
    private String orderId;
    private String productId;
    // getters, setters, constructor
}

// Failure Events
public class PaymentFailedEvent {
    private String orderId;
    private String reason;
    // getters, setters, constructor
}

public class InventoryFailedEvent {
    private String orderId;
    private String productId;
    private String reason;
    // getters, setters, constructor
}

// Order Entity
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;
    private String userId;
    private String productId;
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, CONFIRMED, CANCELLED
    
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // getters, setters
}

// Payment Entity
@Entity
@Table(name = "payments")
public class PaymentTransaction {
    @Id
    private String id;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, COMPLETED, REFUNDED
    
    private String transactionId;
    private LocalDateTime createdAt;
    // getters, setters
}

// Inventory Entity
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    private String productId;
    private String productName;
    private Integer quantity;
    private LocalDateTime updatedAt;
    // getters, setters
}
```

### Example 2: Combining Multiple Patterns

```java
@Service
public class ResilientOrderService {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private RestTemplate restTemplate;
    
    // Circuit Breaker + Retry + Timeout + Fallback
    @CircuitBreaker(name = "createOrder", fallbackMethod = "createOrderFallback")
    @Retry(name = "createOrder")
    @Timeout(name = "createOrder")
    public OrderResponse createOrder(OrderRequest request) {
        
        // 1. Validate request
        validateOrder(request);
        
        // 2. Create order in PENDING state
        Order order = new Order(request);
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        
        // 3. Save event in outbox (atomic with order)
        OutboxEvent event = new OutboxEvent(
            "OrderCreated",
            savedOrder.getId(),
            new OrderCreatedEvent(savedOrder)
        );
        outboxRepository.save(event);
        
        // 4. Return immediately
        return new OrderResponse(
            savedOrder.getId(),
            OrderStatus.PENDING,
            "Your order is being processed"
        );
    }
    
    // Fallback when all retries fail
    public OrderResponse createOrderFallback(OrderRequest request, Exception ex) {
        log.error("Order creation failed after retries: {}", ex.getMessage());
        
        // Save failed order for manual review
        FailedOrder failedOrder = new FailedOrder(request, ex.getMessage());
        failedOrderRepository.save(failedOrder);
        
        // Return graceful error
        return new OrderResponse(
            null,
            OrderStatus.FAILED,
            "We're experiencing technical difficulties. Please try again later."
        );
    }
}
```

**Configuration (application.yml)**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      createOrder:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        
  retry:
    instances:
      createOrder:
        maxAttempts: 3
        waitDuration: 2s
        retryExceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException
          
  timelimiter:
    instances:
      createOrder:
        timeoutDuration: 10s
```

---

## Comparison of Approaches

### Failure Handling Patterns

| Pattern | Use Case | Pros | Cons |
|---------|----------|------|------|
| Circuit Breaker | Prevent cascading failures | Protects system, Fast fail | Adds complexity |
| Retry | Transient failures | Simple, Effective for temporary issues | Can worsen overload |
| Timeout | Unresponsive services | Prevents hanging | May interrupt valid operations |
| Fallback | Graceful degradation | Better UX | May return stale data |
| Async | Non-critical operations | Non-blocking, Resilient | Eventual consistency |

### Distributed Transaction Patterns

| Pattern | Consistency | Complexity | Performance | Best For |
|---------|-------------|------------|-------------|----------|
| Saga (Choreography) | Eventual | Medium | High | Event-driven systems |
| Saga (Orchestration) | Eventual | Medium | Medium | Complex workflows |
| 2PC | Strong (ACID) | High | Low | Banking, Critical data |
| Outbox | Eventual | Low | High | Reliable messaging |

---

## Best Practices

### Failure Handling

1. **Always use timeouts** - Never let requests hang indefinitely
2. **Combine patterns** - Circuit Breaker + Retry + Fallback together
3. **Log failures** - Track patterns to identify root causes
4. **Monitor circuit breakers** - Set up alerts when circuits open
5. **Test failure scenarios** - Use chaos engineering (e.g., Chaos Monkey)
6. **Idempotency** - Ensure operations can be safely retried
7. **Bulkhead pattern** - Isolate thread pools to prevent cascading failures

### Distributed Transactions

1. **Prefer Saga over 2PC** - Better for microservices
2. **Design for failure** - Always have compensation logic
3. **Idempotent operations** - Services should handle duplicate messages
4. **Event versioning** - Plan for schema evolution
5. **Dead letter queues** - Handle messages that fail repeatedly
6. **Correlation IDs** - Track requests across services
7. **Monitor saga completion** - Alert on stuck sagas
8. **Keep sagas short** - Long-running sagas are hard to manage

### Data Consistency

1. **Embrace eventual consistency** - It's okay for data to be temporarily inconsistent
2. **Use status fields** - Track state transitions (PENDING → COMPLETED → FAILED)
3. **Implement compensating transactions** - For rollbacks
4. **Audit logs** - Track all state changes for debugging
5. **Reconciliation jobs** - Periodically verify data consistency
6. **Unique identifiers** - Use UUIDs or distributed ID generators

---

## Common Interview Questions

### Q1: What's the difference between Circuit Breaker and Retry?

**Answer**:
- **Retry**: Tries the same operation multiple times hoping it succeeds. Best for transient failures.
- **Circuit Breaker**: Stops trying after detecting repeated failures, giving the system time to recover. Prevents cascading failures.
- **Use Together**: Circuit breaker prevents retries when service is known to be down.

### Q2: When should you NOT use Saga pattern?

**Answer**:
- When you need strong consistency (ACID transactions)
- For financial operations requiring immediate consistency (use 2PC or single service)
- When compensation is impossible or too complex
- For simple operations that can be in a single service

### Q3: How do you handle duplicate messages in event-driven architecture?

**Answer**:
Use idempotency:
```java
@KafkaListener(topics = "payment-completed")
public void processPayment(PaymentEvent event) {
    // Check if already processed
    if (paymentRepository.existsByTransactionId(event.getTransactionId())) {
        log.info("Payment already processed, skipping");
        return;
    }
    
    // Process payment
    Payment payment = new Payment(event);
    paymentRepository.save(payment);
}
```

### Q4: What happens if the orchestrator fails in Saga Orchestration?

**Answer**:
- Store saga state in a database
- On restart, orchestrator reads pending sagas and continues
- Use a stateful orchestrator framework (e.g., Temporal, Camunda)

### Q5: How do you test failure scenarios?

**Answer**:
1. **Unit tests**: Mock failures in dependencies
2. **Integration tests**: Use Testcontainers with WireMock to simulate failures
3. **Chaos engineering**: Use Chaos Monkey to randomly kill services
4. **Load testing**: Stress test to find breaking points

### Q6: What's the difference between Saga and Outbox pattern?

**Answer**:
- **Saga**: Manages distributed transactions across services
- **Outbox**: Ensures reliable event publishing from a single service
- **Together**: Outbox ensures events are published reliably in each step of a Saga

### Q7: How do you monitor distributed transactions?

**Answer**:
1. **Correlation IDs**: Track requests across services
2. **Distributed tracing**: Use Zipkin, Jaeger
3. **Saga dashboards**: Visualize saga state
4. **Metrics**: Track success/failure rates, duration
5. **Alerts**: Notify on stuck or failed sagas

---

### Tools
- **Resilience4j**: Circuit breaker, retry, rate limiter
- **Spring Cloud**: Comprehensive microservices toolkit
- **Zipkin/Jaeger**: Distributed tracing
- **Kafka**: Event streaming platform
- **Temporal**: Workflow orchestration

---