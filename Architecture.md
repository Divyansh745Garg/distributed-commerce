# Architecture & Engineering Decisions

> **⚠️ Work in Progress**

This document describes the architectural decisions, distributed system patterns, and engineering rationale behind the Distributed E-Commerce Backend.

Unlike the project README, this document focuses on **why** the system is designed the way it is rather than explaining how to build or run it.

The project is currently under active development. While the core checkout workflow and distributed communication model have been implemented, several production-oriented capabilities are still being refined.

[//]: # (### Current Areas of Development)

[//]: # ()
[//]: # (- Improved Saga compensation workflow)

[//]: # (- Transactional Outbox reliability)

[//]: # (- Prometheus & Grafana monitoring dashboards)

[//]: # (- Zipkin distributed tracing)

[//]: # (- OpenTelemetry instrumentation)

[//]: # (- Dead Letter Queue &#40;DLQ&#41; support)

[//]: # (- Resilience4j circuit breakers)

[//]: # (- Kubernetes deployment strategy)

The architectural decisions documented here represent the intended production design and will continue to evolve as additional capabilities are implemented.

---

# Overview

Modern e-commerce systems are significantly more complex than traditional CRUD applications. A single checkout request typically spans multiple independent services responsible for authentication, inventory management, payment processing, notifications, and persistence.

These services must coordinate their work without relying on shared databases or distributed database transactions. At the same time, the platform must remain resilient to partial failures, duplicate client requests, network interruptions, and downstream service outages.

This project explores those engineering challenges by implementing a production-inspired distributed checkout platform using independently deployable Spring Boot microservices.

Rather than optimizing solely for functionality, the system prioritizes architectural concerns commonly encountered in enterprise backend systems:

- Loose coupling between services
- Independent ownership of business domains
- Event-driven communication
- High concurrency
- Failure recovery
- Horizontal scalability
- Eventual consistency
- Production-inspired observability

Many of the design patterns implemented throughout the project—including Saga Choreography, Redis-backed idempotency, API Gateway security, and the Transactional Outbox pattern—are commonly adopted in modern cloud-native applications where distributed consistency is more important than centralized control.

---

# Architectural Goals

The primary objective of this project is not to build a feature-rich shopping application, but to explore architectural patterns used in scalable distributed systems.

The platform is intentionally designed around the following engineering goals.

## 1. Service Isolation

Each microservice owns a single business capability and is responsible for managing its own data and business logic.

Services do not access one another's databases directly.

Instead, communication occurs through well-defined APIs or asynchronous domain events.

This isolation improves maintainability while allowing services to evolve independently.

---

## 2. Failure Isolation

Failures are expected rather than treated as exceptional situations.

If a downstream service becomes unavailable, the remaining components should continue operating wherever possible without bringing down the entire platform.

This philosophy influences every major architectural decision throughout the system.

---

## 3. Event-Driven Collaboration

Long-running business workflows should avoid synchronous dependencies whenever immediate consistency is unnecessary.

Instead of tightly coupling services together through direct HTTP calls, the platform publishes domain events that interested services consume independently.

This significantly reduces coupling while improving scalability.

---

## 4. Eventual Consistency

Traditional ACID transactions cannot safely span multiple independent databases.

Rather than attempting distributed transactions using two-phase commit (2PC), the platform embraces eventual consistency through asynchronous event propagation and compensating actions.

This approach provides significantly better scalability while remaining resilient to service failures.

---

## 5. Horizontal Scalability

Services are designed to remain stateless whenever possible.

Application state is externalized into dedicated infrastructure components such as PostgreSQL and Redis, allowing additional service replicas to be deployed without changing application logic.

---

## 6. Production-Oriented Design

Although this project is intended for learning and portfolio purposes, many implementation decisions intentionally mirror production systems.

Examples include:

- Database-per-Service architecture
- API Gateway security perimeter
- Redis-backed idempotency
- JWT authentication
- RabbitMQ messaging
- Saga-based distributed transactions
- Transactional Outbox
- Virtual Threads
- Distributed tracing
- Metrics collection

The objective is to understand the trade-offs involved in building reliable distributed backend systems rather than simply demonstrating framework usage.

---

# High-Level Architecture

<p align="center">
    <img src="./image/HLD 2.png" width="1200">
</p>

The platform follows a microservices architecture where each service owns a clearly defined business responsibility.

Client requests enter exclusively through the API Gateway, which acts as the security perimeter for the entire platform. After authentication and request validation, traffic is routed to the appropriate business service.

Business operations combine synchronous REST communication with asynchronous messaging through RabbitMQ.

Operations requiring immediate consistency—such as inventory reservation—use synchronous communication.

Long-running workflows such as payment processing and notifications are executed asynchronously using domain events.

This hybrid communication model minimizes response latency while preserving loose coupling between services.

---

# Core Architectural Principles

Several principles guide every component of the system.

## Single Responsibility

Each microservice owns exactly one business capability.

Responsibilities are intentionally separated so that changes within one domain have minimal impact on the remainder of the platform.

For example:

- Authentication is isolated from business logic.
- Inventory ownership belongs exclusively to the Product Service.
- Payment processing is delegated entirely to the Payment Service.
- Notification delivery never participates directly in checkout transactions.

This separation simplifies maintenance while improving deployment independence.

---

## Database per Service

Every microservice owns its own persistence layer.

Although the current Docker environment uses a shared PostgreSQL instance for simplicity, each service operates against an isolated logical database and never performs joins across another service's schema.

Inter-service communication always occurs through APIs or messaging rather than direct database access.

This maintains clear ownership boundaries and enables future independent deployments.

---

## Loose Coupling

Services communicate using contracts rather than implementation details.

Whenever immediate consistency is unnecessary, asynchronous messaging is preferred over synchronous HTTP communication.

Loose coupling provides several advantages:

- Independent deployment
- Better fault isolation
- Easier service evolution
- Reduced runtime dependencies
- Improved scalability

---

## Failure-Oriented Thinking

Distributed systems cannot eliminate failures.

Network partitions, service crashes, payment gateway outages, duplicate client requests, and delayed message delivery are all considered normal operating conditions.

Instead of attempting to prevent every failure, the platform is designed to recover safely from them through retries, idempotency, event replay, and compensating transactions.

This philosophy heavily influences the checkout workflow discussed later in this document.

---

## Observability by Design

Modern distributed systems are difficult to debug without visibility into request flow.

The architecture therefore incorporates observability as a first-class concern.

The long-term objective is to provide:

- Distributed tracing using Zipkin
- Metrics collection using Prometheus
- Dashboard visualization through Grafana
- Structured application logging
- Cross-service request correlation

These capabilities make it significantly easier to diagnose failures and understand system behaviour under load.

---

## Concurrency

The platform targets Java 21 and leverages Virtual Threads (Project Loom) to improve scalability under blocking workloads such as:

- Database operations
- HTTP communication
- RabbitMQ messaging
- External payment gateway requests

Unlike traditional thread-per-request models, virtual threads allow a significantly larger number of concurrent requests while consuming fewer operating system resources.

This approach preserves the simplicity of imperative programming without requiring a fully reactive programming model.

---

# Service Boundaries

The platform is intentionally divided into independent business domains rather than technical layers. Each microservice owns a single responsibility, its own persistence layer, and a clearly defined public contract.

This separation allows services to evolve independently while minimizing coupling across the system.

Rather than sharing databases or business logic, services collaborate using REST APIs and asynchronous domain events.

---

## API Gateway

The API Gateway is the only publicly accessible component of the platform.

No client communicates directly with internal microservices.

Instead, every request first enters the Gateway, where cross-cutting concerns are handled before traffic reaches the business layer.

### Responsibilities

- Request routing
- JWT validation
- Authentication & authorization
- Redis blacklist validation
- Rate limiting
- Request filtering
- Centralized logging
- Cross-service request correlation

### Why a Gateway?

Without an API Gateway, every microservice would need to duplicate authentication logic, security filters, and request validation.

Centralizing these responsibilities provides several advantages:

- Consistent security policies
- Reduced code duplication
- Simpler business services
- Easier auditing
- Single public entry point

Business services remain completely focused on domain logic rather than infrastructure concerns.

---

## Auth Service

The Auth Service owns the platform's identity management.

Its responsibility ends after successfully authenticating a user and issuing a signed JWT.

It deliberately contains **no checkout, inventory, payment, or notification logic.**

### Responsibilities

- User registration
- User authentication
- Password verification
- JWT issuance
- Token refresh
- Logout event generation

### Why isolate authentication?

Authentication evolves independently from business functionality.

Keeping authentication separate allows future integration with providers such as:

- OAuth2
- Google Sign-In
- Microsoft Entra ID
- LDAP
- Keycloak

without affecting any business service.

---

## Product Service

The Product Service owns the inventory domain.

No other service is permitted to modify product stock directly.

During checkout, the service validates inventory availability and performs stock reservation before payment begins.

### Responsibilities

- Product catalogue
- Inventory management
- Stock reservation
- Inventory restoration
- Product availability

### Why reserve stock first?

Inventory is validated before payment processing begins.

If sufficient stock is unavailable, checkout terminates immediately.

This avoids charging customers for products that cannot be fulfilled while preventing overselling during concurrent purchases.

The Product Service therefore acts as the authoritative source of inventory state.

---

## Order Service

The Order Service owns the lifecycle of customer orders.

Contrary to many monolithic implementations, it does **not** execute payment itself.

Instead, it records the business transaction, manages order state transitions, and publishes domain events that initiate downstream processing.

### Responsibilities

- Checkout workflow
- Redis-backed idempotency
- Order creation
- Order status transitions
- Domain event publication
- Saga participation

Orders progress through several lifecycle states, including:

- PAYMENT_PENDING
- COMPLETED
- CANCELLED
- PAYMENT_FAILED

### Why separate Orders from Payments?

Orders represent business intent.

Payments represent financial execution.

Separating these concerns provides several advantages:

- Independent scaling
- Independent deployment
- Cleaner domain boundaries
- Easier auditing
- Simpler payment provider replacement

The Order Service therefore becomes the authoritative owner of business state rather than financial processing.

---

## Payment Service

The Payment Service encapsulates all interaction with external payment providers.

No other microservice communicates directly with Stripe (or any future payment gateway).

Instead, payment requests are received through asynchronous events published by the Order Service.

### Responsibilities

- Payment execution
- Payment verification
- Success event publication
- Failure event publication
- Payment audit records

### Why isolate payment processing?

Payment gateways are external dependencies with unpredictable latency and availability.

Keeping payment logic isolated prevents these concerns from propagating throughout the rest of the platform.

It also simplifies future migration to providers such as:

- Razorpay
- PayPal
- Adyen
- Square

Only the Payment Service would require modification.

---

## Notification Service

The Notification Service consumes business events and delivers user notifications.

It never participates directly in checkout transactions.

### Responsibilities

- Email notifications
- Payment receipts
- Failure notifications
- Order confirmations

Because notifications are non-critical, they are processed asynchronously.

Checkout completion never waits for email delivery.

This improves response time while preventing notification failures from affecting business transactions.

---

# Engineering Decisions

The following sections describe the major architectural decisions that shaped the system.

Rather than optimizing solely for implementation simplicity, each decision attempts to balance scalability, resilience, maintainability, and operational complexity.

---

# 1. Castle & Moat Security

The platform follows the **Castle & Moat** security model.

In this architecture, internal services are isolated inside a private Docker network while the API Gateway acts as the single protected entrance into the system.

```
                 Internet
                     │
                     ▼
              API Gateway
                     │
      ┌──────────────┼──────────────┐
      │              │              │
   Auth         Product         Order
                     │
                 RabbitMQ
                     │
            Payment / Notification
```

Every external request must pass through the Gateway before reaching any business service.

This allows authentication and authorization policies to be enforced once rather than duplicated across every microservice.

### Advantages

- Single public entry point
- Centralized authentication
- Consistent authorization
- Simplified service implementation
- Reduced attack surface

---

# 2. Stateless Authentication

Authentication is implemented using cryptographically signed JWT tokens.

Once issued, a JWT contains all information required to authenticate future requests without requiring server-side session storage.

This allows every application instance to validate tokens independently, making horizontal scaling straightforward.

### The Logout Problem

JWTs remain valid until they expire.

If a user logs out, the token cannot normally be revoked immediately.

The platform solves this problem using Redis.

Revoked tokens are inserted into a distributed blacklist with an expiration time matching the token's remaining lifetime.

During request processing, the API Gateway performs a constant-time lookup before forwarding traffic.

```
JWT

↓

Gateway

↓

Redis Blacklist

↓

Valid?

↓

Forward Request
```

This approach preserves stateless authentication while supporting immediate logout.

---

# 3. Redis-backed Idempotency

Financial APIs must assume that duplicate requests will occur.

Clients may retry requests because of:

- Network interruptions
- Browser refreshes
- Mobile reconnections
- Timeout retries

Executing payment twice for the same order would be unacceptable.

To prevent this, every checkout request includes an `Idempotency-Key`.

The Order Service performs the following workflow:

1. Acquire a Redis lock.
2. Check whether the key has already been processed.
3. If found, immediately return the cached response.
4. Otherwise, continue with checkout.
5. Cache the final response for future retries.

This guarantees that identical requests produce identical results regardless of how many times they are submitted.

The design closely mirrors idempotency strategies used by payment platforms such as Stripe.

---

# 4. Database-per-Service

Each microservice owns its own persistence layer.

Although the development environment currently uses a shared PostgreSQL container, every service operates against an isolated logical database.

Examples include:

- `auth_db`
- `product_db`
- `order_db`
- `payment_db`

No service performs joins across another service's schema.

Instead, all collaboration occurs through:

- REST APIs
- RabbitMQ domain events

This preserves service autonomy while allowing independent schema evolution.

---

# 5. Choosing Between Synchronous and Asynchronous Communication

Not every operation requires the same communication model.

The platform intentionally combines synchronous REST calls with asynchronous messaging.

## Synchronous Communication

REST is used when immediate consistency is required.

For example, inventory must be successfully reserved before an order can be created.

```
Client

↓

Gateway

↓

Product Service

↓

Inventory Reserved

↓

Order Created
```

If inventory reservation fails, checkout stops immediately.

---

## Asynchronous Communication

Long-running operations execute through RabbitMQ.

Once the Order Service creates an order in the `PAYMENT_PENDING` state, it publishes an `OrderCreatedEvent`.

The Payment Service consumes the event independently and begins payment processing.

The client does not wait for downstream services to complete.

This approach reduces latency while improving fault isolation.


# Service Boundaries

The platform is intentionally divided into independent business domains rather than technical layers. Each microservice owns a single responsibility, its own persistence layer, and a clearly defined public contract.

This separation allows services to evolve independently while minimizing coupling across the system.

Rather than sharing databases or business logic, services collaborate using REST APIs and asynchronous domain events.

---

## API Gateway

The API Gateway is the only publicly accessible component of the platform.

No client communicates directly with internal microservices.

Instead, every request first enters the Gateway, where cross-cutting concerns are handled before traffic reaches the business layer.

### Responsibilities

- Request routing
- JWT validation
- Authentication & authorization
- Redis blacklist validation
- Rate limiting
- Request filtering
- Centralized logging
- Cross-service request correlation

### Why a Gateway?

Without an API Gateway, every microservice would need to duplicate authentication logic, security filters, and request validation.

Centralizing these responsibilities provides several advantages:

- Consistent security policies
- Reduced code duplication
- Simpler business services
- Easier auditing
- Single public entry point

Business services remain completely focused on domain logic rather than infrastructure concerns.

---

## Auth Service

The Auth Service owns the platform's identity management.

Its responsibility ends after successfully authenticating a user and issuing a signed JWT.

It deliberately contains **no checkout, inventory, payment, or notification logic.**

### Responsibilities

- User registration
- User authentication
- Password verification
- JWT issuance
- Token refresh
- Logout event generation

### Why isolate authentication?

Authentication evolves independently from business functionality.

Keeping authentication separate allows future integration with providers such as:

- OAuth2
- Google Sign-In
- Microsoft Entra ID
- LDAP
- Keycloak

without affecting any business service.

---

## Product Service

The Product Service owns the inventory domain.

No other service is permitted to modify product stock directly.

During checkout, the service validates inventory availability and performs stock reservation before payment begins.

### Responsibilities

- Product catalogue
- Inventory management
- Stock reservation
- Inventory restoration
- Product availability

### Why reserve stock first?

Inventory is validated before payment processing begins.

If sufficient stock is unavailable, checkout terminates immediately.

This avoids charging customers for products that cannot be fulfilled while preventing overselling during concurrent purchases.

The Product Service therefore acts as the authoritative source of inventory state.

---

## Order Service

The Order Service owns the lifecycle of customer orders.

Contrary to many monolithic implementations, it does **not** execute payment itself.

Instead, it records the business transaction, manages order state transitions, and publishes domain events that initiate downstream processing.

### Responsibilities

- Checkout workflow
- Redis-backed idempotency
- Order creation
- Order status transitions
- Domain event publication
- Saga participation

Orders progress through several lifecycle states, including:

- PAYMENT_PENDING
- COMPLETED
- CANCELLED
- PAYMENT_FAILED

### Why separate Orders from Payments?

Orders represent business intent.

Payments represent financial execution.

Separating these concerns provides several advantages:

- Independent scaling
- Independent deployment
- Cleaner domain boundaries
- Easier auditing
- Simpler payment provider replacement

The Order Service therefore becomes the authoritative owner of business state rather than financial processing.

---

## Payment Service

The Payment Service encapsulates all interaction with external payment providers.

No other microservice communicates directly with Stripe (or any future payment gateway).

Instead, payment requests are received through asynchronous events published by the Order Service.

### Responsibilities

- Payment execution
- Payment verification
- Success event publication
- Failure event publication
- Payment audit records

### Why isolate payment processing?

Payment gateways are external dependencies with unpredictable latency and availability.

Keeping payment logic isolated prevents these concerns from propagating throughout the rest of the platform.

It also simplifies future migration to providers such as:

- Razorpay
- PayPal
- Adyen
- Square

Only the Payment Service would require modification.

---

## Notification Service

The Notification Service consumes business events and delivers user notifications.

It never participates directly in checkout transactions.

### Responsibilities

- Email notifications
- Payment receipts
- Failure notifications
- Order confirmations

Because notifications are non-critical, they are processed asynchronously.

Checkout completion never waits for email delivery.

This improves response time while preventing notification failures from affecting business transactions.

---

# Engineering Decisions

The following sections describe the major architectural decisions that shaped the system.

Rather than optimizing solely for implementation simplicity, each decision attempts to balance scalability, resilience, maintainability, and operational complexity.

---

# 1. Castle & Moat Security

The platform follows the **Castle & Moat** security model.

In this architecture, internal services are isolated inside a private Docker network while the API Gateway acts as the single protected entrance into the system.

```
                 Internet
                     │
                     ▼
              API Gateway
                     │
      ┌──────────────┼──────────────┐
      │              │              │
   Auth         Product         Order
                     │
                 RabbitMQ
                     │
            Payment / Notification
```

Every external request must pass through the Gateway before reaching any business service.

This allows authentication and authorization policies to be enforced once rather than duplicated across every microservice.

### Advantages

- Single public entry point
- Centralized authentication
- Consistent authorization
- Simplified service implementation
- Reduced attack surface

---

# 2. Stateless Authentication

Authentication is implemented using cryptographically signed JWT tokens.

Once issued, a JWT contains all information required to authenticate future requests without requiring server-side session storage.

This allows every application instance to validate tokens independently, making horizontal scaling straightforward.

### The Logout Problem

JWTs remain valid until they expire.

If a user logs out, the token cannot normally be revoked immediately.

The platform solves this problem using Redis.

Revoked tokens are inserted into a distributed blacklist with an expiration time matching the token's remaining lifetime.

During request processing, the API Gateway performs a constant-time lookup before forwarding traffic.

```
JWT

↓

Gateway

↓

Redis Blacklist

↓

Valid?

↓

Forward Request
```

This approach preserves stateless authentication while supporting immediate logout.

---

# 3. Redis-backed Idempotency

Financial APIs must assume that duplicate requests will occur.

Clients may retry requests because of:

- Network interruptions
- Browser refreshes
- Mobile reconnections
- Timeout retries

Executing payment twice for the same order would be unacceptable.

To prevent this, every checkout request includes an `Idempotency-Key`.

The Order Service performs the following workflow:

1. Acquire a Redis lock.
2. Check whether the key has already been processed.
3. If found, immediately return the cached response.
4. Otherwise, continue with checkout.
5. Cache the final response for future retries.

This guarantees that identical requests produce identical results regardless of how many times they are submitted.

The design closely mirrors idempotency strategies used by payment platforms such as Stripe.

---

# 4. Database-per-Service

Each microservice owns its own persistence layer.

Although the development environment currently uses a shared PostgreSQL container, every service operates against an isolated logical database.

Examples include:

- `auth_db`
- `product_db`
- `order_db`
- `payment_db`

No service performs joins across another service's schema.

Instead, all collaboration occurs through:

- REST APIs
- RabbitMQ domain events

This preserves service autonomy while allowing independent schema evolution.

---

# 5. Choosing Between Synchronous and Asynchronous Communication

Not every operation requires the same communication model.

The platform intentionally combines synchronous REST calls with asynchronous messaging.

## Synchronous Communication

REST is used when immediate consistency is required.

For example, inventory must be successfully reserved before an order can be created.

```
Client

↓

Gateway

↓

Product Service

↓

Inventory Reserved

↓

Order Created
```

If inventory reservation fails, checkout stops immediately.

---

## Asynchronous Communication

Long-running operations execute through RabbitMQ.

Once the Order Service creates an order in the `PAYMENT_PENDING` state, it publishes an `OrderCreatedEvent`.

The Payment Service consumes the event independently and begins payment processing.

The client does not wait for downstream services to complete.

This approach reduces latency while improving fault isolation.


# 6. Transactional Outbox Pattern

One of the fundamental challenges in distributed systems is ensuring that database state and published events remain consistent.

Consider the following checkout sequence:

1. The Order Service successfully commits a new order to its database.
2. Immediately afterwards, it attempts to publish an `OrderCreatedEvent` to RabbitMQ.
3. RabbitMQ becomes temporarily unavailable.

The result is a dangerous inconsistency.

The order now exists in the database, but no downstream service is aware of it.

Since the Payment Service never receives the event, payment processing never begins, leaving the order permanently stuck in the `PAYMENT_PENDING` state.

This problem cannot be solved using a distributed database transaction because RabbitMQ does not participate in ACID transactions with PostgreSQL.

---

## The Solution

Instead of publishing directly to RabbitMQ, the Order Service writes two records within the **same local database transaction**:

- The new order.
- A corresponding Outbox record describing the event that must eventually be published.

```
Database Transaction

├── Order
└── Outbox Event
```

Because both writes occur atomically, the system guarantees that an order can never exist without a corresponding event waiting to be published.

---

## Event Publication

A lightweight background publisher continuously polls the Outbox table for unpublished events.

For every pending record it:

1. Reads the event payload.
2. Publishes it to RabbitMQ.
3. Marks the Outbox entry as published.

```
Order DB

↓

Outbox Table

↓

Outbox Publisher

↓

RabbitMQ
```

If RabbitMQ is temporarily unavailable, the Outbox record simply remains pending.

Once the broker becomes available again, the publisher retries automatically.

This design guarantees **eventual event delivery** without introducing distributed transactions.

---

## Why This Pattern?

The Transactional Outbox provides several important guarantees.

- Database state and event publication remain consistent.
- RabbitMQ outages do not lose business events.
- Failed publications can be retried safely.
- Services remain loosely coupled.
- No distributed transaction coordinator is required.

Although the current implementation is still evolving, the architecture is intentionally designed around this production-proven pattern.

---

# 7. Saga Choreography

Traditional ACID transactions work well inside a single database.

However, modern microservices own independent databases.

Once multiple services participate in the same business workflow, distributed transactions become impractical.

Rather than relying on Two-Phase Commit (2PC), this platform coordinates business operations using **Saga Choreography**.

Each service completes its own local transaction before publishing a domain event.

Other services react to those events independently.

No central coordinator controls the workflow.

---

## Why Choreography Instead of Orchestration?

Two common Saga implementations exist.

### Saga Orchestration

<p align="center">
    <img src="./image/SAGA Sequence.png" width="1100">
</p>

A central coordinator instructs every service what to do.

```
Orchestrator

├── Product
├── Order
├── Payment
└── Notification
```

While simple to understand, the orchestrator becomes another critical service that must remain available.

It also introduces tighter coupling because every business workflow depends on a single coordinator.

---

### Saga Choreography

This project instead adopts an event-driven choreography model.

```
Order Service

↓

RabbitMQ

↓

Payment Service

↓

RabbitMQ

↓

Notification Service
```

Each service reacts only to events relevant to its own domain.

No service needs to understand the complete business workflow.

---

## Advantages

Choosing choreography provides several benefits.

- No central coordinator
- Better service autonomy
- Independent deployment
- Easier horizontal scaling
- Loose coupling
- Natural event-driven communication

---

## Trade-offs

The approach is not without disadvantages.

Business workflows become distributed across multiple services.

Understanding request flow often requires distributed tracing and centralized logging.

This is one reason observability becomes a first-class architectural concern in production microservice systems.

---

# 8. Distributed Transaction Lifecycle

The checkout workflow consists of several independent local transactions connected through domain events.

Rather than one long database transaction, each service commits its own work before notifying downstream consumers.

---

## Successful Checkout

```
Client

↓

API Gateway

↓

Product Service
Validate & Reserve Inventory

↓

Order Service
Create Order (PAYMENT_PENDING)

↓

Order + Outbox
(Single Database Transaction)

↓

Outbox Publisher

↓

RabbitMQ

↓

Payment Service

↓

Stripe

↓

PaymentCompletedEvent

↓

Order Service

↓

Order Status = COMPLETED

↓

Notification Service

↓

Confirmation Email
```

Each service owns only its own data.

Consistency emerges gradually through event propagation rather than distributed locking.

---

## Failed Checkout

If payment fails, the system performs a compensating transaction instead of attempting to roll back completed database operations.

```
Payment Failed

↓

PaymentFailedEvent

↓

Order Service

↓

Order Status = PAYMENT_FAILED

↓

InventoryRollbackEvent

↓

Product Service

↓

Restore Inventory

↓

Notification Service

↓

Failure Email
```

Rather than undoing previous commits, the platform restores consistency through additional business actions.

This compensation-based approach is significantly more resilient than attempting global rollbacks across multiple databases.

---

# 9. Failure-Oriented Design

Failures are expected in distributed systems.

The platform therefore assumes that every network call, message broker, and external dependency may eventually fail.

Examples include:

- Client retries
- Duplicate submissions
- Network partitions
- Service crashes
- Payment gateway outages
- RabbitMQ downtime
- Delayed message delivery

Instead of treating these situations as exceptional, the architecture incorporates recovery mechanisms directly into the design.

These mechanisms include:

- Redis-backed idempotency
- Saga compensation
- Transactional Outbox
- Event replay
- Retry policies (planned)
- Dead Letter Queues (planned)

This philosophy significantly improves resilience while avoiding tight coupling between services.

---

# 10. Gateway Request Pipeline

Every external request follows the same processing pipeline before reaching business logic.

```
Incoming Request

↓

Logging Filter

↓

Authentication Filter

↓

JWT Validation

↓

Redis Blacklist Check

↓

Rate Limiter

↓

Request Correlation

↓

Route Resolution

↓

Target Microservice
```

Each filter has a single responsibility.

This pipeline architecture improves maintainability while making it straightforward to introduce new cross-cutting concerns without modifying business services.

---

# 11. High-Concurrency Processing

The platform targets Java 21 and leverages **Virtual Threads (Project Loom)** for handling blocking workloads.

Traditional thread-per-request architectures allocate one operating system thread to every incoming request.

Under heavy load this quickly becomes expensive in terms of memory consumption and context-switch overhead.

Virtual Threads allow the application to maintain the familiar imperative programming model while dramatically increasing the number of concurrent requests that can be processed.

Typical blocking operations include:

- PostgreSQL queries
- REST communication
- RabbitMQ messaging
- External payment gateway calls

By combining Virtual Threads with stateless services, the platform is able to scale more efficiently without adopting a fully reactive programming model.



# 12. Scalability Strategy

One of the primary motivations behind adopting a microservices architecture is the ability to scale business capabilities independently.

Unlike a monolithic application—where every feature must scale together—the platform allows each service to be replicated according to its own workload characteristics.

For example, during a large promotional event, payment requests may increase significantly while authentication traffic remains relatively stable.

Rather than deploying additional instances of the entire application, only the Payment Service requires additional replicas.

```
                     API Gateway
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
     Auth ×2          Product ×3         Order ×4
                                              │
                                        RabbitMQ
                                              │
                                  Payment ×6      Notification ×2
```

This deployment model provides several advantages:

- Independent horizontal scaling
- Better resource utilization
- Fault isolation
- Independent deployments
- Faster release cycles

Because services remain stateless, replicas can be added or removed without changing application logic.

Persistent state is delegated to dedicated infrastructure such as PostgreSQL and Redis.

---

# 13. Observability Strategy

Distributed systems are significantly more difficult to debug than monolithic applications.

A single client request may traverse multiple independent services before completion.

Without proper observability, identifying the source of failures becomes increasingly difficult as the system grows.

For this reason, observability is treated as a core architectural concern rather than an afterthought.

---

## Metrics Collection

The platform exposes application and JVM metrics using Spring Boot Actuator and Micrometer.

Prometheus periodically scrapes these metrics from every microservice.

Examples include:

- HTTP request latency
- Request throughput
- Error rates
- JVM memory usage
- Garbage collection activity
- Thread utilization
- Custom business metrics

These metrics provide visibility into application health and system performance.

---

## Visualization

Grafana consumes metrics collected by Prometheus and presents them through configurable dashboards.

Typical dashboards include:

- Service health
- JVM performance
- Request latency
- Business metrics
- Infrastructure monitoring

The objective is to provide operators with a centralized view of the entire platform.

---

## Distributed Tracing

Asynchronous workflows make request debugging considerably more challenging.

A single checkout request may span several independent services before completion.

To reconstruct the complete execution path, the architecture incorporates distributed tracing using Zipkin.

Each request receives a unique Trace ID that propagates across service boundaries.

This allows developers to visualize:

- Request latency
- Cross-service communication
- External API calls
- Slow downstream dependencies
- Failure propagation

Tracing becomes especially valuable when debugging Saga workflows that involve multiple asynchronous events.

---

## Structured Logging

Application logs are generated in a structured format so that events from multiple services can be correlated using shared request identifiers.

Instead of treating logs as isolated messages, structured logging enables developers to reconstruct complete business workflows across service boundaries.

Future iterations may integrate centralized log aggregation solutions such as the ELK Stack or Grafana Loki.

---

# 14. Trade-offs & Design Considerations

Every architectural decision introduces trade-offs.

This project intentionally favors scalability, resilience, and service autonomy over implementation simplicity.

| Decision | Benefit | Trade-off |
|----------|---------|-----------|
| Microservices | Independent deployment and scaling | Increased operational complexity |
| Saga Choreography | Loose coupling | Harder to visualize workflows |
| RabbitMQ | Asynchronous processing | Eventual consistency |
| Database-per-Service | Strong ownership boundaries | No cross-service joins |
| JWT Authentication | Stateless scalability | Requires token revocation strategy |
| Transactional Outbox | Reliable event publication | Additional infrastructure and background processing |
| Virtual Threads | High concurrency with imperative code | Requires Java 21+ |

Understanding these trade-offs is an important aspect of designing production distributed systems.

There is no universally "correct" architecture—only architectures that best satisfy a system's functional and operational requirements.

---

# 15. Current Status

The project is actively evolving toward a more production-oriented architecture.

Several capabilities have already been implemented, while others remain under development.

### Implemented

- Microservices architecture
- Spring Cloud Gateway
- JWT authentication
- Redis-backed idempotency
- Database-per-Service pattern
- RabbitMQ messaging
- Saga-based distributed transactions
- Payment isolation
- Java 21 Virtual Threads
- Dead Letter Queues (DLQ)
- Docker Compose deployment
- Prometheus metrics
- Grafana dashboards
- Zipkin distributed tracing

### In Progress

- Transactional Outbox publisher
- Improved Saga compensation workflow
- OpenTelemetry instrumentation

### Planned

- Retry policies
- Resilience4j circuit breakers
- Kubernetes deployment
- GitHub Actions CI/CD
- Service discovery
- Kafka-based event streaming
- Distributed configuration management

The architecture has intentionally been designed so that these capabilities can be incorporated incrementally without requiring major structural changes.

---

# 16. Future Roadmap

The long-term objective is to evolve this project into a production-inspired reference implementation for modern distributed backend systems.

Future work is organized into the following areas.

## Reliability

- Dead Letter Queues (DLQ)
- Retry policies
- Reconciliation jobs
- Transactional Outbox optimization

## Observability

- Prometheus
- Grafana
- Zipkin
- OpenTelemetry
- Centralized logging

## Scalability

- Kubernetes deployment
- Kafka event streaming
- Service discovery
- Autoscaling

## DevOps

- GitHub Actions CI/CD
- Automated testing pipeline
- Container image publishing
- Infrastructure as Code

---

# 17. Concepts Demonstrated

This project demonstrates a collection of architectural patterns commonly encountered in modern distributed systems.

### Distributed Systems

- Microservices Architecture
- Domain-Driven Service Boundaries
- Database-per-Service
- Event-Driven Architecture
- Saga Choreography
- Eventual Consistency

### Reliability

- Transactional Outbox Pattern
- Redis-backed Idempotency
- Failure-Oriented Design
- Compensating Transactions

### Security

- API Gateway
- Castle & Moat Security
- Stateless JWT Authentication
- Redis Token Blacklisting

### Scalability

- Horizontal Scaling
- Java 21 Virtual Threads
- Loose Coupling
- Independent Service Deployment

[//]: # (### Observability)

[//]: # ()
[//]: # (- Prometheus Metrics)

[//]: # (- Grafana Dashboards)

[//]: # (- Zipkin Distributed Tracing)

[//]: # (- Structured Logging)

---

# Closing Remarks

This project is intended to explore the architectural challenges involved in building resilient distributed backend systems.

Rather than focusing solely on feature development, the implementation emphasizes engineering trade-offs, service boundaries, distributed communication, and failure recovery mechanisms that are commonly encountered in production environments.

As the project continues to evolve, additional capabilities will be incorporated to further align the implementation with real-world cloud-native system design while preserving the architectural principles outlined throughout this document.