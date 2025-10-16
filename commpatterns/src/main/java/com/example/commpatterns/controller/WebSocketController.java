package com.example.commpatterns.controller;

import com.example.commpatterns.handler.CustomWebSocketHandler;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/websocket")
public class WebSocketController {

    // Get WebSocket status
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connectedClients", CustomWebSocketHandler.getSessionCount());
        status.put("endpoint", "ws://localhost:8080/ws/messages");
        status.put("timestamp", LocalDateTime.now());
        status.put("instructions", "Connect using WebSocket client to ws://localhost:8080/ws/messages");
        return status;
    }

    // Trigger a broadcast to all WebSocket clients
    @PostMapping("/broadcast")
    public Map<String, String> broadcast(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isEmpty()) {
            message = "Broadcast at " + LocalDateTime.now();
        }

        CustomWebSocketHandler.broadcast(message);

        Map<String, String> response = new HashMap<>();
        response.put("status", "Broadcast sent");
        response.put("message", message);
        response.put("clients", String.valueOf(CustomWebSocketHandler.getSessionCount()));
        return response;
    }
}