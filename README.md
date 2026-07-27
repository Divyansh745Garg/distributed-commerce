# Distributed E-Commerce Backend

A highly scalable, distributed e-commerce backend built with **Java 21**, **Spring Boot 3**, and **Docker**. This project follows a microservices architecture designed to support high concurrency, resilient payment processing, asynchronous event-driven communication, and cloud-native scalability.

## System Architecture

[![System Architecture Diagram](./image/Architecture.png)](./image/Architecture.png)

---

# ✨ Features

- 🚀 **Microservices Architecture**
  - Independent Spring Boot services with clear separation of responsibilities.
  - Easy horizontal scaling and independent deployments.

- 🌐 **Spring Cloud Gateway**
  - Centralized entry point for all incoming traffic.
  - Built using Spring WebFlux and Netty.
  - Supports:
    - JWT Authentication
    - Rate Limiting
    - Request Logging
    - Routing

- ⚡ **High-Concurrency Processing**
  - Java 21 Virtual Threads enable thousands of concurrent blocking operations with minimal resource consumption.
  - Ideal for database-heavy workloads.

- 💳 **Idempotent Payment Processing**
  - Dedicated Payment Service.
  - Redis distributed locks prevent duplicate payment execution during retries.
  - Safe handling of network failures and duplicate requests.

- 📨 **Event-Driven Communication**
  - RabbitMQ enables asynchronous communication between services.
  - Decouples long-running workflows from user-facing requests.

- 🛡️ **Cloud-Native Infrastructure**
  - Dockerized services.
  - One-command deployment using Docker Compose.

---

# Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| API Gateway | Spring Cloud Gateway |
| Concurrency | Java Virtual Threads (Project Loom) |
| Database | PostgreSQL |
| Cache | Redis |
| Message Broker | RabbitMQ |
| Containerization | Docker, Docker Compose |
| Architecture | Microservices, Event-Driven |

---

# Design Patterns

- Event-Driven Architecture
- API Gateway Pattern
- Idempotent Payment Processing
- Distributed Caching
- Distributed Locking (Redis)
- Saga Pattern *(Planned)*

---

# Prerequisites

Install the following before running the project:

- Java 21 JDK
- Docker
- Docker Desktop
- Docker Compose

---

# Running the Project

## Clone the Repository

```bash
git clone https://github.com/your-username/distributed-commerce.git
cd distributed-commerce
```

---

## Build All Microservices

```bash
./mvnw clean package -DskipTests
```

---

## Start the Entire System

```bash
docker-compose up --build -d
```

This command starts:

- PostgreSQL
- Redis
- RabbitMQ
- API Gateway
- All Spring Boot Microservices

Wait approximately **20–30 seconds** for every service to become healthy.

---

## Verify Deployment

Check whether the API Gateway has started successfully.

```bash
docker logs api-gateway
```

---

# 🧹 Cleanup

Stop and remove all containers.

```bash
docker-compose down
```

Remove unused Docker images and cached layers.

```bash
docker system prune -f
```

---

# 📡 Service Architecture

## API Gateway

Acts as the single entry point for all requests.

Responsibilities:

- Authentication
- Authorization
- Rate Limiting
- Request Logging
- Service Routing

Built using:

- Spring Cloud Gateway
- Spring WebFlux
- Netty

---

## Order Service

Responsible for:

- Order creation
- Order lifecycle
- Publishing checkout events

Publishes:

```
OrderPlacedEvent
```

to RabbitMQ.

---

## Payment Service

Responsible for secure payment execution.

Features:

- Redis distributed locking
- Idempotent transaction handling
- Retry-safe processing

Ensures duplicate network requests never create duplicate charges.

---

## Notification Service

Consumes events asynchronously from RabbitMQ.

Example:

```
OrderPlacedEvent
        │
        ▼
Notification Service
        │
        ▼
Email / SMS / Push Notification
```

This keeps checkout latency low while notifications execute in the background.

---

## Redis

Used for:

- Distributed Locks
- Idempotency Keys
- Temporary State

---

## RabbitMQ

Acts as the event bus between services.

Example Event Flow:

```text
Client
   │
   ▼
Order Service
   │
   ▼
RabbitMQ
   │
   ▼
Notification Service
```

---

## PostgreSQL

Stores persistent business data including:

- Users
- Orders
- Products
- Payments

---

# 📈 Scalability Highlights

- Java 21 Virtual Threads for massive concurrency
- Reactive API Gateway using Netty
- Independent microservice deployments
- Asynchronous messaging with RabbitMQ
- Redis-backed distributed locking
- Dockerized infrastructure
- Fault isolation between services

---

# 🔮 Planned Enhancements

- Saga Pattern for distributed transactions
- Distributed tracing (OpenTelemetry)
- Prometheus & Grafana monitoring
- Kubernetes deployment
- Circuit Breakers using Resilience4j
- Service Discovery with Eureka
- Centralized Configuration Server

---

# 📄 License

This project is intended for educational and demonstration purposes.
