package com.example.commpatterns.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/sse")
public class SSEController {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private int eventCounter = 0;

    // Client subscribes to receive server-sent events
    @GetMapping("/stream")
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        emitters.add(emitter);

        // Clean up handlers
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            System.out.println("SSE client completed. Remaining: " + emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            System.out.println("SSE client timeout. Remaining: " + emitters.size());
        });

        emitter.onError((e) -> {
            emitters.remove(emitter);
            System.out.println("SSE client error. Remaining: " + emitters.size());
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("Connected to SSE stream. Use /trigger to send events."));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        System.out.println("New SSE client connected. Total: " + emitters.size());
        return emitter;
    }

    // Manually trigger an event to all SSE clients
    @PostMapping("/trigger")
    public Map<String, Object> triggerEvent(@RequestBody(required = false) Map<String, String> request) {
        eventCounter++;

        String message = request != null && request.containsKey("message")
                ? request.get("message")
                : "SSE Event " + eventCounter + " at " + LocalDateTime.now();

        int sent = sendEventToAll(message);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Event sent");
        response.put("message", message);
        response.put("clientsSent", sent);
        response.put("eventNumber", eventCounter);
        return response;
    }

    // Get current SSE status
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connectedClients", emitters.size());
        status.put("endpoint", "GET /api/sse/stream");
        status.put("eventsSent", eventCounter);
        status.put("timestamp", LocalDateTime.now());
        return status;
    }

    // Sends event to all subscribed clients
    private int sendEventToAll(String message) {
        int successCount = 0;
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
                successCount++;
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        // Remove dead emitters
        emitters.removeAll(deadEmitters);

        return successCount;
    }
}