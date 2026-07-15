# Distributed E-Commerce Microservices Architecture

An event-driven, containerized e-commerce backend designed to demonstrate modern distributed system patterns, including centralized perimeter security, asynchronous messaging, and distributed caching for transactional idempotency.

## System Architecture

![System Architecture Diagram](./image/Architecture.png)
This architecture utilizes a **"Castle and Moat" perimeter security model**. All internal microservices are isolated within a private Docker network and are inaccessible from the outside world. All traffic must pass through the API Gateway.

### Core Flows:
1. **Authentication Offloading:** The `api-gateway` intercepts all incoming requests, extracts the JWT, and cryptographically verifies it using a shared secret. If valid, it routes the traffic to downstream services. Downstream services do not contain security dependencies.
2. **Transactional Idempotency:** The `order-service` leverages Redis to hash and store incoming `Idempotency-Key` headers. This prevents duplicate database entries or double-charging if a user clicks "Checkout" multiple times.
3. **Event-Driven Messaging:** Upon saving an order to PostgreSQL, the `order-service` publishes an `OrderEvent` to a RabbitMQ exchange. The `notification-service` consumes this queue asynchronously, ensuring the main user thread is never blocked by email/SMS processing delays.

## Tech Stack
* **Language:** Java 21
* **Framework:** Spring Boot 3.x, Spring Cloud Gateway
* **Security:** JSON Web Tokens (jjwt)
* **Databases:** PostgreSQL (Relational), Redis (Distributed Cache)
* **Message Broker:** RabbitMQ
* **Containerization:** Docker & Docker Compose

## Microservices Breakdown

| Service | Port | Description |
| :--- | :--- | :--- |
| `api-gateway` | `8080` | The single entry point. Handles routing and JWT validation (The Bouncer). |
| `auth-service` | `8081` | Identity provider. Issues cryptographically signed JWTs. |
| `product-service`| `8082` | Manages product catalog. Isolated behind the Gateway. |
| `order-service` | `8083` | Handles checkout logic, Redis idempotency checks, and publishes events. |
| `notification-service`| `8084` | Background worker that consumes RabbitMQ queues to simulate emails. |

## Local Development & Testing

### Prerequisites
* Java 21 & Maven installed locally.
* Docker Desktop installed and running.

### 1. Build the Microservices
Run this from the root directory to compile all Java modules into `.jar` files:
```bash
mvn clean package -DskipTests
