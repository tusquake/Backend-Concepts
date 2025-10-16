package com.example.commpatterns.controller;

import com.example.commpatterns.service.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    @Autowired
    private WebhookService webhookService;

    private final List<Map<String, Object>> receivedWebhooks =
            Collections.synchronizedList(new ArrayList<>());

    // Register a callback URL to receive webhook notifications
    @PostMapping("/register")
    public Map<String, Object> registerWebhook(@RequestBody Map<String, String> request) {
        String callbackUrl = request.get("callbackUrl");
        webhookService.registerCallback(callbackUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Webhook registered successfully");
        response.put("callbackUrl", callbackUrl);
        response.put("note", "Use /trigger to send webhook to this URL");
        return response;
    }

    // Unregister a callback URL
    @DeleteMapping("/unregister")
    public Map<String, String> unregisterWebhook(@RequestBody Map<String, String> request) {
        String callbackUrl = request.get("callbackUrl");
        webhookService.unregisterCallback(callbackUrl);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Webhook unregistered successfully");
        return response;
    }

    // List all registered webhooks
    @GetMapping("/list")
    public Map<String, Object> listWebhooks() {
        Map<String, Object> response = new HashMap<>();
        response.put("callbacks", webhookService.getCallbacks());
        response.put("count", webhookService.getCallbacks().size());
        return response;
    }

    // Manually trigger webhooks for testing
    @PostMapping("/trigger")
    public Map<String, Object> triggerWebhooks(@RequestBody(required = false) Map<String, String> request) {
        String customEvent = request != null ? request.get("event") : null;
        return webhookService.triggerWebhooks(customEvent);
    }

    // Example endpoint that receives webhook callbacks for testing
    @PostMapping("/callback")
    public Map<String, Object> receiveCallback(@RequestBody Map<String, Object> payload) {
        payload.put("receivedAt", LocalDateTime.now());
        receivedWebhooks.add(payload);

        System.out.println("Received webhook: " + payload);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "received");
        response.put("payload", payload);
        return response;
    }

    // View all received webhooks
    @GetMapping("/received")
    public Map<String, Object> getReceivedWebhooks() {
        Map<String, Object> response = new HashMap<>();
        response.put("webhooks", new ArrayList<>(receivedWebhooks));
        response.put("count", receivedWebhooks.size());
        return response;
    }

    // Clear received webhooks
    @PostMapping("/clear")
    public Map<String, String> clearReceived() {
        receivedWebhooks.clear();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cleared all received webhooks");
        return response;
    }
}