package com.example.commpatterns.controller;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/short-polling")
public class ShortPollingController {

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger counter = new AtomicInteger(0);

    // Client calls this endpoint every few seconds
    // Returns current state immediately
    @GetMapping("/messages")
    public Map<String, Object> getMessages() {
        int count = counter.incrementAndGet();

        // Add a new message every 3 requests to simulate new data
        if (count % 3 == 0) {
            messages.add("Message " + count + " at " + LocalDateTime.now());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("messages", new ArrayList<>(messages));
        response.put("pollCount", count);
        response.put("note", "Keep polling this endpoint to see messages appear");

        return response;
    }

    // Manually trigger a new message for testing
    @PostMapping("/trigger")
    public Map<String, String> triggerMessage() {
        int count = counter.incrementAndGet();
        String message = "Manual message " + count + " at " + LocalDateTime.now();
        messages.add(message);

        Map<String, String> response = new HashMap<>();
        response.put("status", "Message added");
        response.put("message", message);
        return response;
    }

    // Reset endpoint for testing
    @PostMapping("/reset")
    public String reset() {
        messages.clear();
        counter.set(0);
        return "Short polling reset";
    }
}
