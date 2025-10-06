# Spring Boot Caching Demo

A comprehensive demonstration of Spring Boot caching using both in-memory and Redis caching strategies with H2 database.

## Overview

This project demonstrates the use of Spring Boot's caching abstraction with the following annotations:
- `@EnableCaching` - Enables caching support
- `@Cacheable` - Caches method results
- `@CachePut` - Updates cache entries
- `@CacheEvict` - Removes entries from cache

**Supports two caching modes:**
1. **In-Memory Cache** (Default) - Uses ConcurrentHashMap
2. **Redis Cache** (Production-ready) - Distributed caching with Redis

## Technologies Used

- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database (In-Memory)
- Spring Cache Abstraction
- Redis (Optional - for distributed caching)
- Java 17

## Project Structure

```
src/main/java/com/example/demo/
├── CachingDemoApplication.java    # Main application
├── config/
│   └── CacheConfig.java           # Cache configuration (In-Memory & Redis)
├── entity/
│   └── Employee.java              # JPA Entity
├── repository/
│   └── EmployeeRepository.java    # JPA Repository
├── service/
│   └── EmployeeService.java       # Service with caching annotations
└── controller/
    ├── EmployeeController.java    # REST Controller
    └── CacheStatsController.java  # Cache monitoring endpoints

src/main/resources/
└── application.properties         # H2, JPA, and Redis configuration
```

## Setup Instructions

### Option 1: Run with In-Memory Cache (Default)

1. Clone the repository
2. Navigate to project directory
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
4. Application will start on `http://localhost:8080`

### Option 2: Run with Redis Cache

1. **Install and start Redis** (see REDIS-SETUP.md for detailed instructions)

   Quick start with Docker:
   ```bash
   docker run -d --name redis-cache -p 6379:6379 redis:latest
   ```

2. **Verify Redis is running:**
   ```bash
   docker exec -it redis-cache redis-cli ping
   ```
   Expected output: `PONG`

3. **Run application with Redis profile:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=redis
   ```

4. **Monitor Redis operations** (in another terminal):
   ```bash
   redis-cli MONITOR
   ```

## API Endpoints

### Employee Management

#### Get All Employees
```
GET http://localhost:8080/api/employees
```

#### Get Employee by ID (Cached)
```
GET http://localhost:8080/api/employees/{id}
```

#### Create Employee
```
POST http://localhost:8080/api/employees
Content-Type: application/json

{
  "name": "John Doe",
  "department": "IT",
  "salary": 75000.0
}
```

#### Update Employee (Updates Cache)
```
PUT http://localhost:8080/api/employees/{id}
Content-Type: application/json

{
  "name": "John Updated",
  "department": "IT",
  "salary": 80000.0
}
```

#### Delete Employee (Evicts from Cache)
```
DELETE http://localhost:8080/api/employees/{id}
```

#### Clear Entire Cache
```
DELETE http://localhost:8080/api/employees/cache/clear
```

### Cache Monitoring Endpoints

#### Get Cache Statistics
```
GET http://localhost:8080/api/cache/stats
```

Response:
```json
{
  "cacheNames": ["employees"],
  "cacheType": "RedisCacheManager",
  "provider": "Redis",
  "distributed": true,
  "cachedKeys": 3,
  "keysList": ["employees::1", "employees::2", "employees::3"]
}
```

#### Check if Specific Key is Cached
```
GET http://localhost:8080/api/cache/check?id=1
```

#### Get All Redis Keys (Redis profile only)
```
GET http://localhost:8080/api/cache/redis/keys
```

#### Get Cache Information
```
GET http://localhost:8080/api/cache/info
```

## Testing the Cache

### Test Cache Hit (In-Memory or Redis)

1. **First call - Slow (Database fetch):**
   ```bash
   curl http://localhost:8080/api/employees/1
   ```
   Expected: ~2 seconds

2. **Second call - Fast (Cache hit):**
   ```bash
   curl http://localhost:8080/api/employees/1
   ```
   Expected: <500ms

3. **Check cache stats:**
   ```bash
   curl http://localhost:8080/api/cache/stats
   ```

### Test Cache Eviction

1. **Clear cache:**
   ```bash
   curl -X DELETE http://localhost:8080/api/employees/cache/clear
   ```

2. **Fetch again - Slow (Cache was cleared):**
   ```bash
   curl http://localhost:8080/api/employees/1
   ```

### Test Cache Update

1. **Update employee:**
   ```bash
   curl -X PUT http://localhost:8080/api/employees/1 \
     -H "Content-Type: application/json" \
     -d '{"name":"John Updated","department":"IT","salary":90000.0}'
   ```

2. **Fetch immediately - Returns updated data from cache:**
   ```bash
   curl http://localhost:8080/api/employees/1
   ```

### Compare In-Memory vs Redis Performance

Run tests with both profiles and compare:

**In-Memory:**
```bash
mvn spring-boot:run
curl http://localhost:8080/api/employees/1  # First call: ~2000ms
curl http://localhost:8080/api/employees/1  # Second call: ~5ms
```

**Redis:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
curl http://localhost:8080/api/employees/1  # First call: ~2000ms
curl http://localhost:8080/api/employees/1  # Second call: ~10-20ms
```

## H2 Database Console

Access the H2 console to view database contents:

**URL:** `http://localhost:8080/h2-console`

**Login Credentials:**
- JDBC URL: `jdbc:h2:mem:employeedb`
- Username: `sa`
- Password: (leave empty)

### Sample Queries

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

## Redis CLI Commands

### View cached data in Redis

```bash
# View all keys
redis-cli KEYS *

# View employee cache keys
redis-cli KEYS employees*

# Get specific cached value
redis-cli GET "employees::1"

# Delete specific key
redis-cli DEL "employees::1"

# Clear all cache
redis-cli FLUSHALL

# Monitor Redis operations in real-time
redis-cli MONITOR
```

## How Caching Works

1. When `getEmployeeById()` is called, Spring AOP intercepts the call
2. CacheManager checks if data exists in cache with the given key
3. **Cache Hit:** Returns cached data immediately (fast)
4. **Cache Miss:** Executes method, fetches from database, stores in cache (slow)
5. Subsequent calls with same ID return cached data

### Cache Flow Diagram

```
Request → @Cacheable → Check Cache
                           ↓
                    Cache Hit? ─Yes→ Return from Cache (Fast)
                           ↓ No
                    Execute Method
                           ↓
                    Fetch from DB (Slow)
                           ↓
                    Store in Cache
                           ↓
                    Return Result
```

## Cache Configuration Comparison

| Feature | In-Memory Cache | Redis Cache |
|---------|----------------|-------------|
| **Speed** | Fastest (~5ms) | Very Fast (~10-20ms) |
| **Persistence** | Lost on restart | Can persist to disk |
| **Scalability** | Single server only | Multiple servers |
| **Memory** | Uses app heap | External process |
| **Distribution** | No | Yes |
| **TTL Support** | Limited | Full support |
| **Use Case** | Development, Single server | Production, Microservices |

## Sample Data

The application pre-loads 5 sample employees on startup:
1. John Doe - IT - $75,000
2. Jane Smith - HR - $65,000
3. Bob Johnson - Finance - $70,000
4. Alice Williams - IT - $80,000
5. Charlie Brown - Marketing - $68,000

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

## Performance Benefits

- Database queries reduced by ~99% for repeated requests
- In-Memory: 2000ms → 5ms (400x faster)
- Redis: 2000ms → 15ms (133x faster)
- Reduced database load and network overhead

## Memory Impact and Considerations

### Current Implementation

This project supports two caching strategies:

**In-Memory Cache:**
- Stores cached data in the application's heap memory
- Very fast but limited to single server
- Cache lost on application restart

**Redis Cache:**
- Stores cached data in external Redis server
- Slightly slower but distributed across servers
- Can persist data and survive restarts

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
Impact: Heavy - Can cause memory issues with in-memory cache
```

### Real-World Problems

1. **Memory Exhaustion** - Large in-memory caches can consume several GB of RAM
2. **Garbage Collection Pressure** - Large caches increase GC pauses
3. **Scalability** - In-memory cache not shared across multiple server instances

### Redis Advantages

Redis solves these problems by:
- Storing cache data outside application heap
- Supporting distributed caching across multiple servers
- Providing built-in TTL (Time To Live) management
- Offering persistence options

## Best Practices

### 1. Cache Only Frequently Accessed Data

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

### 2. Set Appropriate TTL (Time To Live)

In `CacheConfig.java`, Redis cache is configured with 10-minute TTL:

```java
RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(10))  // Cache expires after 10 minutes
    .disableCachingNullValues();
```

### 3. Use Redis for Production

**When to use In-Memory:**
- Development and testing
- Single server applications
- Small datasets (< 10K records)
- No need for cache persistence

**When to use Redis:**
- Production environments
- Microservices with multiple instances
- Large datasets (> 100K records)
- Need for cache persistence
- Distributed systems

### 4. Monitor Cache Performance

Use the cache monitoring endpoints:

```bash
# Check cache statistics
curl http://localhost:8080/api/cache/stats

# Check if specific key is cached
curl http://localhost:8080/api/cache/check?id=1

# Get all Redis keys (Redis profile only)
curl http://localhost:8080/api/cache/redis/keys
```

### 5. Implement Cache Eviction Strategies

The project includes cache eviction via:
- `@CacheEvict` on delete operations
- Manual cache clearing endpoint
- Redis TTL (automatic expiration)

## Configuration Options

### Redis Configuration

Modify `application.properties` to customize Redis:

```properties
# Redis host and port
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Redis password (if secured)
spring.data.redis.password=your_password

# Connection timeout
spring.data.redis.timeout=60000

# Connection pool settings
spring.data.redis.jedis.pool.max-active=8
spring.data.redis.jedis.pool.max-idle=8
spring.data.redis.jedis.pool.min-idle=0
```

### Cache TTL Configuration

Modify TTL in `CacheConfig.java`:

```java
// Change cache expiration time
.entryTtl(Duration.ofMinutes(30))  // 30 minutes
.entryTtl(Duration.ofHours(1))     // 1 hour
.entryTtl(Duration.ofDays(1))      // 1 day
```

### Custom Cache Names

Add multiple caches with different configurations:

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    
    // Short-lived cache for frequently changing data
    cacheConfigurations.put("employees", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)));
    
    // Long-lived cache for static data
    cacheConfigurations.put("departments", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24)));
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

## Cache Type Comparison

| Cache Type | Memory Impact | Speed | Scalability | Persistence | Use Case |
|------------|---------------|-------|-------------|-------------|----------|
| **In-Memory** | High (app heap) | Fastest (5ms) | Single server | No | Dev/Testing |
| **Redis** | Low (external) | Very Fast (15ms) | Multi-server | Yes | Production |
| **Memcached** | Low (external) | Fast (20ms) | Multi-server | No | High throughput |
| **Caffeine** | Medium (app heap) | Fastest (5ms) | Single server | No | Controlled growth |

## Switching Between Cache Types

### Run with In-Memory Cache
```bash
mvn spring-boot:run
```

### Run with Redis Cache
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

### Or set in application.properties
```properties
spring.profiles.active=redis
```

## Production Deployment Recommendations

### 1. Use Redis in Production

```bash
# Docker deployment
docker run -d \
  --name redis-prod \
  -p 6379:6379 \
  --restart unless-stopped \
  -v redis-data:/data \
  redis:latest redis-server --appendonly yes
```

### 2. Secure Redis

```properties
# Enable authentication
spring.data.redis.password=strong_password_here

# Use SSL/TLS
spring.data.redis.ssl=true
```

### 3. Monitor Cache Performance

```bash
# Redis info
redis-cli INFO stats

# Monitor cache hit/miss ratio
redis-cli INFO stats | grep keyspace
```

### 4. Set JVM Memory Limits

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -jar app.jar
```

### 5. Use Redis Cluster for High Availability

For production systems handling millions of requests:
- Redis Sentinel for automatic failover
- Redis Cluster for horizontal scaling
- Master-Slave replication

## Troubleshooting

### Redis Connection Issues

**Problem:** `Cannot connect to Redis at localhost:6379`

**Solution:**
1. Check if Redis is running:
   ```bash
   redis-cli ping
   ```
2. Verify port in application.properties
3. Check firewall settings

### Cache Not Working

**Problem:** Data always fetched from database

**Solution:**
1. Verify `@EnableCaching` is present in CacheConfig
2. Check cache profile is active
3. Look for errors in console logs
4. Verify cache key is correctly configured

### Memory Issues with In-Memory Cache

**Problem:** OutOfMemoryError

**Solution:**
1. Switch to Redis cache
2. Implement cache size limits
3. Add TTL to cache entries
4. Increase JVM heap size

### Redis Memory Full

**Problem:** Redis OOM (Out of Memory)

**Solution:**
1. Set maxmemory in Redis config
2. Configure eviction policy:
   ```bash
   redis-cli CONFIG SET maxmemory 256mb
   redis-cli CONFIG SET maxmemory-policy allkeys-lru
   ```

## Testing Guide

### Unit Testing Cache

```java
@SpringBootTest
class EmployeeServiceTest {
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    void testCaching() {
        // Clear cache
        cacheManager.getCache("employees").clear();
        
        // First call - should hit database
        long start1 = System.currentTimeMillis();
        Employee emp1 = employeeService.getEmployeeById(1L);
        long time1 = System.currentTimeMillis() - start1;
        
        // Second call - should hit cache
        long start2 = System.currentTimeMillis();
        Employee emp2 = employeeService.getEmployeeById(1L);
        long time2 = System.currentTimeMillis() - start2;
        
        // Cache hit should be much faster
        assertTrue(time2 < time1);
        assertEquals(emp1.getId(), emp2.getId());
    }
}
```

### Load Testing

Use tools like Apache JMeter or k6 to test cache performance:

```bash
# Install k6
brew install k6

# Run load test
k6 run load-test.js
```

## Additional Resources

- **Redis Setup:** See `REDIS-SETUP.md` for detailed Redis installation
- **Spring Cache Documentation:** https://spring.io/guides/gs/caching/
- **Redis Documentation:** https://redis.io/documentation
- **Spring Data Redis:** https://spring.io/projects/spring-data-redis

## Project Features Summary

✅ Dual cache support (In-Memory & Redis)  
✅ Profile-based configuration  
✅ Cache monitoring endpoints  
✅ Automatic cache eviction  
✅ TTL support with Redis  
✅ Distributed caching ready  
✅ H2 database with console  
✅ RESTful API  
✅ Performance logging  
✅ Production-ready configuration

## Notes

- H2 database is in-memory and data is lost on application restart
- In-memory cache is cleared on application restart
- Redis cache can persist data with appropriate configuration
- The 2-second delay in `getEmployeeById()` is simulated for demonstration
- Always monitor cache size and hit rates in production
- Redis is recommended for production environments

## License

This is a demo project for learning purposes.