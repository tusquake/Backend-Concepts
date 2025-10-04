# Spring Boot Caching Demo

A simple demonstration of Spring Boot caching using H2 in-memory database.

## Overview

This project demonstrates the use of Spring Boot's caching abstraction with the following annotations:
- `@EnableCaching` - Enables caching support
- `@Cacheable` - Caches method results
- `@CachePut` - Updates cache entries
- `@CacheEvict` - Removes entries from cache

## Technologies Used

- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (In-Memory)
- Spring Cache Abstraction
- Java 17

## Project Structure

```
src/main/java/com/example/demo/
├── CachingDemoApplication.java    # Main application with @EnableCaching
├── entity/
│   └── Employee.java              # JPA Entity
├── repository/
│   └── EmployeeRepository.java    # JPA Repository
├── service/
│   └── EmployeeService.java       # Service with caching annotations
└── controller/
    └── EmployeeController.java    # REST Controller

src/main/resources/
└── application.properties         # H2 and JPA configuration
```

## Setup Instructions

1. Clone the repository
2. Navigate to project directory
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
4. Application will start on `http://localhost:8080`

## API Endpoints

### Get All Employees
```
GET http://localhost:8080/api/employees
```

### Get Employee by ID (Cached)
```
GET http://localhost:8080/api/employees/{id}
```

### Create Employee
```
POST http://localhost:8080/api/employees
Content-Type: application/json

{
  "name": "John Doe",
  "department": "IT",
  "salary": 75000.0
}
```

### Update Employee (Updates Cache)
```
PUT http://localhost:8080/api/employees/{id}
Content-Type: application/json

{
  "name": "John Updated",
  "department": "IT",
  "salary": 80000.0
}
```

### Delete Employee (Evicts from Cache)
```
DELETE http://localhost:8080/api/employees/{id}
```

### Clear Entire Cache
```
DELETE http://localhost:8080/api/employees/cache/clear
```

## Testing the Cache

### Test Cache Hit
1. Call GET `/api/employees/1` - First call takes ~2 seconds (database fetch)
2. Call GET `/api/employees/1` again - Second call takes <500ms (cache hit)
3. Check console output to see performance difference

### Test Cache Eviction
1. Call DELETE `/api/employees/cache/clear`
2. Call GET `/api/employees/1` - Will be slow again (cache was cleared)

### Test Cache Update
1. Call PUT `/api/employees/1` with updated data
2. Call GET `/api/employees/1` - Returns updated data from cache instantly

## H2 Database Console

Access the H2 console to view database contents:

**URL:** `http://localhost:8080/h2-console`

**Login Credentials:**
- JDBC URL: `jdbc:h2:mem:employeedb`
- Username: `sa`
- Password: (leave empty)

## Sample Queries

```sql
-- View all employees
SELECT * FROM EMPLOYEES;

-- Search by name
SELECT * FROM EMPLOYEES WHERE NAME LIKE 'J%';

-- Filter by department
SELECT * FROM EMPLOYEES WHERE DEPARTMENT = 'IT';

-- Count employees
SELECT COUNT(*) FROM EMPLOYEES;
```

## How Caching Works

1. When `getEmployeeById()` is called, Spring AOP intercepts the call
2. CacheManager checks if data exists in cache with the given key
3. **Cache Hit:** Returns cached data immediately (fast)
4. **Cache Miss:** Executes method, fetches from database, stores in cache (slow)
5. Subsequent calls with same ID return cached data

## Cache Configuration

The application uses Spring's default `ConcurrentMapCacheManager` which creates an in-memory cache using `ConcurrentHashMap`.

Cache name: `employees`  
Cache key: Employee ID

## Console Output

The application logs show caching behavior:

```
Starting request for employee ID: 1
Fetching employee from H2 Database for ID: 1
Request completed in: 2015ms
SLOW! Data fetched from DATABASE

Starting request for employee ID: 1
Request completed in: 8ms
FAST! Data served from CACHE
```

## Sample Data

The application pre-loads 5 sample employees on startup:
1. John Doe - IT - $75,000
2. Jane Smith - HR - $65,000
3. Bob Johnson - Finance - $70,000
4. Alice Williams - IT - $80,000
5. Charlie Brown - Marketing - $68,000

## Performance Benefits

- Database queries reduced by ~99% for repeated requests
- Response time improved from 2000ms to <10ms
- Reduced database load and network overhead

## Memory Impact and Considerations

### Current Implementation

This project uses **in-memory caching** which stores cached data in the application's heap memory.

**Pros:**
- Very fast access (microseconds)
- No network latency
- Simple to set up
- Good for single-server applications

**Cons:**
- Consumes application heap memory
- Cache lost on application restart
- Not shared across multiple instances
- Can cause OutOfMemoryError if cache grows too large

### When Does Cache Become Heavy?

#### Small Dataset (Current Demo)
```
5 employees × ~200 bytes = ~1 KB
Impact: Negligible - No problem at all
```

#### Medium Dataset
```
10,000 employees × ~200 bytes = ~2 MB
Impact: Still manageable - Acceptable for most applications
```

#### Large Dataset
```
1,000,000 employees × ~200 bytes = ~200 MB
10,000,000 employees × ~200 bytes = ~2 GB
Impact: Heavy - Can cause memory issues
```

### Real-World Problems

1. **Memory Exhaustion** - Large caches can consume several GB of RAM
2. **Garbage Collection Pressure** - Large caches increase GC pauses
3. **Not Scalable** - Each server instance maintains its own cache

### Best Practices

#### 1. Cache Only Frequently Accessed Data

**Good Practice:**
```java
// Cache individual records accessed often
@Cacheable("employees")
public Employee getEmployeeById(Long id)
```

**Bad Practice:**
```java
// Don't cache large collections
@Cacheable("employees")
public List<Employee> getAllEmployees()
```

#### 2. Set Cache Size Limits

Use Caffeine for better cache management:

**Add to pom.xml:**
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

**Create CacheConfig.java:**
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("employees");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)              // Max 1000 entries
                .expireAfterWrite(10, TimeUnit.MINUTES)  // Expire after 10 min
                .recordStats());                 // Track cache statistics
        return cacheManager;
    }
}
```

#### 3. Use External Cache for Production

For production applications with multiple servers, use Redis:

**Add to pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Add to application.properties:**
```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

**Benefits:**
- Shared across all application instances
- Doesn't consume application memory
- Persistent (survives restarts)
- Can handle TBs of data

#### 4. Monitor Cache Performance

```java
@Scheduled(fixedRate = 60000) // Every minute
public void logCacheStats() {
    CacheManager cacheManager = ...; // inject
    Cache cache = cacheManager.getCache("employees");

    if (cache instanceof CaffeineCache) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                        ((CaffeineCache) cache).getNativeCache();

        CacheStats stats = nativeCache.stats();
        System.out.println("Cache hit rate: " + stats.hitRate());
        System.out.println("Cache size: " + nativeCache.estimatedSize());
    }
}
```

### Cache Type Comparison

| Cache Type | Memory Impact | Speed | Scalability | Use Case |
|------------|---------------|-------|-------------|----------|
| **In-Memory (Current)** | High | Fastest | Single server only | Small datasets, dev/testing |
| **In-Memory + Caffeine** | Medium | Very Fast | Single server only | Medium datasets, controlled growth |
| **Redis** | Low (on app) | Fast | Multi-server | Production, large datasets |
| **Memcached** | Low (on app) | Fast | Multi-server | High-throughput scenarios |

### Recommendations by Scale

**Development/Demo (Current):**
- Use default in-memory cache
- Perfect for learning and testing

**Small Production (< 10K records):**
- Add Caffeine with size limits
- Set appropriate TTL (Time To Live)

**Large Production (> 100K records):**
- Use Redis for distributed caching
- Implement cache eviction policies
- Monitor memory usage

**Example for Medium Production:**
```java
Caffeine.newBuilder()
    .maximumSize(5000)           // Cache only 5000 most accessed
    .expireAfterWrite(1, TimeUnit.HOURS)
    .recordStats()
```
This would use only ~1MB of memory while still providing excellent performance.

### JVM Memory Configuration

For production, configure JVM memory appropriately:

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -jar app.jar
```

- `-Xms512m` - Initial heap size
- `-Xmx2g` - Maximum heap size
- `-XX:+UseG1GC` - Use G1 Garbage Collector (better for large heaps)

## Notes

- H2 database is in-memory and data is lost on application restart
- Cache is also in-memory and cleared on application restart
- For production, consider using Redis or another distributed cache
- The 2-second delay in `getEmployeeById()` is simulated for demonstration
- Always monitor cache size and hit rates in production
- Implement cache eviction policies to prevent memory issues