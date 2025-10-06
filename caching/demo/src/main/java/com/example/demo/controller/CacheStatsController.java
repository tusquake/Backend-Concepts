package com.example.demo.controller;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/cache")
public class CacheStatsController {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheStatsController(CacheManager cacheManager,
                                Optional<RedisTemplate<String, Object>> redisTemplate) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate.orElse(null);
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        Collection<String> cacheNames = cacheManager.getCacheNames();
        stats.put("cacheNames", cacheNames);
        stats.put("cacheType", cacheManager.getClass().getSimpleName());

        // If Redis is being used
        if (cacheManager.getClass().getSimpleName().contains("Redis")) {
            stats.put("provider", "Redis");
            stats.put("distributed", true);

            if (redisTemplate != null) {
                try {
                    // Get all keys matching employees pattern
                    Set<String> keys = redisTemplate.keys("employees*");
                    stats.put("cachedKeys", keys != null ? keys.size() : 0);
                    stats.put("keysList", keys);
                } catch (Exception e) {
                    stats.put("error", "Could not retrieve Redis keys: " + e.getMessage());
                }
            }
        } else {
            stats.put("provider", "In-Memory (ConcurrentHashMap)");
            stats.put("distributed", false);
        }

        return stats;
    }

    /**
     * Check if a specific key exists in cache
     */
    @GetMapping("/check")
    public Map<String, Object> checkCache(Long id) {
        Map<String, Object> result = new HashMap<>();

        Cache cache = cacheManager.getCache("employees");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(id);
            result.put("exists", wrapper != null);
            result.put("key", id);
            result.put("value", wrapper != null ? wrapper.get() : null);
        } else {
            result.put("error", "Cache 'employees' not found");
        }

        return result;
    }

    /**
     * Get all Redis keys (only works with Redis profile)
     */
    @GetMapping("/redis/keys")
    @Profile("redis")
    public Map<String, Object> getAllRedisKeys() {
        Map<String, Object> result = new HashMap<>();

        if (redisTemplate != null) {
            try {
                Set<String> allKeys = redisTemplate.keys("*");
                result.put("totalKeys", allKeys != null ? allKeys.size() : 0);
                result.put("keys", allKeys);

                Set<String> employeeKeys = redisTemplate.keys("employees*");
                result.put("employeeCacheKeys", employeeKeys != null ? employeeKeys.size() : 0);
                result.put("employeeKeys", employeeKeys);
            } catch (Exception e) {
                result.put("error", e.getMessage());
            }
        } else {
            result.put("error", "Redis is not configured");
        }

        return result;
    }

    /**
     * Get cache information
     */
    @GetMapping("/info")
    public Map<String, Object> getCacheInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("cacheManager", cacheManager.getClass().getName());
        info.put("cacheNames", cacheManager.getCacheNames());

        Cache employeeCache = cacheManager.getCache("employees");
        if (employeeCache != null) {
            info.put("employeeCacheClass", employeeCache.getClass().getName());
            info.put("nativeCache", employeeCache.getNativeCache().getClass().getName());
        }

        return info;
    }
}