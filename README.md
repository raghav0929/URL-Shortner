# URL Shortener — Microservices System

A distributed URL shortening service built with **Java Spring Boot**, designed and implemented from a system design whiteboard exercise through to a working multi-service architecture.

It converts long URLs into short, shareable codes and redirects users back to the original URL — similar to Bitly or TinyURL — but built as a set of independently deployable microservices to demonstrate service discovery, API gateway routing, distributed ID generation, and NoSQL data modeling.

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
                                  │ (url data)  │       │ (ID ranges) │
                                  └─────────────┘       └─────────────┘
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| **service-registry** | 8761 | Eureka server — service discovery for all other services |
| **api-gateway** | 8080 | Single entry point; routes requests to the correct downstream service |
| **token-service** | 8081 | Hands out non-overlapping numeric ID ranges to prevent short-code collisions |
| **url-shortener-service** | 8082 | Core business logic — shortens URLs, resolves/redirects, stores mappings |

---

## Key Design Decisions

### Distributed, collision-free ID generation
Rather than using a single auto-increment counter (a bottleneck and single point of failure) or independent random generation per instance (which risks collisions), `url-shortener-service` instances request **blocks of IDs** (e.g. 1,000 at a time) from `token-service`. Each instance then generates IDs from its local block in memory, only contacting `token-service` again once its block is exhausted. This keeps ID generation both collision-free and fast, since most ID generation never touches the network.

`token-service` guarantees no two instances ever receive overlapping ranges using a **pessimistic row lock** (`SELECT ... FOR UPDATE`) on its PostgreSQL-backed counter table, wrapped in a database transaction.

### Base62 encoding
Numeric IDs are encoded into short, URL-safe strings using a 62-character alphabet (`0-9`, `a-z`, `A-Z`), giving compact codes (`aZ3kP`) while supporting a very large ID space.

### Cassandra for URL storage
The URL mapping table is write-optimized and horizontally scalable, well suited to a system where reads (redirects) vastly outnumber writes (new short URLs) once cached, and where the dataset can grow very large over time.

### 302 (not 301) redirects
Redirects use HTTP 302 rather than 301 so that every click passes through the server — this keeps click-analytics tracking possible, whereas a 301 would let browsers cache the redirect and bypass the server on repeat visits.

---

## Tech Stack

- **Java 17**, **Spring Boot 3.5**
- **Spring Cloud** — Netflix Eureka (service discovery), Spring Cloud Gateway (API gateway), OpenFeign (inter-service HTTP calls)
- **Apache Cassandra** — URL mapping storage
- **PostgreSQL** — token/ID-range allocation storage
- **Maven** — build tool

---

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL (running locally, with a database created for token-service)
- Apache Cassandra (running locally — via Docker or native install)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/url-shortener-microservices.git
cd url-shortener-microservices
```

### 2. Set up PostgreSQL

Create a database for `token-service`:

```sql
CREATE DATABASE token_db;
```

Seed the initial counter row (run once, after `token-service` has started at least once and created its table):

```sql
INSERT INTO token_ranges (id, next_available, range_size, updated_at)
VALUES (1, 1, 1000, NOW());
```

Set your database credentials via environment variable (or edit `token-service/src/main/resources/application.yml` directly):

```bash
export DB_PASSWORD=your_postgres_password
```

### 3. Set up Cassandra

Start Cassandra (example using Docker):

```bash
docker run -d --name cassandra -p 9042:9042 cassandra:4.1
```

Once running, create the keyspace and table:

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

### 4. Run the services (in this order)

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
├── api-gateway/                 # Spring Cloud Gateway — single entry point
├── token-service/                # Distributed ID range allocation (Postgres)
├── url-shortener-service/       # Core shorten/resolve logic (Cassandra)
├── .gitignore
└── README.md
```

---

## Roadmap

- [ ] Redis caching on the read/resolve path
- [ ] Click analytics and a `/stats/{short_code}` endpoint
- [ ] Custom alias support
- [ ] Rate limiting at the API Gateway
- [ ] Dockerized full-stack deployment (`docker-compose`) for one-command startup

---

## Author

Built as a hands-on system design and microservices learning project — from whiteboard architecture through to a working distributed system.
