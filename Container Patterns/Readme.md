# 🐳 Container Patterns — A Complete Guide

> **Simple explanations, real-world analogies, and interview prep for the 5 essential Kubernetes/Docker container design patterns.**

---

## Table of Contents

1. [What Are Container Patterns?](#what-are-container-patterns)
2. [Sidecar Pattern](#1-sidecar-pattern)
3. [Adapter Pattern](#2-adapter-pattern)
4. [Ambassador Pattern](#3-ambassador-pattern)
5. [Work Queue Pattern](#4-work-queue-pattern)
6. [Init Pattern](#5-init-pattern)
7. [Quick Comparison Table](#quick-comparison-table)
8. [Common Interview Questions](#common-interview-questions)

---

## What Are Container Patterns?

When running applications in containers (like Docker/Kubernetes), you often need more than just one container to make an app work well. **Container patterns** are proven, reusable blueprints that describe *how to arrange multiple containers together* inside a Pod (in Kubernetes) to solve common infrastructure problems cleanly.

Think of them like architectural blueprints for a house — you don't invent how to build a kitchen from scratch every time; you follow a proven design.

---

## 1. Sidecar Pattern

### 🚗 Real-World Analogy
Imagine a **motorcycle with a sidecar**. The motorcycle (main app) does the core job — driving. The sidecar (helper container) rides alongside and provides an extra seat or luggage space without changing how the motorcycle works. You could detach the sidecar and the motorcycle still runs fine.

### What It Does
The Sidecar pattern attaches a **helper container** alongside the main application container *within the same Pod*. Both containers share the same storage volumes and network. The helper enhances or extends the main app without modifying its code.

### Common Use Cases
- **Log shipping** — A sidecar collects logs written by the main app and forwards them to a central logging system (e.g., Fluentd alongside your app)
- **Monitoring/metrics** — A Prometheus exporter sidecar scrapes metrics from the main app
- **Config reloading** — A sidecar watches for config file changes and reloads them automatically
- **TLS termination** — A sidecar handles SSL certificates so the main app doesn't have to

### How It Looks

```
+------------------------------- Pod ------------------------------+
|                                                                  |
|  [ Main App Container ]    [ Sidecar Container ]                 |
|  (your web server)         (log shipper / metrics agent)        |
|                                                                  |
|  Shared Volume: /var/logs                                        |
+------------------------------------------------------------------+
```

### Key Points
- Both containers run **simultaneously**
- They share **network (localhost)** and can share **volumes**
- The sidecar is **transparent** to the main app
- Promotes **separation of concerns**

---

## 2. Adapter Pattern

### 🔌 Real-World Analogy
Imagine you're traveling from India to the US. Your Indian charger plug doesn't fit the US socket. You use a **travel adapter** that converts the interface — your charger stays the same, the socket stays the same, only the adapter in between changes.

### What It Does
The Adapter pattern places a **translator container** next to the main container to **standardize the output** of the main app so it conforms to an expected interface or format — without modifying the main app itself.

### Common Use Cases
- **Metrics normalization** — Your app emits metrics in a custom format, but your monitoring system (Prometheus) expects a specific format. The adapter transforms the output.
- **Log format conversion** — Converting app logs from JSON to plain text (or vice versa) before forwarding
- **Protocol translation** — Main app speaks HTTP, but external system expects gRPC; the adapter bridges them

### How It Looks

```
+------------------------------- Pod ------------------------------+
|                                                                  |
|  [ Main App ]  --raw output-->  [ Adapter Container ]           |
|  (custom metrics)               (converts to Prometheus format) |
|                                        |                         |
|                                        v                         |
|                               External Monitoring System         |
+------------------------------------------------------------------+
```

### Key Points
- The main app remains **unchanged**
- The adapter **normalizes or translates** data
- External systems see a **consistent interface** regardless of the main app's internals
- Useful for **legacy apps** or **third-party apps** you can't modify

### Sidecar vs. Adapter
| | Sidecar | Adapter |
|---|---|---|
| Purpose | Adds functionality | Translates/normalizes output |
| Direction | Enhances the main app | Adapts the main app's interface |

---

## 3. Ambassador Pattern

### 🤝 Real-World Analogy
Think of an **embassy ambassador**. When a foreign country wants to communicate with another country, they go through the ambassador, who handles diplomatic protocol, translation, and routing. The ambassador acts as a **proxy** between two parties.

Similarly, when your app needs to talk to the outside world, the Ambassador container acts as an intelligent proxy.

### What It Does
The Ambassador pattern places a **proxy container** that handles all **outbound communication** on behalf of the main app. The main app always talks to `localhost`, and the ambassador figures out where to actually send the request.

### Common Use Cases
- **Service discovery** — Your app always connects to `localhost:5432`, and the ambassador routes to the right database pod dynamically
- **Connection pooling** — Ambassador maintains a pool of connections to the database; the main app doesn't need to manage this
- **Load balancing** — Ambassador distributes outbound requests across multiple backend services
- **Circuit breaking** — If a downstream service is failing, the ambassador stops routing requests there

### How It Looks

```
+------------------------------- Pod ------------------------------+
|                                                                  |
|  [ Main App ]  -->  localhost:5432  -->  [ Ambassador ]          |
|                                              |                   |
+----------------------------------------------|-------------------+
                                               |
                         Intelligent routing to the right service
                                               |
                              +----------------+-----------+
                              |                |           |
                        [ DB-Primary ]  [ DB-Replica ] [DB-Backup]
```

### Key Points
- Main app always talks to **localhost** (simple!)
- Ambassador handles **complexity of routing** in the outside world
- Decouples the app from **service discovery and networking logic**
- Think of it as an **outbound proxy**

### Adapter vs. Ambassador
| | Adapter | Ambassador |
|---|---|---|
| Direction | Translates **output** | Proxies **outbound requests** |
| Focus | Data format | Network communication |

---

## 4. Work Queue Pattern

### 🏭 Real-World Analogy
Imagine an **Amazon fulfillment warehouse**. Orders (tasks) come in and go into a queue. Workers (containers) pick up one order at a time, process it (pack it), mark it done, and grab the next one. If you have a rush of orders, you hire more workers (scale up containers). When it's slow, workers go home (scale down).

### What It Does
The Work Queue pattern uses a **shared queue** where jobs/tasks are placed. Multiple **worker containers** pull tasks from the queue, process them independently, and move on to the next task. This is perfect for **batch processing and parallelism**.

### Common Use Cases
- **Image/video processing** — A queue of uploaded videos to be transcoded; multiple worker pods process them in parallel
- **Email sending** — A queue of emails to be sent; workers pick and dispatch them
- **Report generation** — Large data reports queued and processed by workers
- **Machine learning inference** — A queue of prediction requests processed by multiple GPU worker pods

### How It Looks

```
            [ Task Producer ]
                   |
                   v
         +-------------------+
         |    Work Queue     |  (e.g., RabbitMQ, Redis, SQS)
         | [T1][T2][T3][T4]  |
         +-------------------+
              |    |    |
              v    v    v
         [W1]  [W2]  [W3]     ← Worker Pods (can scale independently)
```

### Key Points
- **Decouples** producers (who create tasks) from consumers (who process them)
- Workers are **stateless** — they just process and report results
- Easy to **scale horizontally** by adding more worker pods
- Built-in **fault tolerance** — if a worker crashes, the task goes back to the queue

---

## 5. Init Pattern

### 🏗️ Real-World Analogy
Think of **setting up a restaurant before opening time**. Before customers arrive (main app starts), you need to: stock the kitchen, set the tables, turn on the stoves, and check that the gas supply is working. These are all **one-time setup tasks**. Once done, the restaurant opens for business and these setup people leave.

The Init Pattern works exactly the same way — it runs setup containers that complete their job and exit *before* the main app starts.

### What It Does
**Init containers** are special containers in a Pod that run **to completion before the main app container starts**. They run in sequence (one after another), and only if all of them succeed does the main app container start.

### Common Use Cases
- **Database migration** — Run `db:migrate` before the web server starts
- **Dependency check** — Wait for a database or external service to be ready before starting the app
- **Config fetching** — Pull secrets from Vault or config from a remote source and write them to a shared volume
- **Permissions setup** — Set file ownership/permissions on a shared volume before the main app uses it
- **Cloning repos** — Pull the latest code or assets into a shared volume

### How It Looks

```
          Pod Startup Sequence:

  [Init Container 1] → runs, completes ✅
         ↓
  [Init Container 2] → runs, completes ✅
         ↓
  [Main App Container] → starts and runs indefinitely 🚀
```

### Key Points
- Init containers run **sequentially**, not in parallel
- They **must succeed** for the main container to start — if one fails, Kubernetes retries it
- They have **separate images** — you can use lightweight tools (like `busybox`) for init tasks
- They share **volumes** with the main container
- They are **temporary** — they exit after completing their task

---

## Quick Comparison Table

| Pattern | When to Use | Role | Runs Alongside Main? |
|---|---|---|---|
| **Sidecar** | You want to add functionality (logging, monitoring) | Helper/Enhancer | ✅ Yes, simultaneously |
| **Adapter** | You need to normalize app output for external systems | Translator | ✅ Yes, simultaneously |
| **Ambassador** | You want to simplify outbound network calls | Outbound Proxy | ✅ Yes, simultaneously |
| **Work Queue** | You have parallelizable batch jobs | Task Processor | N/A (separate workers) |
| **Init** | You need setup tasks done before app starts | Initializer | ❌ Runs before, then exits |

---

## Common Interview Questions

### Conceptual Questions

**Q1. What is the Sidecar pattern, and why would you use it instead of just adding functionality to the main container?**

> The Sidecar pattern keeps concerns separated. By putting logging, monitoring, or config management in a separate container, you keep the main app simple and focused. This also means you can update the sidecar independently, reuse it across different apps, and your main app doesn't need to be modified. It follows the Single Responsibility Principle.

---

**Q2. What's the difference between Sidecar and Adapter patterns?**

> Both run alongside the main container, but their purposes differ. A Sidecar *adds* functionality (like shipping logs). An Adapter *translates* the main container's output into a format that external systems can understand. The Adapter is essentially a specialized Sidecar focused on interface normalization.

---

**Q3. How is the Ambassador pattern different from a Service Mesh like Istio?**

> The Ambassador pattern is implemented at the Pod level as a container. A Service Mesh like Istio also injects a sidecar proxy (Envoy), but it operates at the cluster level with centralized control, mutual TLS, and observability across all services. The Ambassador pattern is simpler and scoped to a single Pod's outbound networking needs.

---

**Q4. When would you choose a Work Queue pattern over a simple REST API?**

> Use a Work Queue when tasks are time-consuming, need to be processed asynchronously, or when you need to handle bursts of traffic without overloading the system. For example, sending 10,000 emails — you don't want the API to wait for all of them to send. You queue them and let workers process them in the background.

---

**Q5. Can Init containers access secrets and ConfigMaps?**

> Yes. Init containers have the same access to secrets, ConfigMaps, and volumes as regular containers. This makes them useful for fetching credentials and writing them to shared volumes before the main app starts.

---

**Q6. What happens if an Init container fails?**

> Kubernetes will restart the Init container according to the Pod's `restartPolicy`. If the restartPolicy is `Always` or `OnFailure`, Kubernetes retries indefinitely. The main container will not start until all Init containers have completed successfully.

---

**Q7. Can you run multiple Sidecar containers in a single Pod?**

> Yes! A Pod can have multiple containers, and multiple of them can serve as sidecars. For example, you might have one sidecar for log shipping and another for metrics collection alongside your main app. All share the same network and can share volumes.

---

**Q8. How does the Work Queue pattern handle a worker crash mid-task?**

> This depends on the message queue system being used (e.g., RabbitMQ, SQS). Good queue systems use **acknowledgments**. A worker only acknowledges a task after it's done. If the worker crashes before acknowledging, the message becomes available again in the queue for another worker to pick up. This provides **at-least-once delivery** guarantees.

---

**Q9. Give a real-world scenario where you'd use the Adapter pattern.**

> Imagine you have a legacy Java application that emits custom metrics in its own proprietary format. Your organization uses Prometheus for monitoring, which requires metrics in a specific text format. Rather than rewriting the Java app, you add an Adapter sidecar that reads the custom metrics and exposes them in Prometheus format. Problem solved without touching legacy code.

---

**Q10. What's the difference between Init containers and regular containers with `lifecycle.postStart` hooks?**

> `postStart` hooks run immediately after a container starts, *in parallel* with the main process — they don't block the container from starting. Init containers, on the other hand, run *before* the main container and must complete successfully before the main container launches. For true prerequisite setup (like waiting for a DB to be ready), Init containers are the right tool.

---

## Summary

Container patterns are essential building blocks for building robust, maintainable, and scalable microservices. Here's the one-liner for each:

- **Sidecar** → "A helpful companion that runs alongside without interfering"
- **Adapter** → "A translator that makes your app speak the right language"
- **Ambassador** → "A smart proxy that handles the outside world for your app"
- **Work Queue** → "An assembly line that distributes and parallelizes tasks"
- **Init** → "A setup crew that prepares everything before the main team arrives"

---