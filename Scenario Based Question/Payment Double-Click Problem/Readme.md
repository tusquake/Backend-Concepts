# Preventing Double Payments in Payment Systems

A professional reference design covering idempotency, concurrency
handling, database protection, gateway safety, and webhook
reconciliation.

------------------------------------------------------------------------

## 1. Problem Statement

Users may double-click the Pay button or the client may retry due to
network delays.\
The backend may receive multiple identical requests for the same logical
payment.\
The system must ensure that **a user is charged only once**, regardless
of how many duplicate requests arrive.

------------------------------------------------------------------------

## 2. Core Design Principles

### 2.1 Idempotency Key

The client must generate a unique **Idempotency-Key** for each intended
payment attempt.\
The server treats all requests with the same key as the same operation.

### 2.2 Database Uniqueness

A unique constraint on `idempotency_key` ensures only one record can be
created per logical payment attempt.

### 2.3 Transaction Safety

Insert a `PENDING` payment record before calling the payment gateway.\
Update this record to `SUCCESS` or `FAILED` after receiving the gateway
response.

### 2.4 Return Stored Response

If the same key is seen again, the system returns the previously stored
response and avoids reprocessing.

### 2.5 Concurrency Protection

Techniques: - Row-level locking (`SELECT … FOR UPDATE`) - Optimistic
locking (versioning) - Optional Redis distributed lock for multi-node
systems

### 2.6 Gateway-Level Idempotency

If the gateway supports idempotency headers, include the same key when
sending requests to avoid multiple charges on the gateway side.

### 2.7 Webhook Reconciliation

If the server crashes after gateway success, the gateway will send a
webhook.\
Webhook processing must: - Update the same payment record - Be
idempotent - Handle delayed or duplicate events

------------------------------------------------------------------------

## 3. End-to-End Flow

1.  Client sends:

        POST /payments
        Idempotency-Key: <uuid>

2.  Server checks if this key exists.

    -   If it exists → return stored result.

3.  If not found:

    -   Create a `PENDING` record (transactional).

4.  Call the external payment gateway (once).

5.  Update the same record to `SUCCESS` or `FAILED`.

6.  Duplicate requests with the same key return the stored response.

------------------------------------------------------------------------

## 4. Database Schema Example

``` sql
CREATE TABLE payments (
  id BIGSERIAL PRIMARY KEY,
  idempotency_key VARCHAR(255) NOT NULL UNIQUE,
  order_id VARCHAR(128) NOT NULL,
  amount NUMERIC(12,2) NOT NULL,
  status VARCHAR(16) NOT NULL,
  gateway_transaction_id VARCHAR(128),
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
```

------------------------------------------------------------------------

## 5. Handling Concurrency

### Row-Level Locking

Lock the order to avoid parallel processing:

    SELECT * FROM orders WHERE id = :orderId FOR UPDATE;

### Redis Distributed Lock (Optional)

Useful for multi-node systems:

    SETNX lock:payment:<idempotencyKey> <nodeId> EX 30

------------------------------------------------------------------------

## 6. Gateway and Webhook Integration

### Gateway Idempotency

Send the same idempotency key to the gateway:

    Idempotency-Key: <same-key>

### Webhook Handling

Webhook processor must: - Validate provider signature - Be fully
idempotent - Update payment record by `gateway_transaction_id` or
`idempotency_key`

------------------------------------------------------------------------

## 7. Testing Checklist

-   Two simultaneous requests → Only one DB record, one gateway charge.
-   Crash simulation after gateway success → Webhook resolves final
    state.
-   Retry with same key → Same result returned.
-   Retry with different key → Treated as new payment attempt.
-   Unique constraint violation handling.
-   Load-test concurrency behavior.

------------------------------------------------------------------------

## 8. Operational Considerations

-   Metrics: pending payments, reconciliation failures, idempotency
    conflicts.
-   Logging: log the idempotency key, order ID, request ID.
-   Monitoring: detect gateway double-charge attempts.
-   Cleanup: archive or clean old records periodically.

------------------------------------------------------------------------

## 9. Example curl Requests

### First Attempt

    curl -X POST https://api.example.com/payments   -H "Content-Type: application/json"   -H "Idempotency-Key: 1111-2222-3333"   -d '{"orderId":"O-1001","amount":500}'

### Duplicate Attempt

    curl -X POST https://api.example.com/payments   -H "Content-Type: application/json"   -H "Idempotency-Key: 1111-2222-3333"   -d '{"orderId":"O-1001","amount":500}'

------------------------------------------------------------------------

## 10. Summary

This design ensures: 
- One logical payment results in at most one
charge 
- Duplicate API calls are safely handled - Concurrency issues are
controlled 
- Gateway failures or server crashes are recoverable 
- Webhooks maintain consistency

This approach is widely used in production-grade payment systems such as
Swiggy, Amazon Pay, Stripe, Razorpay, and others.
