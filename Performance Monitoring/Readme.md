# Complete Spring Boot Application Monitoring Guide

## Overview

This comprehensive guide covers various monitoring tools and techniques for Spring Boot applications, from basic health checks to advanced distributed tracing and APM solutions.

## Table of Contents

1. [Prometheus and Grafana](#prometheus-and-grafana)
2. [Spring Boot Actuator](#spring-boot-actuator)
3. [ELK Stack (Elasticsearch, Logstash, Kibana)](#elk-stack)
4. [Distributed Tracing](#distributed-tracing)
5. [Application Performance Monitoring (APM)](#application-performance-monitoring)
6. [Cloud-Native Monitoring](#cloud-native-monitoring)
7. [Database Monitoring](#database-monitoring)
8. [Custom Metrics](#custom-metrics)

---

## Prometheus and Grafana

### Overview
Prometheus collects time-series metrics data, while Grafana visualizes it through dashboards.

**Real-World Analogy**: Prometheus is like a restaurant inspector who continuously records kitchen performance data. Grafana is the management dashboard that displays this data in easy-to-understand charts and graphs.

### Setup

**Dependencies (pom.xml):**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Configuration (application.yml):**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

**Use Cases:**
- Real-time metrics monitoring
- System resource tracking
- Custom business metrics
- Alerting on threshold violations

---

## Spring Boot Actuator

### Overview
Built-in Spring Boot feature that provides production-ready monitoring and management endpoints.

**Real-World Analogy**: Like a car's dashboard that shows speed, fuel level, engine temperature, and warning lights - all essential information about the vehicle's current state.

### Setup

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configuration:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
      base-path: /actuator
  endpoint:
    health:
      show-details: always
```

### Key Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application information |
| `/actuator/metrics` | Available metrics list |
| `/actuator/env` | Environment properties |
| `/actuator/loggers` | Logger configuration |
| `/actuator/threaddump` | Thread dump |
| `/actuator/heapdump` | Heap dump |
| `/actuator/beans` | Spring beans list |

**Use Cases:**
- Quick health checks
- Runtime configuration changes
- Debugging production issues
- Integration with monitoring tools

---

## ELK Stack (Elasticsearch, Logstash, Kibana)

### Overview
Centralized logging solution for collecting, processing, storing, and visualizing application logs.

**Real-World Analogy**: Like a security camera system in a building - Logstash is the camera collecting footage, Elasticsearch is the storage system, and Kibana is the monitoring room where security staff review the recordings.

### Components

**Elasticsearch**: Stores and indexes logs
**Logstash**: Processes and transforms logs
**Kibana**: Visualizes logs and creates dashboards

### Setup

**Dependencies:**
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Logback Configuration (logback-spring.xml):**
```xml
<configuration>
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

**Use Cases:**
- Centralized log aggregation
- Log search and analysis
- Error tracking and debugging
- Audit trail maintenance
- Security monitoring

---

## Distributed Tracing

### Zipkin

**Overview**: Tracks requests across multiple microservices to identify performance bottlenecks.

**Real-World Analogy**: Like tracking a package through postal service - you can see exactly where it is at each stage, how long it stayed at each location, and where delays occurred.

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

**Configuration:**
```yaml
spring:
  zipkin:
    base-url: http://localhost:9411
  sleuth:
    sampler:
      probability: 1.0
```

### Jaeger

**Overview**: Alternative to Zipkin, originally developed by Uber for distributed tracing.

**Dependencies:**
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-spring-jaeger-web-starter</artifactId>
</dependency>
```

**Use Cases:**
- Microservices communication tracking
- Performance bottleneck identification
- Dependency mapping
- Latency analysis

---

## Application Performance Monitoring (APM)

### New Relic

**Overview**: Cloud-based APM solution providing comprehensive application insights.

**Dependencies:**
```xml
<dependency>
    <groupId>com.newrelic.agent.java</groupId>
    <artifactId>newrelic-api</artifactId>
</dependency>
```

**Features:**
- Real-user monitoring
- Transaction tracing
- Error analytics
- Infrastructure monitoring

### Dynatrace

**Overview**: AI-powered full-stack monitoring platform.

**Setup**: Agent-based installation
```bash
java -javaagent:/path/to/dynatrace/agent.jar -jar application.jar
```

### AppDynamics

**Overview**: Enterprise APM for monitoring application performance and user experience.

**Use Cases:**
- End-to-end transaction monitoring
- Code-level diagnostics
- Business transaction tracking
- Automatic baseline detection

---

## Cloud-Native Monitoring

### AWS CloudWatch

**Dependencies:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-cloudwatch2</artifactId>
</dependency>
```

**Configuration:**
```yaml
management:
  metrics:
    export:
      cloudwatch:
        namespace: SpringBootApp
        batch-size: 20
```

### Azure Application Insights

**Dependencies:**
```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>applicationinsights-spring-boot-starter</artifactId>
</dependency>
```

### Google Cloud Monitoring (Stackdriver)

**Dependencies:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-stackdriver</artifactId>
</dependency>
```

**Use Cases:**
- Cloud-native application monitoring
- Integration with cloud services
- Auto-scaling decisions
- Cost optimization

---

## Database Monitoring

### HikariCP Metrics

**Configuration:**
```yaml
spring:
  datasource:
    hikari:
      metrics-tracker-factory: com.zaxxer.hikari.metrics.micrometer.MicrosoftSqlServerMetricsTrackerFactory
```

### Database-Specific Tools

**MySQL**: MySQL Enterprise Monitor, Percona Monitoring
**PostgreSQL**: pgAdmin, pg_stat_statements
**MongoDB**: MongoDB Atlas, Ops Manager

**Key Metrics to Monitor:**
- Connection pool size and usage
- Query execution time
- Transaction rate
- Lock contention
- Cache hit ratio

---

## Custom Metrics

### Creating Custom Metrics

**Using Micrometer:**
```java
@Service
public class OrderService {
    private final Counter orderCounter;
    private final Timer orderTimer;
    
    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "online")
            .register(registry);
            
        this.orderTimer = Timer.builder("orders.processing.time")
            .description("Order processing time")
            .register(registry);
    }
    
    public void createOrder(Order order) {
        orderTimer.record(() -> {
            // Process order
            orderCounter.increment();
        });
    }
}
```

**Custom Gauge:**
```java
@Component
public class CustomMetrics {
    @Autowired
    public CustomMetrics(MeterRegistry registry, OrderRepository orderRepository) {
        Gauge.builder("orders.pending", orderRepository, OrderRepository::countPending)
            .description("Number of pending orders")
            .register(registry);
    }
}
```

---

## Comparison Matrix

| Tool | Type | Best For | Complexity | Cost |
|------|------|----------|------------|------|
| Actuator | Built-in | Basic monitoring | Low | Free |
| Prometheus + Grafana | Metrics | Time-series data | Medium | Free |
| ELK Stack | Logging | Log analysis | High | Free/Paid |
| Zipkin/Jaeger | Tracing | Microservices | Medium | Free |
| New Relic | APM | Full-stack monitoring | Low | Paid |
| Dynatrace | APM | Enterprise monitoring | Low | Paid |
| CloudWatch | Cloud | AWS applications | Medium | Paid |

---

## Monitoring Strategy

### Basic Setup (Small Applications)
1. Spring Boot Actuator for health checks
2. Application logs to files
3. Basic metrics collection

### Intermediate Setup (Medium Applications)
1. Actuator + Prometheus + Grafana
2. Centralized logging (ELK or cloud-native)
3. Custom business metrics
4. Database monitoring

### Advanced Setup (Large/Enterprise Applications)
1. Full APM solution (New Relic, Dynatrace)
2. Distributed tracing (Zipkin/Jaeger)
3. ELK Stack for log analysis
4. Prometheus + Grafana for metrics
5. Cloud-native monitoring integration
6. Custom dashboards and alerts
7. Business intelligence integration