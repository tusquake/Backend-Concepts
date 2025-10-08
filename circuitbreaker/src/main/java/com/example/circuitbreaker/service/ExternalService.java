package com.example.circuitbreaker.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ExternalService {

    private final Random random = new Random();
    private final AtomicInteger callCounter = new AtomicInteger(0);

    // Simulate different failure scenarios
    private boolean simulateFailure = false;
    private boolean simulateSlowResponse = false;

    /**
     * This method is protected by Circuit Breaker
     * - Uses "externalService" circuit breaker instance from config
     * - Falls back to fallbackMethod when circuit is OPEN or call fails
     */
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallbackResponse")
    public String callExternalAPI(String requestData) {
        int callNumber = callCounter.incrementAndGet();
        log.info("Call #{} - Attempting to call external API with data: {}", callNumber, requestData);

        // Simulate slow response
        if (simulateSlowResponse) {
            try {
                Thread.sleep(3000); // Exceeds slowCallDurationThreshold
                log.warn("Call #{} - Slow response detected", callNumber);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Simulate random failures
        if (simulateFailure || random.nextDouble() < 0.3) {
            log.error("Call #{} - External API call failed!", callNumber);
            throw new RuntimeException("External service is unavailable");
        }

        log.info("Call #{} - External API call successful", callNumber);
        return "Success: Response from external API for " + requestData;
    }

    /**
     * Fallback method called when circuit breaker is OPEN
     * Must have same signature as original method plus optional Throwable parameter
     */
    private String fallbackResponse(String requestData, Throwable t) {
        log.warn("Fallback triggered for request: {} - Reason: {}",
                requestData, t.getMessage());
        return "Fallback: Service temporarily unavailable. Please try again later.";
    }

    /**
     * Method without circuit breaker for comparison
     */
    public String callWithoutCircuitBreaker(String requestData) {
        log.info("Calling without circuit breaker protection");
        if (random.nextDouble() < 0.5) {
            throw new RuntimeException("Service failed!");
        }
        return "Success without circuit breaker";
    }

    // Control methods for testing
    public void enableFailureSimulation() {
        this.simulateFailure = true;
        log.info("Failure simulation ENABLED");
    }

    public void disableFailureSimulation() {
        this.simulateFailure = false;
        log.info("Failure simulation DISABLED");
    }

    public void enableSlowResponseSimulation() {
        this.simulateSlowResponse = true;
        log.info("Slow response simulation ENABLED");
    }

    public void disableSlowResponseSimulation() {
        this.simulateSlowResponse = false;
        log.info("Slow response simulation DISABLED");
    }

    public int getCallCount() {
        return callCounter.get();
    }

    public void resetCallCount() {
        callCounter.set(0);
        log.info("Call counter reset");
    }
}