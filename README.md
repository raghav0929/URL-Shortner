# URL Shortener — Microservices System

A distributed URL shortening service built with **Java Spring Boot**, designed and implemented from a system design whiteboard exercise through to a working multi-service architecture.

It converts long URLs into short, shareable codes and redirects users back to the original URL — similar to Bitly or TinyURL — but built as a set of independently deployable microservices to demonstrate service discovery, API gateway routing, distributed ID generation, caching, and NoSQL data modeling.

---

## Architecture

```
                              ┌─────────────────────┐
                              │   Service Registry   │
                              │      (Eureka)        │
                              └──────────▲───────────┘
                                         │ registers
                    ┌────────────────────┼────────────────────┐
                    │                    │                    │
             ┌──────▼──────┐     ┌───────▼────────┐    ┌──────▼───────┐
   Client ──▶│ API Gateway │────▶│ URL Shortener   │───▶│ Token Service│
             │ (port 8080) │     │ Service (8082)  │    │  (port 8081) │
             └─────────────┘     └───────┬─────────┘    └──────┬───────┘
                                         │                     │
                                  ┌──────▼──────┐       ┌──────▼──────┐
                                  │  Cassandra  │       │  PostgreSQL │
                                  │ + Redis     │       │ (ID ranges) │
                                  │ (url data,  │       └─────────────┘
                                  │  caching)   │
                                  └─────────────┘
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| **service-registry** | 8761 | Eureka server — service discovery for all other services |
| **api-gateway** | 8080 | Single entry point; routes requests to the correct downstream service |
| **token-service** | 8081 | Hands out non-overlapping numeric ID ranges to prevent short-code collisions |
| **url-shortener-service** | 8082 | Core business logic — shortens URLs, resolves/redirects, tracks clicks, serves stats |

---

## Key Design Decisions

### Distributed, collision-free ID generation
Rather than using a single auto-increment counter (a bottleneck and single point of failure) or independent random generation per instance (which risks collisions), `url-shortener-service` instances request **blocks of IDs** (e.g. 1,000 at a time) from `token-service`. Each instance then generates IDs from its local block in memory, only contacting `token-service` again once its block is exhausted. This keeps ID generation both collision-free and fast, since most ID generation never touches the network.

`token-service` guarantees no two instances ever receive overlapping ranges using a **pessimistic row lock** (`SELECT ... FOR UPDATE`) on its PostgreSQL-backed counter table, wrapped in a database transaction.

### Base62 encoding
Numeric IDs are encoded into short, URL-safe strings using a 62-character alphabet (`0-9`, `a-z`, `A-Z`), giving compact codes (`aZ3kP`) while supporting a very large ID space.

### Cassandra for URL storage
The URL mapping table is write-optimized and horizontally scalable, well suited to a system where reads (redirects) vastly outnumber writes (new short URLs), and where the dataset can grow very large over time.

### Redis caching on the read path
Since redirects are the dominant traffic pattern for any URL shortener, resolved URLs are cached in Redis (`@Cacheable`, 24-hour TTL) after the first Cassandra lookup. Subsequent redirects for the same short code are served entirely from Redis, keeping Cassandra load low and redirect latency minimal.

### Asynchronous click tracking
Click counts are updated via an `@Async` method so that incrementing `click_count` never blocks or slows down the redirect response itself — the user gets their 302 immediately while the count updates in the background. This is a read-modify-write against Cassandra (not a native `COUNTER` column), which is a deliberate simplification; a production system with very high concurrent click volume on the same short code would use a Cassandra `COUNTER` column in a separate table for atomic increments.

### 302 (not 301) redirects
Redirects use HTTP 302 rather than 301 so that every click passes through the server — this keeps click-analytics tracking possible, whereas a 301 would let browsers cache the redirect and bypass the server on repeat visits.

---

## Tech Stack

- **Java 17**, **Spring Boot 3.5**
- **Spring Cloud** — Netflix Eureka (service discovery), Spring Cloud Gateway (WebMVC variant), OpenFeign (inter-service HTTP calls), Spring Cloud LoadBalancer
- **Apache Cassandra** — URL mapping storage
- **Redis** — read-path caching
- **PostgreSQL** — token/ID-range allocation storage
- **Spring Boot Actuator** — health checks and observability
- **Docker Compose** — local Cassandra + Redis provisioning
- **Maven** — build tool

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker Desktop (for Cassandra + Redis)
- PostgreSQL (native local install)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/url-shortener-microservices.git
cd url-shortener-microservices
```

### 2. Start Cassandra and Redis via Docker Compose

```bash
docker-compose up -d
```

This starts both containers with persistent volumes. Verify:

```bash
docker ps
```

### 3. Create the Cassandra keyspace and table

```bash
docker exec -it cassandra cqlsh
```

Inside the `cqlsh>` prompt:

```sql
CREATE KEYSPACE url_shortener
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE url_shortener;

CREATE TABLE urls (
    short_code text PRIMARY KEY,
    original_url text,
    created_at timestamp,
    expires_at timestamp,
    click_count bigint
);
```

### 4. Set up PostgreSQL

Create a database for `token-service`:

```sql
CREATE DATABASE token_db;
```

Seed the initial counter row (run once, after `token-service` has started at least once and created its table via `ddl-auto: update`):

```sql
INSERT INTO token_ranges (id, next_available, range_size, updated_at)
VALUES (1, 1, 1000, NOW());
```

Set your database credentials via environment variable (or edit `token-service/src/main/resources/application.yml` directly):

```bash
export DB_PASSWORD=your_postgres_password
```

### 5. Run the services (in this order)

Each service can be run from its own folder via Maven, or directly from your IDE.

```bash
# 1. Service Registry
cd service-registry
./mvnw spring-boot:run

# 2. Token Service (in a new terminal)
cd token-service
./mvnw spring-boot:run

# 3. URL Shortener Service (in a new terminal)
cd url-shortener-service
./mvnw spring-boot:run

# 4. API Gateway (in a new terminal)
cd api-gateway
./mvnw spring-boot:run
```

Verify all services registered successfully at the Eureka dashboard: **http://localhost:8761**

Verify the gateway is healthy: **http://localhost:8080/actuator/health**

---

## API Reference

All requests go through the API Gateway on **`http://localhost:8080`**.

### Shorten a URL

```
POST /api/v1/shorten
Content-Type: application/json

{
  "original_url": "https://example.com"
}
```

**Response — `200 OK`**
```json
{
  "short_code": "aZ3kP"
}
```

### Resolve / redirect

```
GET /api/v1/{short_code}
```

**Response — `302 Found`**, with header:
```
Location: https://example.com
```

First request for a given short code is served from Cassandra and cached in Redis; subsequent requests are served from Redis until the 24-hour TTL expires. Each successful redirect asynchronously increments `click_count`.

### Get stats for a short URL

```
GET /api/v1/stats/{short_code}
```

**Response — `200 OK`**
```json
{
  "shortCode": "aZ3kP",
  "originalUrl": "https://example.com",
  "createdAt": "2026-07-06T12:00:00Z",
  "expiresAt": "2027-07-06T12:00:00Z",
  "clickCount": 4
}
```

### Error responses

```json
{
  "timestamp": "2026-07-06T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Short code not found: xyz123",
  "path": "/api/v1/xyz123"
}
```

---

## Project Structure

```
url-shortener-microservices/
├── service-registry/           # Eureka service discovery server
├── api-gateway/                 # Spring Cloud Gateway (WebMVC) — single entry point
├── token-service/                # Distributed ID range allocation (Postgres)
├── url-shortener-service/       # Core shorten/resolve/stats logic (Cassandra + Redis)
├── assets/                       # HLD diagram
├── docker-compose.yml            # Cassandra + Redis provisioning
├── .gitignore
└── README.md
```

---

## System Design

![High Level Design](./assets/hld-diagram.png)

Original whiteboard design covering capacity estimation, character set/URL length reasoning, the range-based token allocation approach (and why a naive shared-counter or independent-Redis-instance approach was rejected), and the read/write flow through the system.

---

## Roadmap

- [x] Redis caching on the read/resolve path
- [x] Click analytics and a `/stats/{short_code}` endpoint
- [x] Docker Compose for one-command Cassandra + Redis startup
- [x] Global exception handling (clean 404/400 JSON responses)
- [ ] Custom alias support
- [ ] Rate limiting at the API Gateway
- [ ] Cassandra `COUNTER` column for atomic click tracking
- [ ] Full containerized deployment (Spring Boot services included in docker-compose)

---

## Author

Built as a hands-on system design and microservices learning project — from whiteboard architecture through to a working distributed system, including debugging real service-discovery and Spring Cloud Gateway routing issues along the way.