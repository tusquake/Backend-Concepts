# Apache Kafka - Complete Guide with Spring Boot

## Table of Contents
1. [What is Kafka?](#what-is-kafka)
2. [Core Concepts](#core-concepts)
3. [When to Use Kafka?](#when-to-use-kafka)
4. [Architecture](#architecture)
5. [Spring Boot Integration](#spring-boot-integration)
6. [Real-World Examples](#real-world-examples)
7. [Advanced Topics](#advanced-topics)
8. [Performance Tuning](#performance-tuning)
9. [Monitoring and Metrics](#monitoring-and-metrics)
10. [Interview Questions](#interview-questions)
11. [Common Pitfalls](#common-pitfalls)

---

## What is Kafka?

**Simple Definition**: Kafka is a distributed messaging system that acts like a high-speed postal service for your applications.

**Real-World Analogy**: 
Think of Kafka as a **newspaper delivery system**:
- **Publishers** (newspapers) → Producers
- **Newspaper office** (storage) → Kafka Broker
- **Subscribers** (readers) → Consumers
- **Different sections** (Sports, News, Tech) → Topics

**Technical Definition**: 
Apache Kafka is a distributed event streaming platform used for building real-time data pipelines and streaming applications.

---

## Core Concepts

### 1. Producer
**What**: Application that sends messages to Kafka

**Real-World Example**: 
- E-commerce website sending "Order Placed" events
- Mobile app sending "User Login" events
- IoT sensor sending temperature readings

**Analogy**: A person posting letters at the post office

```java
// Producer sends messages
producer.send("order-topic", "Order #12345 placed");
```

### 2. Consumer
**What**: Application that reads messages from Kafka

**Real-World Example**:
- Email service reading "Order Placed" to send confirmation
- Analytics service reading "User Login" to track metrics
- Warehouse service reading orders to prepare shipment

**Analogy**: A person receiving letters from their mailbox

```java
// Consumer reads messages
String message = consumer.poll("order-topic");
```

### 3. Topic
**What**: Category or feed name where messages are stored

**Real-World Example**:
- `order-events` - All order-related messages
- `user-events` - All user activity messages
- `payment-events` - All payment transactions

**Analogy**: Different newspaper sections (Sports, Business, Tech)

**Naming Convention**: 
- `domain.entity.action` → `ecommerce.order.created`
- `service.event.type` → `payment.transaction.completed`

### 4. Partition
**What**: Topics are divided into partitions for scalability

**Real-World Example**:
Topic: `order-events`
- Partition 0: Orders from Region A
- Partition 1: Orders from Region B
- Partition 2: Orders from Region C

**Analogy**: Multiple checkout counters at a supermarket (parallel processing)

**Why Partitions?**
- **Scalability**: Multiple consumers can read in parallel
- **Ordering**: Messages in same partition maintain order
- **Load Distribution**: Spread data across multiple servers

```
Topic: orders
├── Partition 0: [msg1, msg2, msg3]
├── Partition 1: [msg4, msg5, msg6]
└── Partition 2: [msg7, msg8, msg9]
```

### 5. Offset
**What**: Unique ID for each message in a partition

**Real-World Example**:
```
Partition 0:
Offset 0: "Order #1001 placed"
Offset 1: "Order #1002 placed"
Offset 2: "Order #1003 placed"
```

**Analogy**: Page numbers in a book (you can bookmark and continue from where you left)

**Use Case**: Resume reading from where you stopped (fault tolerance)

### 6. Consumer Group
**What**: Multiple consumers working together as a team

**Real-World Example**:
Consumer Group: `email-service`
- Consumer 1: Reads Partition 0
- Consumer 2: Reads Partition 1
- Consumer 3: Reads Partition 2

**Analogy**: Team of workers where each worker handles specific tasks

**Benefits**:
- **Load Balancing**: Work is distributed
- **Fault Tolerance**: If one fails, others continue
- **Scalability**: Add more consumers to handle more load

```
Topic: orders (3 partitions)
Consumer Group: email-service
├── Consumer 1 → Partition 0
├── Consumer 2 → Partition 1
└── Consumer 3 → Partition 2
```

### 7. Broker
**What**: Kafka server that stores and serves messages

**Real-World Example**:
- Broker 1: Stores partitions 0, 3, 6
- Broker 2: Stores partitions 1, 4, 7
- Broker 3: Stores partitions 2, 5, 8

**Analogy**: Different post office branches in a city

**Cluster**: Multiple brokers working together (3-5 brokers typically)

### 8. ZooKeeper (being replaced by KRaft)
**What**: Manages Kafka cluster metadata

**Real-World Example**: 
- Tracks which broker is the leader
- Maintains cluster configuration
- Monitors broker health

**Analogy**: Manager who coordinates all post office branches

**Note**: Kafka 3.0+ is moving to KRaft (Kafka Raft) to remove ZooKeeper dependency

### 9. Replication
**What**: Copies of partitions stored on multiple brokers

**Real-World Example**:
```
Partition 0 (Replication Factor = 3)
├── Leader: Broker 1 (handles reads/writes)
├── Follower: Broker 2 (backup copy)
└── Follower: Broker 3 (backup copy)
```

**Analogy**: Keeping backup copies of important documents in multiple locations

**Benefits**:
- **Fault Tolerance**: If leader fails, follower becomes leader
- **High Availability**: Data never lost
- **Disaster Recovery**: Survive broker failures

---

## When to Use Kafka?

### Good Use Cases

#### 1. Event-Driven Architecture
**Example**: E-commerce Order Processing
```
Order Placed → Kafka → Multiple Services React
├── Email Service: Send confirmation
├── Inventory Service: Reserve items
├── Payment Service: Process payment
└── Analytics Service: Track metrics
```

#### 2. Real-Time Analytics
**Example**: Website Click Tracking
```
User Clicks → Kafka → Analytics Dashboard
- Track page views in real-time
- Monitor user behavior
- Generate live reports
```

#### 3. Log Aggregation
**Example**: Collecting Application Logs
```
App Server 1 → Kafka ← App Server 2
                 ↓
           Log Processing
           (Elasticsearch)
```

#### 4. Microservices Communication
**Example**: Decoupling Services
```
Service A → Kafka → Service B
(No direct dependency, async communication)
```

#### 5. Data Pipeline
**Example**: ETL (Extract, Transform, Load)
```
Database → Kafka → Transform → Data Warehouse
```

### Not Good Use Cases

1. **Request-Response Pattern**: Use REST/gRPC instead
2. **Small Data Volume**: Overhead not justified
3. **Immediate Consistency Required**: Kafka is eventually consistent
4. **Simple Point-to-Point**: Use message queues like RabbitMQ

---

## Architecture

### Basic Architecture

```
┌─────────────┐         ┌─────────────────────────┐
│  Producer   │────────>│   Kafka Cluster         │
│ (App/API)   │         │  ┌─────────────────┐    │
└─────────────┘         │  │   Broker 1      │    │
                        │  │  Topic: orders  │    │
┌─────────────┐         │  │   Partition 0   │    │
│  Producer   │────────>│  │   Partition 1   │    │
│ (App/API)   │         │  └─────────────────┘    │
└─────────────┘         │  ┌─────────────────┐    │
                        │  │   Broker 2      │    │
┌─────────────┐         │  │  Topic: orders  │    │
│  Consumer   │<────────│  │   Partition 2   │    │
│ (Service)   │         │  └─────────────────┘    │
└─────────────┘         └─────────────────────────┘
```

### Message Flow

```
1. Producer writes to Topic
   ↓
2. Kafka stores in Partition (based on key or round-robin)
   ↓
3. Message assigned an Offset
   ↓
4. Message replicated to follower brokers
   ↓
5. Consumer reads from Partition
   ↓
6. Consumer commits Offset (marks as processed)
```

---

## Spring Boot Integration

### 1. Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### 2. Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # Kafka broker address
    
    # Producer Configuration
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all  # Wait for all replicas to acknowledge
      retries: 3  # Retry failed sends
      batch-size: 16384  # 16KB batch
      linger-ms: 10  # Wait 10ms for batching
      compression-type: snappy  # Compress messages
      
    # Consumer Configuration
    consumer:
      group-id: my-consumer-group  # Consumer group name
      auto-offset-reset: earliest  # Start from beginning if no offset
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      enable-auto-commit: false  # Manual offset commit
      max-poll-records: 500  # Max records per poll
      
    # Listener Configuration
    listener:
      ack-mode: manual  # Manual acknowledgment
      concurrency: 3  # 3 consumer threads
```

### 3. Producer Example

```java
@Service
public class OrderProducer {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Simple send
    public void sendOrder(String orderId) {
        kafkaTemplate.send("order-topic", orderId);
    }
    
    // Send with key (for partitioning)
    public void sendOrderWithKey(String userId, String orderId) {
        kafkaTemplate.send("order-topic", userId, orderId);
        // Messages with same userId go to same partition (ordering)
    }
    
    // Send with callback
    public void sendOrderWithCallback(String orderId) {
        ListenableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send("order-topic", orderId);
            
        future.addCallback(
            result -> {
                System.out.println("Sent: " + orderId);
                System.out.println("Offset: " + result.getRecordMetadata().offset());
            },
            ex -> System.err.println("Failed: " + ex.getMessage())
        );
    }
    
    // Send JSON object
    public void sendOrderObject(Order order) throws JsonProcessingException {
        String orderJson = objectMapper.writeValueAsString(order);
        kafkaTemplate.send("order-topic", order.getUserId(), orderJson);
    }
    
    // Send with headers
    public void sendOrderWithHeaders(Order order) throws JsonProcessingException {
        ProducerRecord<String, String> record = new ProducerRecord<>(
            "order-topic",
            order.getUserId(),
            objectMapper.writeValueAsString(order)
        );
        
        record.headers().add("source", "web-app".getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        
        kafkaTemplate.send(record);
    }
}
```

### 4. Consumer Example

```java
@Service
public class OrderConsumer {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Simple consumer
    @KafkaListener(topics = "order-topic", groupId = "email-service")
    public void consumeOrder(String orderId) {
        System.out.println("Received order: " + orderId);
        // Send email confirmation
        sendEmailConfirmation(orderId);
    }
    
    // Consumer with metadata
    @KafkaListener(topics = "order-topic", groupId = "analytics-service")
    public void consumeWithMetadata(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp) {
        
        System.out.println("Topic: " + topic);
        System.out.println("Partition: " + partition);
        System.out.println("Offset: " + offset);
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Message: " + message);
    }
    
    // Consume JSON
    @KafkaListener(topics = "order-topic", groupId = "warehouse-service")
    public void consumeOrderObject(String orderJson) throws JsonProcessingException {
        Order order = objectMapper.readValue(orderJson, Order.class);
        System.out.println("Processing order: " + order.getOrderId());
        processOrder(order);
    }
    
    // Manual acknowledgment
    @KafkaListener(topics = "order-topic", groupId = "payment-service")
    public void consumeWithAck(String message, Acknowledgment acknowledgment) {
        try {
            // Process message
            processPayment(message);
            // Manually commit offset
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Don't acknowledge - will retry
            System.err.println("Processing failed: " + e.getMessage());
        }
    }
    
    // Batch consumer
    @KafkaListener(
        topics = "order-topic", 
        groupId = "batch-processor",
        containerFactory = "batchFactory"
    )
    public void consumeBatch(List<String> messages) {
        System.out.println("Received batch of " + messages.size() + " messages");
        messages.forEach(this::processMessage);
    }
    
    // Multiple topics
    @KafkaListener(topics = {"order-topic", "payment-topic"}, groupId = "multi-service")
    public void consumeMultipleTopics(String message) {
        System.out.println("Received: " + message);
    }
    
    // Topic pattern
    @KafkaListener(topicPattern = ".*-events", groupId = "event-processor")
    public void consumeByPattern(String message) {
        System.out.println("Received event: " + message);
    }
}
```

### 5. Configuration Class

```java
@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Producer Configuration
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "default-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }
    
    // Batch Listener Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }
}
```

---

## Real-World Examples

### Example 1: E-Commerce Order Processing

```java
// ========== Domain Models ==========

public class Order {
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private double totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    // getters, setters, constructors
}

public class OrderEvent {
    private String orderId;
    private String userId;
    private String eventType;  // ORDER_CREATED, ORDER_PAID, ORDER_SHIPPED
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    // getters, setters, constructors
}

// ========== Producer - Order Service ==========

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) 
            throws JsonProcessingException {
        
        // 1. Create order in database
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserId(request.getUserId());
        order.setItems(request.getItems());
        order.setTotalAmount(calculateTotal(request.getItems()));
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        
        orderRepository.save(order);
        
        // 2. Publish event to Kafka
        OrderEvent event = new OrderEvent(
            order.getOrderId(),
            order.getUserId(),
            "ORDER_CREATED",
            LocalDateTime.now()
        );
        event.setMetadata(Map.of(
            "totalAmount", order.getTotalAmount(),
            "itemCount", order.getItems().size()
        ));
        
        String eventJson = objectMapper.writeValueAsString(event);
        
        kafkaTemplate.send("order-events", 
            order.getUserId(),  // Key (ensures same user orders go to same partition)
            eventJson
        ).addCallback(
            result -> System.out.println("Order event published: " + order.getOrderId()),
            ex -> System.err.println("Failed to publish event: " + ex.getMessage())
        );
        
        return ResponseEntity.ok(new OrderResponse(order.getOrderId(), "Order created successfully"));
    }
    
    @PutMapping("/{orderId}/pay")
    public ResponseEntity<String> payOrder(@PathVariable String orderId) 
            throws JsonProcessingException {
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        
        // Publish payment event
        OrderEvent event = new OrderEvent(
            orderId,
            order.getUserId(),
            "ORDER_PAID",
            LocalDateTime.now()
        );
        
        kafkaTemplate.send("order-events", 
            order.getUserId(), 
            objectMapper.writeValueAsString(event)
        );
        
        return ResponseEntity.ok("Payment processed");
    }
}

// ========== Consumer 1 - Email Service ==========

@Service
public class EmailNotificationService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EmailService emailService;
    
    @KafkaListener(topics = "order-events", groupId = "email-service")
    public void sendOrderConfirmation(String eventJson, Acknowledgment ack) {
        try {
            OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);
            
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                    emailService.sendOrderConfirmation(
                        event.getUserId(), 
                        event.getOrderId()
                    );
                    System.out.println("Confirmation email sent for order: " + event.getOrderId());
                    break;
                    
                case "ORDER_PAID":
                    emailService.sendPaymentConfirmation(
                        event.getUserId(), 
                        event.getOrderId()
                    );
                    System.out.println("Payment email sent for order: " + event.getOrderId());
                    break;
                    
                case "ORDER_SHIPPED":
                    emailService.sendShippingNotification(
                        event.getUserId(), 
                        event.getOrderId()
                    );
                    System.out.println("Shipping email sent for order: " + event.getOrderId());
                    break;
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            // Don't acknowledge - will retry
        }
    }
}

// ========== Consumer 2 - Inventory Service ==========

@Service
public class InventoryService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderEvent(String eventJson, Acknowledgment ack) {
        try {
            OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);
            
            if ("ORDER_CREATED".equals(event.getEventType())) {
                // Reserve inventory
                boolean reserved = inventoryRepository.reserveItems(event.getOrderId());
                
                if (reserved) {
                    // Publish inventory reserved event
                    InventoryEvent invEvent = new InventoryEvent(
                        event.getOrderId(),
                        "INVENTORY_RESERVED",
                        LocalDateTime.now()
                    );
                    
                    kafkaTemplate.send("inventory-events", 
                        event.getOrderId(),
                        objectMapper.writeValueAsString(invEvent)
                    );
                    
                    System.out.println("Inventory reserved for order: " + event.getOrderId());
                } else {
                    // Publish inventory insufficient event
                    InventoryEvent invEvent = new InventoryEvent(
                        event.getOrderId(),
                        "INVENTORY_INSUFFICIENT",
                        LocalDateTime.now()
                    );
                    
                    kafkaTemplate.send("inventory-events", 
                        event.getOrderId(),
                        objectMapper.writeValueAsString(invEvent)
                    );
                    
                    System.out.println("Insufficient inventory for order: " + event.getOrderId());
                }
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Failed to process inventory: " + e.getMessage());
        }
    }
}

// ========== Consumer 3 - Analytics Service ==========

@Service
public class AnalyticsService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MetricsRepository metricsRepository;
    
    @KafkaListener(topics = "order-events", groupId = "analytics-service")
    public void trackOrderMetrics(String eventJson) {
        try {
            OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);
            
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                    metricsRepository.incrementOrderCount();
                    metricsRepository.recordOrderValue(
                        (Double) event.getMetadata().get("totalAmount")
                    );
                    System.out.println("Metrics updated for new order");
                    break;
                    
                case "ORDER_PAID":
                    metricsRepository.incrementPaymentCount();
                    System.out.println("Payment metrics updated");
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Failed to update analytics: " + e.getMessage());
            // Analytics failure shouldn't block processing
        }
    }
}

// ========== Consumer 4 - Warehouse Service ==========

@Service
public class WarehouseService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @KafkaListener(topics = "order-events", groupId = "warehouse-service")
    public void handleOrderForShipping(String eventJson, Acknowledgment ack) {
        try {
            OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);
            
            if ("ORDER_PAID".equals(event.getEventType())) {
                // Process for shipping
                boolean shipped = processShipping(event.getOrderId());
                
                if (shipped) {
                    // Publish shipping event
                    OrderEvent shippingEvent = new OrderEvent(
                        event.getOrderId(),
                        event.getUserId(),
                        "ORDER_SHIPPED",
                        LocalDateTime.now()
                    );
                    
                    kafkaTemplate.send("order-events", 
                        event.getUserId(),
                        objectMapper.writeValueAsString(shippingEvent)
                    );
                    
                    System.out.println("Order shipped: " + event.getOrderId());
                }
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Failed to process shipping: " + e.getMessage());
        }
    }
    
    private boolean processShipping(String orderId) {
        // Shipping logic
        return true;
    }
}
```

**Benefits of This Architecture**:
1. Email service fails? Order still processed
2. Inventory service slow? Doesn't block order creation
3. Add new service? Just add new consumer
4. Each service works independently
5. Easy to scale individual services
6. Fault tolerant - services can retry

### Example 2: Real-Time Stock Price Updates

```java
// ========== Domain Model ==========

public class StockPrice {
    private String symbol;
    private double price;
    private long volume;
    private LocalDateTime timestamp;
    // getters, setters
}

// ========== Producer - Stock Price Service ==========

@Service
public class StockPriceProducer {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private StockMarketAPI stockMarketAPI;
    
    @Scheduled(fixedRate = 1000)  // Every second
    public void publishStockPrices() throws JsonProcessingException {
        List<StockPrice> prices = stockMarketAPI.getCurrentPrices();
        
        for (StockPrice price : prices) {
            String priceJson = objectMapper.writeValueAsString(price);
            
            kafkaTemplate.send("stock-prices", 
                price.getSymbol(),  // Key (same stock to same partition)
                priceJson
            );
        }
        
        System.out.println("Published " + prices.size() + " stock prices");
    }
}

// ========== Consumer 1 - WebSocket Service ==========

@Service
public class StockPriceWebSocketService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "stock-prices", groupId = "websocket-service")
    public void broadcastPriceUpdate(String priceJson) throws JsonProcessingException {
        StockPrice price = objectMapper.readValue(priceJson, StockPrice.class);
        
        // Broadcast to WebSocket clients subscribed to this stock
        messagingTemplate.convertAndSend(
            "/topic/stock/" + price.getSymbol(),
            price
        );
        
        // Also broadcast to "all stocks" topic
        messagingTemplate.convertAndSend("/topic/stocks/all", price);
    }
}

// ========== Consumer 2 - Price Alert Service ==========

@Service
public class PriceAlertService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @KafkaListener(topics = "stock-prices", groupId = "alert-service")
    public void checkPriceAlerts(String priceJson) throws JsonProcessingException {
        StockPrice price = objectMapper.readValue(priceJson, StockPrice.class);
        
        // Get all alerts for this symbol
        List<PriceAlert> alerts = alertRepository.findBySymbol(price.getSymbol());
        
        for (PriceAlert alert : alerts) {
            if (alert.getCondition() == AlertCondition.ABOVE && 
                price.getPrice() >= alert.getTargetPrice()) {
                
                notificationService.sendAlert(
                    alert.getUserId(),
                    "Price Alert: " + price.getSymbol() + " is now $" + price.getPrice()
                );
                
                // Mark alert as triggered
                alert.setTriggered(true);
                alertRepository.save(alert);
            }
        }
    }
}

// ========== Consumer 3 - Historical Data Service ==========

@Service
public class HistoricalDataService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TimeSeriesDatabase timeSeriesDB;
    
    @KafkaListener(
        topics = "stock-prices", 
        groupId = "historical-data",
        containerFactory = "batchFactory"
    )
    public void storeHistoricalData(List<String> pricesJson) throws JsonProcessingException {
        List<StockPrice> prices = new ArrayList<>();
        
        for (String json : pricesJson) {
            prices.add(objectMapper.readValue(json, StockPrice.class));
        }
        
        // Batch insert for performance
        timeSeriesDB.saveBatch(prices);
        
        System.out.println("Stored " + prices.size() + " historical price records");
    }
}
```

### Example 3: IoT Sensor Data Processing

```java
// ========== Domain Model ==========

public class SensorData {
    private String deviceId;
    private String sensorType;
    private double value;
    private String unit;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    // getters, setters
}

// ========== Producer - IoT Device Simulator ==========

@Service
public class IoTDeviceSimulator {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Scheduled(fixedRate = 5000)  // Every 5 seconds
    public void publishSensorData() throws JsonProcessingException {
        List<String> devices = Arrays.asList("device-001", "device-002", "device-003");
        
        for (String deviceId : devices) {
            SensorData temperature = new SensorData(
                deviceId,
                "TEMPERATURE",
                20 + Math.random() * 15,  // 20-35°C
                "CELSIUS",
                LocalDateTime.now()
            );
            
            SensorData humidity = new SensorData(
                deviceId,
                "HUMIDITY",
                40 + Math.random() * 40,  // 40-80%
                "PERCENT",
                LocalDateTime.now()
            );
            
            kafkaTemplate.send("sensor-data", 
                deviceId, 
                objectMapper.writeValueAsString(temperature)
            );
            
            kafkaTemplate.send("sensor-data", 
                deviceId, 
                objectMapper.writeValueAsString(humidity)
            );
        }
    }
}

// ========== Consumer 1 - Anomaly Detection ==========

@Service
public class AnomalyDetectionService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @KafkaListener(topics = "sensor-data", groupId = "anomaly-detection")
    public void detectAnomaly(String dataJson) throws JsonProcessingException {
        SensorData data = objectMapper.readValue(dataJson, SensorData.class);
        
        boolean isAnomaly = false;
        String reason = "";
        
        if (data.getSensorType().equals("TEMPERATURE")) {
            if (data.getValue() > 35.0) {
                isAnomaly = true;
                reason = "Temperature too high";
            } else if (data.getValue() < 10.0) {
                isAnomaly = true;
                reason = "Temperature too low";
            }
        }
        
        if (isAnomaly) {
            Alert alert = new Alert(
                data.getDeviceId(),
                data.getSensorType(),
                data.getValue(),
                reason,
                LocalDateTime.now()
            );
            
            kafkaTemplate.send("alerts", 
                data.getDeviceId(),
                objectMapper.writeValueAsString(alert)
            );
            
            System.out.println("Anomaly detected: " + reason + " on " + data.getDeviceId());
        }
    }
}

// ========== Consumer 2 - Real-time Dashboard ==========

@Service
public class DashboardService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @KafkaListener(topics = "sensor-data", groupId = "dashboard-service")
    public void updateDashboard(String dataJson) throws JsonProcessingException {
        SensorData data = objectMapper.readValue(dataJson, SensorData.class);
        
        // Send to WebSocket for real-time dashboard
        messagingTemplate.convertAndSend(
            "/topic/device/" + data.getDeviceId(),
            data
        );
    }
}

// ========== Consumer 3 - Data Storage ==========

@Service
public class SensorDataStorageService {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SensorDataRepository repository;
    
    @KafkaListener(
        topics = "sensor-data", 
        groupId = "storage-service",
        containerFactory = "batchFactory"
    )
    public void storeData(List<String> dataJsonList) throws JsonProcessingException {
        List<SensorData> dataList = new ArrayList<>();
        
        for (String json : dataJsonList) {
            dataList.add(objectMapper.readValue(json, SensorData.class));
        }
        
        // Batch insert
        repository.saveAll(dataList);
        
        System.out.println("Stored " + dataList.size() + " sensor readings");
    }
}
```

---

## Advanced Topics

### 1. Custom Partitioning Strategy

```java
// Custom Partitioner
public class DeviceIdPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        if (key == null) {
            return 0;
        }
        
        String deviceId = (String) key;
        
        // Extract device number (e.g., "device-001" -> 1)
        int deviceNumber = Integer.parseInt(deviceId.split("-")[1]);
        
        // Partition based on device number
        return deviceNumber % numPartitions;
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}

// Configuration
@Bean
public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DeviceIdPartitioner.class);
    // ... other config
    return new DefaultKafkaProducerFactory<>(config);
}
```

### 2. Error Handling with Dead Letter Queue

```java
@Service
public class OrderConsumerWithDLQ {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ProcessedMessageRepository processedRepo;
    
    private static final int MAX_RETRIES = 3;
    
    @KafkaListener(topics = "orders", groupId = "order-processor")
    public void consume(String message, Acknowledgment ack, 
                       @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset) {
        
        String messageId = extractMessageId(message);
        
        try {
            // Check if already processed (idempotency)
            if (processedRepo.exists(messageId)) {
                System.out.println("Message already processed: " + messageId);
                ack.acknowledge();
                return;
            }
            
            // Process message
            processOrder(message);
            
            // Mark as processed
            processedRepo.save(new ProcessedMessage(
                messageId, 
                LocalDateTime.now(),
                partition,
                offset
            ));
            
            // Acknowledge successful processing
            ack.acknowledge();
            
        } catch (RetryableException e) {
            // Retryable error - don't acknowledge
            int retryCount = getRetryCount(messageId);
            
            if (retryCount >= MAX_RETRIES) {
                // Send to DLQ after max retries
                sendToDeadLetterQueue(message, e);
                ack.acknowledge();
            } else {
                incrementRetryCount(messageId);
                System.err.println("Retryable error (attempt " + retryCount + "): " + e.getMessage());
                // Don't acknowledge - Kafka will retry
            }
            
        } catch (NonRetryableException e) {
            // Non-retryable error - send to DLQ immediately
            sendToDeadLetterQueue(message, e);
            ack.acknowledge();
            
            System.err.println("Non-retryable error, sent to DLQ: " + e.getMessage());
        }
    }
    
    private void sendToDeadLetterQueue(String message, Exception e) {
        DeadLetterMessage dlqMessage = new DeadLetterMessage(
            message,
            e.getClass().getSimpleName(),
            e.getMessage(),
            LocalDateTime.now()
        );
        
        try {
            kafkaTemplate.send("orders-dlq", 
                objectMapper.writeValueAsString(dlqMessage)
            );
        } catch (Exception ex) {
            System.err.println("Failed to send to DLQ: " + ex.getMessage());
        }
    }
}

// Dead Letter Queue Consumer
@Service
public class DeadLetterQueueConsumer {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AlertService alertService;
    
    @KafkaListener(topics = "orders-dlq", groupId = "dlq-processor")
    public void processFailed(String dlqMessageJson) throws JsonProcessingException {
        DeadLetterMessage dlqMessage = objectMapper.readValue(dlqMessageJson, DeadLetterMessage.class);
        
        // Log to database
        System.err.println("DLQ Message: " + dlqMessage.getOriginalMessage());
        System.err.println("Error: " + dlqMessage.getErrorMessage());
        
        // Send alert to operations team
        alertService.sendAlert(
            "Dead Letter Queue Alert",
            "Failed to process message: " + dlqMessage.getErrorMessage()
        );
        
        // Store for manual review
        // dlqRepository.save(dlqMessage);
    }
}
```

### 3. Idempotent Producer

```java
@Configuration
public class IdempotentProducerConfig {
    
    @Bean
    public ProducerFactory<String, String> idempotentProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Idempotent producer configuration
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> idempotentKafkaTemplate() {
        return new KafkaTemplate<>(idempotentProducerFactory());
    }
}
```

### 4. Transactional Producer (Exactly-Once Semantics)

```java
@Configuration
public class TransactionalConfig {
    
    @Bean
    public ProducerFactory<String, String> transactionalProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Transactional configuration
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-id");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> transactionalKafkaTemplate() {
        return new KafkaTemplate<>(transactionalProducerFactory());
    }
}

@Service
public class TransactionalService {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Transactional
    public void processWithTransaction() {
        kafkaTemplate.executeInTransaction(template -> {
            template.send("topic1", "message1");
            template.send("topic2", "message2");
            
            // If any send fails, both are rolled back
            
            return true;
        });
    }
}
```

### 5. Message Filtering

```java
@Configuration
public class FilteringConfig {
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> filteringFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Add record filter
        factory.setRecordFilterStrategy(record -> {
            try {
                Order order = objectMapper.readValue(record.value(), Order.class);
                return order.getTotalAmount() < 1000;  // Filter out orders < 1000
            } catch (Exception e) {
                return true;  // Filter out invalid messages
            }
        });
        
        return factory;
    }
}

@Service
public class HighValueOrderConsumer {
    
    @KafkaListener(
        topics = "orders",
        groupId = "high-value-processor",
        containerFactory = "filteringFactory"
    )
    public void consumeHighValueOrders(String orderJson) {
        // Only orders >= 1000 reach here
        System.out.println("Processing high-value order: " + orderJson);
    }
}
```

---

## Performance Tuning

### Producer Optimization

```yaml
spring:
  kafka:
    producer:
      # Batching - group multiple messages
      batch-size: 16384  # 16KB
      linger-ms: 10  # Wait 10ms to batch messages
      
      # Compression - reduce network usage
      compression-type: snappy  # or gzip, lz4, zstd
      
      # Buffer - memory for batching
      buffer-memory: 33554432  # 32MB
      
      # Acknowledgment
      acks: 1  # Leader only (faster but less durable)
      # acks: all  # All replicas (slower but more durable)
      
      # Retries
      retries: 3
      retry-backoff-ms: 100
      
      # In-flight requests
      max-in-flight-requests-per-connection: 5
```

### Consumer Optimization

```yaml
spring:
  kafka:
    consumer:
      # Fetch configuration
      fetch-min-size: 1  # Minimum bytes to fetch
      fetch-max-wait-ms: 500  # Max wait time for min bytes
      max-partition-fetch-bytes: 1048576  # 1MB per partition
      
      # Poll configuration
      max-poll-records: 500  # Max records per poll
      max-poll-interval-ms: 300000  # 5 minutes
      
      # Session configuration
      session-timeout-ms: 30000  # 30 seconds
      heartbeat-interval-ms: 3000  # 3 seconds
      
      # Concurrency
    listener:
      concurrency: 3  # 3 consumer threads per listener
```

### Partition and Replication

```java
@Configuration
public class TopicConfig {
    
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")
            .partitions(10)  # More partitions = more parallelism
            .replicas(3)  # More replicas = more fault tolerance
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")  # 7 days
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "snappy")
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
            .build();
    }
}
```

---

## Monitoring and Metrics

### Spring Boot Actuator Integration

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Custom Metrics

```java
@Service
public class KafkaMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter messagesSent;
    private final Counter messagesConsumed;
    private final Timer producerTimer;
    private final Timer consumerTimer;
    
    public KafkaMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesSent = Counter.builder("kafka.producer.messages.sent")
            .description("Total messages sent")
            .register(meterRegistry);
        this.messagesConsumed = Counter.builder("kafka.consumer.messages.consumed")
            .description("Total messages consumed")
            .register(meterRegistry);
        this.producerTimer = Timer.builder("kafka.producer.send.time")
            .description("Producer send time")
            .register(meterRegistry);
        this.consumerTimer = Timer.builder("kafka.consumer.processing.time")
            .description("Consumer processing time")
            .register(meterRegistry);
    }
    
    public void recordMessageSent() {
        messagesSent.increment();
    }
    
    public void recordMessageConsumed() {
        messagesConsumed.increment();
    }
    
    public void recordSendTime(Runnable task) {
        producerTimer.record(task);
    }
    
    public void recordProcessingTime(Runnable task) {
        consumerTimer.record(task);
    }
}
```

### Important Metrics to Monitor

**Producer Metrics**:
- Messages sent per second
- Failed sends
- Average send latency
- Buffer utilization
- Request rate

**Consumer Metrics**:
- Messages consumed per second
- Consumer lag (how far behind)
- Processing time
- Rebalance frequency
- Commit rate

**Broker Metrics**:
- Request rate
- Disk usage
- Network throughput
- Under-replicated partitions
- Leader election rate

---

## Interview Questions

### Q1: What is Kafka and why use it?

**Answer**:
Kafka is a distributed event streaming platform for building real-time data pipelines.

**Why use it**:
- High throughput (millions of messages/sec)
- Scalable (horizontal scaling)
- Fault-tolerant (replication)
- Durable (persistent storage)
- Decouples services (async communication)

**Real Example**: "In our e-commerce, when order is placed, Kafka notifies email service, inventory service, and analytics simultaneously without blocking order creation."

### Q2: Explain partitions and why they're important

**Answer**:
Partitions divide a topic into smaller units for parallel processing.

**Benefits**:
1. **Scalability**: Multiple consumers read in parallel
2. **Ordering**: Messages in same partition are ordered
3. **Load Distribution**: Spread across brokers

**Example**: Topic with 3 partitions, 3 consumers each read one partition simultaneously.

### Q3: What is a consumer group?

**Answer**:
Consumer group is a set of consumers working together to consume a topic.

**Key Points**:
- Each partition assigned to only one consumer in a group
- Multiple groups can consume same topic
- Provides load balancing and fault tolerance

**Example**:
```
Topic: orders (3 partitions)
Group 1 (email-service): 3 consumers, each reads 1 partition
Group 2 (analytics): 1 consumer, reads all 3 partitions
```

### Q4: How does Kafka ensure message delivery?

**Answer**:
Three levels of guarantee:

1. **At-most-once** (may lose):
   - acks=0, auto-commit enabled
   - Fastest, least reliable

2. **At-least-once** (may duplicate):
   - acks=all, manual commit
   - Most common

3. **Exactly-once**:
   - Idempotent producer + transactions
   - Slowest, most reliable

### Q5: What happens if a consumer fails?

**Answer**:
1. Kafka detects failure (heartbeat timeout)
2. Triggers rebalancing
3. Failed consumer's partitions reassigned to others
4. New consumer resumes from last committed offset

**Example**: 3 consumers, 6 partitions. If 1 fails, remaining 2 get 3 partitions each.

### Q6: How to handle duplicate messages?

**Answer**:
Make consumer idempotent by tracking processed message IDs in database.

### Q7: Kafka vs RabbitMQ?

**Answer**:

**Kafka**:
- High throughput, persistent storage, pull-based
- Best for event streaming, logs, analytics

**RabbitMQ**:
- Complex routing, push-based
- Best for task queues, request-response

### Q8: How to ensure message ordering?

**Answer**:
1. Send messages with same key to same partition
2. Single consumer per partition
3. Process synchronously

### Q9: What is offset and how is it managed?

**Answer**:
Offset is unique ID for each message in a partition. Can be auto-committed or manually committed after processing.

### Q10: How to handle slow consumers?

**Answer**:
1. Increase partitions for more parallelism
2. Add more consumers (up to partition count)
3. Batch processing
4. Async processing
5. Optimize processing logic

---

## Common Pitfalls and Solutions

### Pitfall 1: Consumer Lag

**Problem**: Consumer falling behind producer

**Solutions**:
- Add more consumers
- Increase max.poll.records
- Optimize processing
- Use batch processing

### Pitfall 2: Message Loss

**Problem**: Messages not delivered

**Solutions**:
- Set acks=all
- Enable idempotent producer
- Use manual offset commit
- Implement retry logic

### Pitfall 3: Out of Order Messages

**Problem**: Messages processed in wrong order

**Solutions**:
- Use message keys
- Single consumer per partition
- Process synchronously

### Pitfall 4: Poison Pill Message

**Problem**: One bad message blocks queue

**Solutions**:
- Error handling
- Dead letter queue
- Max retry attempts
- Skip bad messages

---

## Summary

**Kafka in One Sentence**:
"Kafka is a distributed messaging system for publishing and subscribing to streams of records, providing high throughput, fault tolerance, and scalability for real-time data processing."

**When to Use Kafka**:
- Event-driven architecture
- Real-time analytics
- Log aggregation
- Microservices communication
- Data pipelines

**Key Takeaways**:
1. Topics organize messages
2. Partitions enable parallelism
3. Consumer groups provide scalability
4. Offsets track progress
5. Replication ensures durability
6. Spring Boot makes integration easy

**Production Checklist**:
- Replication configured (factor of 3)
- Monitoring setup (Prometheus/Grafana)
- Error handling (DLQ)
- Idempotent consumers
- Performance tuned
- Security enabled (SSL/SASL)
- Testing complete
- Documentation ready

**Next Steps**:
1. Setup local Kafka cluster
2. Build simple producer-consumer
3. Implement real use case
4. Add error handling
5. Monitor and optimize