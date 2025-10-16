package com.example.commpatterns.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WebhookService {

    private final List<String> registeredCallbacks = new CopyOnWriteArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private int eventCounter = 0;

    // Registers a new callback URL
    public void registerCallback(String callbackUrl) {
        if (!registeredCallbacks.contains(callbackUrl)) {
            registeredCallbacks.add(callbackUrl);
        }
    }

    // Removes a callback URL
    public void unregisterCallback(String callbackUrl) {
        registeredCallbacks.remove(callbackUrl);
    }

    // Returns all registered callbacks
    public List<String> getCallbacks() {
        return new ArrayList<>(registeredCallbacks);
    }

    // Manually trigger webhooks for testing
    public Map<String, Object> triggerWebhooks(String customEvent) {
        eventCounter++;
        String event = customEvent != null ? customEvent : "Webhook Event " + eventCounter;

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("eventId", eventCounter);

        List<Map<String, String>> results = new ArrayList<>();

        for (String callback : registeredCallbacks) {
            Map<String, String> result = new HashMap<>();
            result.put("url", callback);

            try {
                restTemplate.postForObject(callback, payload, String.class);
                result.put("status", "success");
            } catch (Exception e) {
                result.put("status", "failed");
                result.put("error", e.getMessage());
            }
            results.add(result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("event", event);
        response.put("callbacksSent", results.size());
        response.put("results", results);

        return response;
    }
}