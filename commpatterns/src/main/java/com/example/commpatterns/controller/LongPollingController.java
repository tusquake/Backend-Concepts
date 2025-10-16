package com.example.commpatterns.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/long-polling")
public class LongPollingController {

    private final List<DeferredResult<Map<String, Object>>> pendingRequests =
            new CopyOnWriteArrayList<>();
    private final List<String> eventQueue = new CopyOnWriteArrayList<>();

    // Client makes a request and server waits for new data
    // Timeout set to 10 seconds to prevent indefinite hanging
    @GetMapping("/events")
    public DeferredResult<Map<String, Object>> getEvents() {
        DeferredResult<Map<String, Object>> result = new DeferredResult<>(10000L);

        // If events are already available, return immediately
        if (!eventQueue.isEmpty()) {
            result.setResult(createResponse());
            return result;
        }

        // Otherwise, hold the request until event arrives or timeout
        pendingRequests.add(result);

        // Handle timeout - return empty response
        result.onTimeout(() -> {
            pendingRequests.remove(result);
            Map<String, Object> timeoutResponse = new HashMap<>();
            timeoutResponse.put("message", "No events within 10 seconds");
            timeoutResponse.put("timestamp", LocalDateTime.now());
            timeoutResponse.put("note", "Call /trigger to generate an event");
            result.setResult(timeoutResponse);
        });

        // Handle completion
        result.onCompletion(() -> pendingRequests.remove(result));

        return result;
    }

    // Trigger a new event manually for testing
    @PostMapping("/trigger")
    public Map<String, String> triggerEvent(@RequestBody(required = false) Map<String, String> payload) {
        String eventMessage = payload != null && payload.containsKey("message")
                ? payload.get("message")
                : "Manual event at " + LocalDateTime.now();

        eventQueue.add(eventMessage);
        notifyPendingRequests();

        Map<String, String> response = new HashMap<>();
        response.put("status", "Event triggered");
        response.put("event", eventMessage);
        response.put("notifiedClients", String.valueOf(pendingRequests.size()));
        return response;
    }

    // Get current status
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("pendingRequests", pendingRequests.size());
        status.put("queuedEvents", eventQueue.size());
        status.put("timestamp", LocalDateTime.now());
        return status;
    }

    // Notifies all waiting clients when new event arrives
    private void notifyPendingRequests() {
        if (pendingRequests.isEmpty()) {
            return;
        }

        Map<String, Object> response = createResponse();

        for (DeferredResult<Map<String, Object>> request : pendingRequests) {
            request.setResult(response);
        }
        pendingRequests.clear();
    }

    // Creates response with current events
    private Map<String, Object> createResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("events", new ArrayList<>(eventQueue));
        response.put("timestamp", LocalDateTime.now());
        eventQueue.clear();
        return response;
    }
}