package com.example.commpatterns.controller;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // Simple health check endpoint
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("message", "All communication patterns are ready");
        return health;
    }

    // List all available endpoints
    @GetMapping("/endpoints")
    public Map<String, Object> listEndpoints() {
        Map<String, Object> endpoints = new HashMap<>();

        Map<String, String> shortPolling = new HashMap<>();
        shortPolling.put("GET /api/short-polling/messages", "Poll for messages");
        shortPolling.put("POST /api/short-polling/trigger", "Add a message");
        shortPolling.put("POST /api/short-polling/reset", "Reset messages");

        Map<String, String> longPolling = new HashMap<>();
        longPolling.put("GET /api/long-polling/events", "Wait for events (10s timeout)");
        longPolling.put("POST /api/long-polling/trigger", "Trigger an event");
        longPolling.put("GET /api/long-polling/status", "Check status");

        Map<String, String> webhook = new HashMap<>();
        webhook.put("POST /api/webhook/register", "Register callback URL");
        webhook.put("GET /api/webhook/list", "List registered webhooks");
        webhook.put("POST /api/webhook/trigger", "Trigger webhooks");
        webhook.put("GET /api/webhook/received", "View received webhooks");

        Map<String, String> websocket = new HashMap<>();
        websocket.put("WS /ws/messages", "WebSocket endpoint");
        websocket.put("GET /api/websocket/status", "WebSocket status");
        websocket.put("POST /api/websocket/broadcast", "Broadcast to clients");

        Map<String, String> sse = new HashMap<>();
        sse.put("GET /api/sse/stream", "Subscribe to SSE stream");
        sse.put("POST /api/sse/trigger", "Send SSE event");
        sse.put("GET /api/sse/status", "SSE status");

        endpoints.put("shortPolling", shortPolling);
        endpoints.put("longPolling", longPolling);
        endpoints.put("webhook", webhook);
        endpoints.put("websocket", websocket);
        endpoints.put("sse", sse);

        return endpoints;
    }
}