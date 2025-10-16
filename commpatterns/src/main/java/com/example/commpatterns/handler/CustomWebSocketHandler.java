package com.example.commpatterns.handler;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomWebSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    // Called when a client connects
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        String welcomeMessage = "Connected to WebSocket server. Session ID: " + session.getId();
        session.sendMessage(new TextMessage(welcomeMessage));
        System.out.println("WebSocket client connected: " + session.getId());

        // Broadcast to all clients
        broadcast("New client connected. Total clients: " + sessions.size());
    }

    // Called when client sends a message
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received from " + session.getId() + ": " + payload);

        // Handle special commands
        if (payload.equalsIgnoreCase("broadcast")) {
            broadcast("Broadcast message at " + LocalDateTime.now());
        } else if (payload.equalsIgnoreCase("count")) {
            session.sendMessage(new TextMessage("Connected clients: " + sessions.size()));
        } else {
            // Echo back to sender with timestamp
            String response = "Echo [" + LocalDateTime.now() + "]: " + payload;
            session.sendMessage(new TextMessage(response));
        }
    }

    // Called when a client disconnects
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket client disconnected: " + session.getId());
        broadcast("Client disconnected. Total clients: " + sessions.size());
    }

    // Broadcasts message to all connected clients
    public static void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting to session: " + e.getMessage());
            }
        }
    }

    // Get count of connected sessions
    public static int getSessionCount() {
        return sessions.size();
    }
}