package com.example.circuitbreaker.controller;

import com.example.circuitbreaker.service.ExternalService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerController {

    private final ExternalService externalService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Endpoint to call external service with circuit breaker protection
     */
    @GetMapping("/call")
    public ResponseEntity<Map<String, Object>> callExternalService(
            @RequestParam(defaultValue = "test-data") String data) {

        log.info("Received request to call external service");

        try {
            String response = externalService.callExternalAPI(data);
            return ResponseEntity.ok(createResponse(response, null));
        } catch (Exception e) {
            log.error("Error calling external service", e);
            return ResponseEntity.status(503)
                    .body(createResponse(null, e.getMessage()));
        }
    }

    /**
     * Get current circuit breaker state and metrics
     */
    @GetMapping("/circuit-breaker/state")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerState() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("externalService");

        Map<String, Object> state = new HashMap<>();
        state.put("state", cb.getState().name());
        state.put("metrics", getMetrics(cb));
        state.put("config", getConfig(cb));

        return ResponseEntity.ok(state);
    }

    /**
     * Get detailed circuit breaker metrics
     */
    @GetMapping("/circuit-breaker/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("externalService");
        return ResponseEntity.ok(getMetrics(cb));
    }

    /**
     * Control endpoints for testing different scenarios
     */
    @PostMapping("/simulate/enable-failures")
    public ResponseEntity<String> enableFailures() {
        externalService.enableFailureSimulation();
        return ResponseEntity.ok("Failure simulation enabled. Circuit breaker will open after threshold.");
    }

    @PostMapping("/simulate/disable-failures")
    public ResponseEntity<String> disableFailures() {
        externalService.disableFailureSimulation();
        return ResponseEntity.ok("Failure simulation disabled. Service will work normally.");
    }

    @PostMapping("/simulate/enable-slow-responses")
    public ResponseEntity<String> enableSlowResponses() {
        externalService.enableSlowResponseSimulation();
        return ResponseEntity.ok("Slow response simulation enabled.");
    }

    @PostMapping("/simulate/disable-slow-responses")
    public ResponseEntity<String> disableSlowResponses() {
        externalService.disableSlowResponseSimulation();
        return ResponseEntity.ok("Slow response simulation disabled.");
    }

    /**
     * Manually transition circuit breaker states (for learning)
     */
    @PostMapping("/circuit-breaker/transition/{state}")
    public ResponseEntity<String> transitionState(@PathVariable String state) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("externalService");

        switch (state.toUpperCase()) {
            case "OPEN":
                cb.transitionToOpenState();
                return ResponseEntity.ok("Circuit breaker transitioned to OPEN state");
            case "CLOSED":
                cb.transitionToClosedState();
                return ResponseEntity.ok("Circuit breaker transitioned to CLOSED state");
            case "HALF_OPEN":
                cb.transitionToHalfOpenState();
                return ResponseEntity.ok("Circuit breaker transitioned to HALF_OPEN state");
            case "DISABLED":
                cb.transitionToDisabledState();
                return ResponseEntity.ok("Circuit breaker DISABLED");
            case "FORCED_OPEN":
                cb.transitionToForcedOpenState();
                return ResponseEntity.ok("Circuit breaker FORCED OPEN");
            default:
                return ResponseEntity.badRequest()
                        .body("Invalid state. Use: OPEN, CLOSED, HALF_OPEN, DISABLED, or FORCED_OPEN");
        }
    }

    /**
     * Reset circuit breaker and counters
     */
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<String> reset() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("externalService");
        cb.reset();
        externalService.resetCallCount();
        return ResponseEntity.ok("Circuit breaker and counters reset");
    }

    // Helper methods
    private Map<String, Object> createResponse(String result, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("error", error);
        response.put("callCount", externalService.getCallCount());

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("externalService");
        response.put("circuitBreakerState", cb.getState().name());

        return response;
    }

    private Map<String, Object> getMetrics(CircuitBreaker cb) {
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
        metricsMap.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
        metricsMap.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        metricsMap.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
        metricsMap.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        metricsMap.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());

        return metricsMap;
    }

    private Map<String, Object> getConfig(CircuitBreaker cb) {
        var config = cb.getCircuitBreakerConfig();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("slidingWindowType", config.getSlidingWindowType().name());
        configMap.put("slidingWindowSize", config.getSlidingWindowSize());
        configMap.put("minimumNumberOfCalls", config.getMinimumNumberOfCalls());
        configMap.put("failureRateThreshold", config.getFailureRateThreshold() + "%");
        configMap.put("slowCallRateThreshold", config.getSlowCallRateThreshold() + "%");
        configMap.put("slowCallDurationThreshold", config.getSlowCallDurationThreshold().toMillis() + "ms");
        configMap.put("waitDurationInOpenState", config.getWaitIntervalFunctionInOpenState().apply(1) + "ms");
        configMap.put("permittedCallsInHalfOpenState", config.getPermittedNumberOfCallsInHalfOpenState());

        return configMap;
    }
}