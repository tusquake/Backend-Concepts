# Communication Patterns Demo Project

A beginner-friendly Spring Boot application that demonstrates 5 different ways clients and servers can communicate with each other.

---

## Table of Contents
- [What This Project Does](#what-this-project-does)
- [Real-World Analogies](#real-world-analogies)
- [Setup Instructions](#setup-instructions)
- [Testing Each Pattern](#testing-each-pattern)
- [When to Use Each Pattern](#when-to-use-each-pattern)
- [Common Issues and Solutions](#common-issues-and-solutions)

---

## What This Project Does

Imagine you're waiting for a pizza delivery. There are different ways you can find out when your pizza arrives:

1. **Short Polling** - You call the pizza shop every 5 minutes asking "Is my pizza ready?"
2. **Long Polling** - You call the pizza shop and stay on the line until your pizza is ready
3. **Webhook** - You give the pizza shop your phone number, and they call you when it's ready
4. **WebSocket** - You keep an open phone line with the pizza shop the entire time
5. **SSE (Server-Sent Events)** - The pizza shop texts you updates automatically

This project implements all 5 patterns so you can see how they work!

---

## Real-World Analogies

### 1. Short Polling - "Are we there yet?"

**How it works:** The client repeatedly asks the server "Do you have new data?" every few seconds.

**Real-world example:**
- Checking your mailbox multiple times a day to see if mail arrived
- Refreshing your email inbox manually every few minutes
- A child asking "Are we there yet?" every 5 minutes on a road trip

**Pros:**
- Very simple to implement
- Works with any server

**Cons:**
- Wastes bandwidth (many requests with no new data)
- Not real-time (delay between checks)
- Expensive if you have many clients

**When to use:**
- When updates happen infrequently
- When real-time updates aren't critical
- Example: Checking weather updates every 10 minutes

---

### 2. Long Polling - "I'll hold the line"

**How it works:** The client asks for data, and the server holds the request open until new data arrives or a timeout occurs.

**Real-world example:**
- Calling customer support and waiting on hold until an agent is available
- Waiting at a restaurant for your table to be ready
- Standing in line at the DMV until your number is called

**Pros:**
- More efficient than short polling
- Near real-time updates
- Works with standard HTTP

**Cons:**
- Server needs to manage many open connections
- More complex than short polling
- Connection can timeout

**When to use:**
- When you need near real-time updates
- When WebSocket is not available
- Example: Chat applications, live notifications

---

### 3. Webhook - "Don't call us, we'll call you"

**How it works:** You tell the server your callback URL. When something happens, the server sends data to your URL.

**Real-world example:**
- Giving the pizza shop your phone number so they can call you
- Setting up automatic alerts on your phone
- A smoke alarm that calls the fire department automatically
- Package delivery notifications sent to your phone

**Pros:**
- Very efficient (server only sends data when needed)
- No polling required
- Scales well

**Cons:**
- Your callback URL must be publicly accessible
- Need to handle failed deliveries
- Security considerations (verifying webhook source)

**When to use:**
- When the server needs to notify you of events
- When you can't maintain a persistent connection
- Example: Payment confirmations, GitHub notifications, subscription renewals

---

### 4. WebSocket - "Keep the line open"

**How it works:** Both client and server maintain an open, two-way connection. Either side can send messages anytime.

**Real-world example:**
- A phone call where both people can talk anytime
- A walkie-talkie conversation
- A video call with continuous back-and-forth
- Two people texting in real-time

**Pros:**
- True real-time, bidirectional communication
- Low latency
- Server can push data anytime

**Cons:**
- Uses resources to keep connection open
- More complex to implement
- Not all proxies/firewalls support it

**When to use:**
- When you need bidirectional real-time communication
- When latency is critical
- Example: Live chat, multiplayer games, collaborative editing, stock tickers

---

### 5. SSE (Server-Sent Events) - "I'll text you updates"

**How it works:** The client opens a connection, and the server sends automatic updates over time. One-way communication from server to client.

**Real-world example:**
- Getting automatic text alerts from your bank
- Weather alerts sent to your phone
- News notifications from an app
- Sports score updates

**Pros:**
- Automatic reconnection built-in
- Simple to implement
- Works over standard HTTP

**Cons:**
- One-way only (server to client)
- Limited browser support for older browsers
- Connection limits in some browsers

**When to use:**
- When server needs to push updates to client
- When you don't need client-to-server messages
- Example: Live feeds, stock prices, social media feeds, live sports scores

---

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven
- Postman (for testing)
- A web browser (for WebSocket and SSE testing)

### Step 1: Clone or Download the Project
```bash
git clone <your-repo-url>
cd commpatterns
```

### Step 2: Build the Project
```bash
mvn clean install
```

### Step 3: Run the Application
```bash
mvn spring-boot:run
```

Or run the main class directly from your IDE:
```
CommunicationPatternsApplication.java
```

### Step 4: Verify It's Running
Open your browser or Postman and go to:
```
http://localhost:8080/api/test/health
```

You should see:
```json
{
  "status": "UP",
  "timestamp": "2025-10-16T...",
  "message": "All communication patterns are ready"
}
```

---

## Testing Each Pattern

### Pattern 1: Short Polling

**Step 1:** Make your first request
```
GET http://localhost:8080/api/short-polling/messages
```

**Expected Response:**
```json
{
  "timestamp": "2025-10-16T...",
  "messages": [],
  "pollCount": 1,
  "note": "Keep polling this endpoint to see messages appear"
}
```

**Step 2:** Keep calling the same endpoint (click Send again)
Every 3rd request, a new message appears!

**Step 3:** Manually trigger a message anytime:
```
POST http://localhost:8080/api/short-polling/trigger
```

**Real-world comparison:** This is like checking your email inbox by clicking refresh repeatedly. Sometimes there's new mail, sometimes there isn't.

---

### Pattern 2: Long Polling

**Setup:** Open TWO tabs in Postman

**Tab 1 - The Waiter:**
```
GET http://localhost:8080/api/long-polling/events
```
This request will wait (up to 10 seconds) for an event to happen. Don't cancel it!

**Tab 2 - The Trigger:**
```
POST http://localhost:8080/api/long-polling/trigger
Body (raw JSON):
{
  "message": "Pizza is ready!"
}
```

**What happens:** As soon as you send the POST request in Tab 2, Tab 1 immediately receives the event!

**Real-world comparison:** This is like calling customer support and waiting on hold. You stay connected until someone is available to help you.

---

### Pattern 3: Webhook

**Scenario:** Imagine you run an online store and want to be notified when someone makes a payment.

**Step 1:** Register your callback URL (where you want to receive notifications)
```
POST http://localhost:8080/api/webhook/register
Body (raw JSON):
{
  "callbackUrl": "http://localhost:8080/api/webhook/callback"
}
```

**Response:**
```json
{
  "message": "Webhook registered successfully",
  "callbackUrl": "http://localhost:8080/api/webhook/callback",
  "note": "Use /trigger to send webhook to this URL"
}
```

**Step 2:** Simulate an event (like a payment being completed)
```
POST http://localhost:8080/api/webhook/trigger
Body (raw JSON):
{
  "event": "Payment of $99.99 completed"
}
```

**Step 3:** Check what webhooks you received
```
GET http://localhost:8080/api/webhook/received
```

**Response:**
```json
{
  "webhooks": [
    {
      "event": "Payment of $99.99 completed",
      "timestamp": "2025-10-16T...",
      "eventId": 1,
      "receivedAt": "2025-10-16T..."
    }
  ],
  "count": 1
}
```

**Real-world comparison:** This is like giving your phone number to a restaurant for takeout. They call you when your order is ready instead of you having to keep calling them.

---

### Pattern 4: WebSocket

**Note:** WebSocket requires a WebSocket client. Postman doesn't handle WebSocket well, so use your browser's JavaScript console.

**Step 1:** Open your browser console (F12) and paste this code:
```javascript
// Connect to WebSocket server
const ws = new WebSocket('ws://localhost:8080/ws/messages');

// When connection opens
ws.onopen = () => {
  console.log('Connected to WebSocket!');
};

// When receiving messages from server
ws.onmessage = (event) => {
  console.log('Server says:', event.data);
};

// When connection closes
ws.onclose = () => {
  console.log('Disconnected from WebSocket');
};
```

**Step 2:** Send a message to the server
```javascript
ws.send('Hello from browser!');
```

You'll see the server echo back your message!

**Step 3:** Try special commands
```javascript
ws.send('broadcast');  // Server sends message to all connected clients
ws.send('count');      // Get number of connected clients
```

**Step 4:** Use Postman to trigger a broadcast to all WebSocket clients
```
POST http://localhost:8080/api/websocket/broadcast
Body (raw JSON):
{
  "message": "Emergency alert: Server restarting in 5 minutes!"
}
```

All connected WebSocket clients will receive this message instantly!

**Real-world comparison:** This is like a group phone call where everyone can talk and listen at the same time.

---

### Pattern 5: Server-Sent Events (SSE)

**Note:** SSE works best with curl or browser EventSource API.

**Option A - Using Curl (Terminal/Command Prompt):**
```bash
curl http://localhost:8080/api/sse/stream
```

Leave this running! It will stay connected and show you messages as they arrive.

**Option B - Using Browser Console:**
```javascript
// Subscribe to SSE stream
const eventSource = new EventSource('http://localhost:8080/api/sse/stream');

// Handle connection event
eventSource.addEventListener('connection', (event) => {
  console.log('Connected:', event.data);
});

// Handle message events
eventSource.addEventListener('message', (event) => {
  console.log('Received:', event.data);
});

// Handle errors
eventSource.onerror = (error) => {
  console.error('SSE Error:', error);
};
```

**Trigger events from Postman:**
```
POST http://localhost:8080/api/sse/trigger
Body (raw JSON):
{
  "message": "Breaking news: New feature released!"
}
```

Your curl or browser console will immediately show the message!

**Check status:**
```
GET http://localhost:8080/api/sse/status
```

**Real-world comparison:** This is like subscribing to text alerts from your favorite store. They send you updates whenever there's a sale, and you don't have to do anything.

---

## When to Use Each Pattern

| Pattern | Use When | Example Use Cases |
|---------|----------|-------------------|
| **Short Polling** | Updates are infrequent and real-time isn't critical | Weather apps, checking order status |
| **Long Polling** | Need near real-time but can't use WebSocket | Chat apps (fallback), notifications |
| **Webhook** | Server needs to notify external systems | Payment confirmations, CI/CD pipelines, third-party integrations |
| **WebSocket** | Need bidirectional real-time communication | Live chat, multiplayer games, collaborative editing |
| **SSE** | Server needs to push updates one-way | Live feeds, stock tickers, notifications, social media updates |


---

## Understanding the Code Structure

```
src/main/java/com/example/commpatterns/
├── CommunicationPatternsApplication.java  (Main entry point)
├── controller/
│   ├── ShortPollingController.java        (Handles short polling requests)
│   ├── LongPollingController.java         (Handles long polling requests)
│   ├── WebhookController.java             (Manages webhook registration)
│   ├── WebSocketController.java           (WebSocket utilities)
│   ├── SSEController.java                 (Server-sent events)
│   └── TestController.java                (Testing utilities)
├── service/
│   └── WebhookService.java                (Webhook logic)
├── config/
│   └── WebSocketConfig.java               (WebSocket configuration)
└── handler/
    └── CustomWebSocketHandler.java        (WebSocket message handling)
```

---

## Quick Reference - All Endpoints

```
Health Check:
GET  /api/test/health
GET  /api/test/endpoints

Short Polling:
GET  /api/short-polling/messages
POST /api/short-polling/trigger
POST /api/short-polling/reset

Long Polling:
GET  /api/long-polling/events
POST /api/long-polling/trigger
GET  /api/long-polling/status

Webhook:
POST /api/webhook/register
GET  /api/webhook/list
POST /api/webhook/trigger
GET  /api/webhook/received
POST /api/webhook/clear

WebSocket:
WS   /ws/messages
GET  /api/websocket/status
POST /api/websocket/broadcast

SSE:
GET  /api/sse/stream
POST /api/sse/trigger
GET  /api/sse/status
```
