# Saga Design Pattern Implementation - Travel Booking System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![H2 Database](https://img.shields.io/badge/Database-H2-blue.svg)](https://www.h2database.com/)

A comprehensive implementation of the **Saga Design Pattern** demonstrating both **Orchestration** and **Choreography** approaches for distributed transaction management in microservices architecture.

---

## 📋 Table of Contents

- [Overview](#overview)
- [What is Saga Pattern?](#what-is-saga-pattern)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Pattern Comparison](#pattern-comparison)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

This project demonstrates a real-world Travel Booking System that coordinates multiple services (Flight, Hotel, Car Rental) to create a complete travel package. It showcases how to maintain data consistency across distributed services without using traditional ACID transactions.

### Business Scenario

When a customer books a complete travel package:
1. **Book a flight** to the destination
2. **Reserve a hotel** for the stay
3. **Rent a car** for transportation

If any step fails, all previous bookings must be automatically cancelled to maintain system consistency.

---

## 🔍 What is Saga Pattern?

The Saga pattern is a microservices architectural pattern for managing distributed transactions. Instead of a single ACID transaction spanning multiple services, a saga breaks the transaction into a series of local transactions, with compensating transactions to undo changes if something fails.

### Key Concepts

- **Local Transactions**: Each service manages its own database transaction
- **Compensation**: Undo operations for rolling back completed steps
- **Eventual Consistency**: System reaches consistent state eventually, not immediately
- **No Distributed Locks**: Avoids two-phase commit and distributed locking

### When to Use Saga Pattern

✅ **Use When:**
- Building microservices with separate databases
- Need to maintain data consistency across services
- Long-running business processes
- High availability requirements
- Services owned by different teams

❌ **Avoid When:**
- Single monolithic application
- Services can share a database
- Need immediate consistency (ACID transactions)
- Simple CRUD operations

---

## 🏗️ Architecture

### Orchestration Pattern

```
┌─────────────────────────────────────────┐
│         Saga Orchestrator               │
│  (Central Coordinator)                  │
└───────────┬─────────────────────────────┘
            │
            ├──────> Flight Service  ──> ✅ Success
            │
            ├──────> Hotel Service   ──> ❌ Failed!
            │
            └──────> Compensation
                     └──> Cancel Flight ✅
```

**Flow:**
1. Orchestrator calls Flight Service → Success
2. Orchestrator calls Hotel Service → Fails
3. Orchestrator initiates compensation
4. Orchestrator cancels Flight booking
5. Returns failure response to client

### Choreography Pattern

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Flight    │      │    Hotel    │      │     Car     │
│   Service   │      │   Service   │      │   Service   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                     │
       │ BOOKING_INITIATED  │                     │
       │◄───────────────────┘                     │
       │                                           │
       │ FLIGHT_BOOKED                             │
       ├────────────────────>                      │
       │                    │                      │
       │                    │ HOTEL_BOOKED         │
       │                    ├─────────────────────>│
       │                    │                      │
       │                    │          ❌ CAR_FAILED
       │                    │◄─────────────────────┤
       │ COMPENSATE_FLIGHT  │                      │
       │◄───────────────────┤                      │
       │                    │                      │
```

**Flow:**
1. Initial event triggers Flight Service
2. Flight publishes FLIGHT_BOOKED event
3. Hotel hears event and books
4. Hotel publishes HOTEL_BOOKED event
5. Car hears event and attempts booking
6. Car fails and publishes CAR_FAILED
7. Compensation events cascade backward
8. Hotel and Flight cancel their bookings

---

## ✨ Features

### Core Capabilities

- ✅ **Dual Pattern Implementation**: Both Orchestration and Choreography
- ✅ **Automatic Compensation**: Rollback on failures
- ✅ **State Persistence**: H2 database for saga state tracking
- ✅ **Event Sourcing**: Complete audit trail of all events
- ✅ **Async Processing**: Non-blocking choreography with @Async
- ✅ **Failure Simulation**: Random failures for testing resilience
- ✅ **RESTful API**: Easy-to-use endpoints

### Advanced Features

- 🔄 **Idempotency Support**: Safe retry mechanisms
- 📊 **Database Console**: H2 web console for inspection
- 🎯 **Transaction Tracking**: Complete saga lifecycle management
- 📝 **Comprehensive Logging**: Detailed execution traces
- ⚡ **Performance Simulation**: Realistic service delays

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17+ | Programming Language |
| **Spring Boot** | 3.2.0 | Application Framework |
| **Spring Data JPA** | 3.2.0 | Database Access |
| **H2 Database** | 2.2.224 | In-Memory Database |
| **Lombok** | 1.18.30 | Boilerplate Reduction |
| **Maven** | 3.9+ | Build Tool |

---

## 📁 Project Structure

```
travel-saga-system/
│
├── src/main/java/com/travelsaga/
│   ├── TravelSagaApplication.java          # Main Application
│   │
│   ├── controller/
│   │   └── BookingController.java          # REST Endpoints
│   │
│   ├── dto/
│   │   ├── TravelBookingRequest.java       # Request DTO
│   │   └── TravelBookingResponse.java      # Response DTO
│   │
│   ├── entity/
│   │   ├── SagaStateEntity.java            # Saga State Entity
│   │   ├── BookingEvent.java               # Event Entity
│   │   ├── SagaStatus.java                 # Status Enum
│   │   └── SagaType.java                   # Type Enum
│   │
│   ├── repository/
│   │   ├── SagaStateRepository.java        # Saga State Repo
│   │   └── BookingEventRepository.java     # Event Repo
│   │
│   ├── saga/
│   │   ├── orchestration/
│   │   │   └── OrchestrationSaga.java      # Orchestration Logic
│   │   │
│   │   └── choreography/
│   │       ├── ChoreographySaga.java       # Choreography Initiator
│   │       ├── FlightServiceListener.java  # Flight Events
│   │       ├── HotelServiceListener.java   # Hotel Events
│   │       └── CarServiceListener.java     # Car Events
│   │
│   ├── service/
│   │   ├── FlightService.java              # Flight Business Logic
│   │   ├── HotelService.java               # Hotel Business Logic
│   │   └── CarRentalService.java           # Car Business Logic
│   │
│   └── event/
│       ├── EventPublisher.java             # Event Publishing
│       └── SagaEvent.java                  # Event Object
│
├── src/main/resources/
│   └── application.properties              # Configuration
│
├── pom.xml                                 # Maven Dependencies
└── README.md                               # Documentation
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.9+** or Gradle
- **IDE** (IntelliJ IDEA, Eclipse, VS Code)
- **Postman** or **cURL** for API testing

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/travel-saga-system.git
cd travel-saga-system
```

2. **Build the project**
```bash
mvn clean install
```

3. **Run the application**
```bash
mvn spring-boot:run
```

4. **Verify it's running**
```bash
curl http://localhost:8080/api/bookings/health
```

Expected Response:
```
Travel Saga Service Running - Both Patterns Available!
```

### Quick Start

**Test Orchestration Pattern:**
```bash
curl -X POST http://localhost:8080/api/bookings/travel \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "destination": "Paris",
    "checkInDate": "2024-12-20",
    "checkOutDate": "2024-12-27",
    "numberOfGuests": 2,
    "sagaType": "ORCHESTRATION"
  }'
```

**Test Choreography Pattern:**
```bash
curl -X POST http://localhost:8080/api/bookings/travel \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST002",
    "destination": "Tokyo",
    "checkInDate": "2024-12-25",
    "checkOutDate": "2025-01-05",
    "numberOfGuests": 3,
    "sagaType": "CHOREOGRAPHY"
  }'
```

---

## 📡 API Documentation

### Base URL
```
http://localhost:8080/api/bookings
```

### Endpoints

#### 1. Create Travel Booking

**Endpoint:** `POST /travel`

**Request Body:**
```json
{
  "customerId": "string",
  "destination": "string",
  "checkInDate": "2024-12-20",
  "checkOutDate": "2024-12-27",
  "numberOfGuests": 2,
  "sagaType": "ORCHESTRATION" | "CHOREOGRAPHY"
}
```

**Success Response (Orchestration):**
```json
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "message": "All bookings completed successfully via ORCHESTRATION!",
  "sagaType": "ORCHESTRATION"
}
```

**Success Response (Choreography):**
```json
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "INITIATED",
  "message": "Booking initiated via CHOREOGRAPHY. Services will process independently.",
  "sagaType": "CHOREOGRAPHY"
}
```

**Failure Response:**
```json
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440002",
  "status": "FAILED",
  "message": "Booking failed: No rooms available. Compensated via ORCHESTRATION.",
  "sagaType": "ORCHESTRATION"
}
```

#### 2. Health Check

**Endpoint:** `GET /health`

**Response:**
```
Travel Saga Service Running - Both Patterns Available!
```

---

## ⚖️ Pattern Comparison

| Aspect | Orchestration | Choreography |
|--------|---------------|--------------|
| **Control** | Centralized | Distributed |
| **Complexity** | Simple | Complex |
| **Coupling** | High (orchestrator knows all) | Low (event-driven) |
| **Response Time** | Synchronous | Asynchronous |
| **Debugging** | Easy (linear flow) | Difficult (trace events) |
| **Scalability** | Limited (single point) | High (independent) |
| **Testing** | Straightforward | Requires event simulation |
| **Monitoring** | Single point | Multiple points |
| **Use Case** | Simple workflows | Complex, independent services |

### Orchestration: Pros & Cons

**Advantages:**
- ✅ Simple to understand and implement
- ✅ Clear business process flow
- ✅ Easy to debug and trace
- ✅ Immediate response to client
- ✅ Centralized monitoring

**Disadvantages:**
- ❌ Single point of failure
- ❌ Tight coupling to all services
- ❌ Orchestrator becomes complex
- ❌ Harder to scale
- ❌ Changes require orchestrator update

### Choreography: Pros & Cons

**Advantages:**
- ✅ Highly decoupled services
- ✅ No single point of failure
- ✅ Easy to add new services
- ✅ Scales independently
- ✅ Resilient to service failures

**Disadvantages:**
- ❌ Complex to understand flow
- ❌ Difficult to debug
- ❌ No immediate client response
- ❌ Cyclic dependencies risk
- ❌ Requires robust event infrastructure

---

## 🗄️ Database Schema

### Tables

#### `saga_state`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| saga_id | VARCHAR | Unique saga identifier |
| customer_id | VARCHAR | Customer identifier |
| destination | VARCHAR | Travel destination |
| check_in_date | DATE | Check-in date |
| check_out_date | DATE | Check-out date |
| number_of_guests | INTEGER | Number of guests |
| flight_booking_id | VARCHAR | Flight booking reference |
| hotel_booking_id | VARCHAR | Hotel booking reference |
| car_rental_id | VARCHAR | Car rental reference |
| status | VARCHAR | PENDING, IN_PROGRESS, COMPLETED, FAILED, COMPENSATING, COMPENSATED |
| saga_type | VARCHAR | ORCHESTRATION or CHOREOGRAPHY |
| completed_steps | VARCHAR | Comma-separated completed steps |
| compensated_steps | VARCHAR | Comma-separated compensated steps |
| failure_reason | VARCHAR | Reason for failure |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

#### `booking_events`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| saga_id | VARCHAR | Reference to saga |
| event_type | VARCHAR | Type of event |
| event_data | VARCHAR | Event payload |
| timestamp | TIMESTAMP | Event timestamp |

### Accessing H2 Console

1. Navigate to: `http://localhost:8080/h2-console`
2. Use these credentials:
    - **JDBC URL:** `jdbc:h2:mem:sagadb`
    - **Username:** `sa`
    - **Password:** *(leave empty)*

### Sample Queries

```sql
-- View all sagas
SELECT * FROM saga_state ORDER BY created_at DESC;

-- View successful bookings
SELECT * FROM saga_state WHERE status = 'COMPLETED';

-- View failed bookings with compensation
SELECT saga_id, status, failure_reason, compensated_steps 
FROM saga_state 
WHERE status IN ('FAILED', 'COMPENSATED');

-- View event timeline for a specific saga
SELECT event_type, timestamp 
FROM booking_events 
WHERE saga_id = 'YOUR-SAGA-ID' 
ORDER BY timestamp ASC;

-- Count bookings by pattern
SELECT saga_type, status, COUNT(*) as count
FROM saga_state
GROUP BY saga_type, status;
```

---

## 🧪 Testing

### Manual Testing

Run multiple requests to see both success and failure scenarios:

```bash
# Run 10 bookings
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/bookings/travel \
    -H "Content-Type: application/json" \
    -d '{
      "customerId": "CUST'$i'",
      "destination": "Paris",
      "checkInDate": "2024-12-20",
      "checkOutDate": "2024-12-27",
      "numberOfGuests": 2,
      "sagaType": "ORCHESTRATION"
    }'
  echo ""
done
```

### Failure Simulation

The services have built-in random failures:
- **Flight Service**: 10% failure rate
- **Hotel Service**: 15% failure rate
- **Car Rental Service**: 12% failure rate

### Testing Scenarios

1. **Happy Path**: All services succeed
2. **Flight Failure**: First service fails, no compensation needed
3. **Hotel Failure**: Second service fails, compensate flight
4. **Car Failure**: Third service fails, compensate hotel and flight
5. **Concurrent Bookings**: Multiple simultaneous requests
6. **Pattern Comparison**: Same booking with both patterns

### Expected Behavior

**Orchestration Success:**
```
✅ Flight booked: FLT-abc123
✅ Hotel booked: HTL-def456
✅ Car rented: CAR-ghi789
✅ Saga completed successfully
```

**Orchestration Failure:**
```
✅ Flight booked: FLT-abc123
❌ Hotel booking failed: No rooms available
🔄 Compensating: Canceling flight FLT-abc123
✅ Compensation completed
```

**Choreography Success:**
```
📢 BOOKING_INITIATED
✈️ Flight booked: FLT-abc123
📢 FLIGHT_BOOKED
🏨 Hotel booked: HTL-def456
📢 HOTEL_BOOKED
🚗 Car rented: CAR-ghi789
📢 BOOKING_COMPLETED
```

---

## ⚙️ Configuration

### `application.properties`

```properties
# Application
spring.application.name=travel-saga-service
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:sagadb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Logging
logging.level.com.travelsaga=INFO
logging.level.org.springframework=WARN
```
