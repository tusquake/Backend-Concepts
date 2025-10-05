package com.example.oauth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/hello")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome! This is a public endpoint - no authentication required");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @GetMapping("/public/info")
    public Map<String, Object> publicInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "OAuth 2.0 Demo");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.of(
                "public", "/api/public/**",
                "user", "/api/user/** (requires 'read' scope)",
                "admin", "/api/admin/** (requires 'write' scope)"
        ));
        return response;
    }

    @GetMapping("/user/profile")
    public Map<String, Object> userProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User profile endpoint - requires authentication with 'read' scope");
        response.put("username", jwt.getSubject());
        response.put("scopes", jwt.getClaim("scope"));
        response.put("issuedAt", jwt.getIssuedAt());
        response.put("expiresAt", jwt.getExpiresAt());
        return response;
    }

    @GetMapping("/user/data")
    public Map<String, Object> userData(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", jwt.getSubject());
        response.put("email", jwt.getSubject() + "@example.com");
        response.put("role", "USER");
        response.put("data", "Some user-specific data");
        return response;
    }

    @GetMapping("/admin/dashboard")
    public Map<String, Object> adminDashboard(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin dashboard - requires 'write' scope");
        response.put("admin", jwt.getSubject());
        response.put("stats", Map.of(
                "totalUsers", 42,
                "activeUsers", 18,
                "serverStatus", "healthy"
        ));
        return response;
    }

    @PostMapping("/admin/action")
    public Map<String, Object> adminAction(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin action executed");
        response.put("executedBy", jwt.getSubject());
        response.put("action", request.get("action"));
        response.put("status", "success");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}