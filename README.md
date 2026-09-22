# Fair Trip Distribution

A production-ready backend system that fairly allocates trips to vendors based on contractual share agreements, using a **Most-Owed-First with carry-forward** algorithm implemented entirely in integer arithmetic.

---

## Problem Statement

Ride-hailing and logistics platforms contract vendors (fleet operators) to receive a promised percentage of trips in a given geographic zone. Without a fairness engine, allocation becomes arbitrary. This system ensures every vendor receives their contracted share — tracked continuously, with no resets — so shortfalls are automatically corrected over time.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | MySQL 8 (H2 for tests) |
| Cache | Redis (Spring Cache abstraction) |
| Security | Spring Security + JWT (HS512) |
| API Docs | Springdoc OpenAPI 2 / Swagger UI |
| Monitoring | Spring Boot Actuator |
| Build | Maven |

---

## Architecture

```
Client
  │
  ▼
REST Controllers  ──► Spring Security (JWT filter)
  │
  ▼
Service Layer     ──► Redis (read-heavy config cache, 10 min TTL)
  │                         │ fallback on miss
  ▼                         ▼
Repository Layer  ──► MySQL (source of truth)
```

- **Stateless**: No HTTP sessions. Each request is authenticated via JWT.
- **Transactional**: All allocation counter updates are atomic. Rejections fully revert state.
- **Pessimistic locking**: `PESSIMISTIC_WRITE` on allocation buckets and capacity rows prevents race conditions under concurrency.
- **Redis optional**: Application runs fully correctly if Redis is unavailable; cache failures are swallowed by a custom `CacheErrorHandler`.

---

## Core Fairness Algorithm

### Allocation Bucket
Each unique **(Zone × TripType)** combination forms an independent fairness bucket.

### Basis Points
Shares are stored as **integer basis points** (10 000 bp = 100%). This eliminates all floating-point rounding errors. Arithmetic is always integer/long.

### Most-Owed-First (carry-forward)
For each eligible vendor in a bucket:

```
shortfall = (bucket.totalTrips × vendor.targetBasisPoints) − (vendor.allocatedTrips × 10 000)
```

The vendor with the **highest shortfall** receives the trip. A large positive shortfall means the vendor is behind their promised share.

**Tie-breaking** (fully deterministic, no randomness):
1. Lower `priority` value wins.
2. Lower `vendorId` wins.

**Carry-forward**: Counters are never reset. A shortfall from Monday is automatically corrected on Tuesday.

### State Update (atomic, in one transaction)
1. Increment `bucket.totalTrips`.
2. Increment `vendor.allocatedTrips`.
3. Decrement `vendor.availableCapacity`.

---

## Capacity Constraints

- Vendors with `availableCapacity ≤ 0` are skipped during allocation.
- Vendors with **no `VendorCapacity` record** are treated as having **unlimited capacity**.
- Capacity is decremented on allocation and restored on rejection.
- Capacity rows are fetched under `PESSIMISTIC_WRITE` lock to prevent double-allocation under concurrency.

---

## Rejection & Cooldown

When a vendor rejects a trip:
1. The current allocation is marked `REJECTED`.
2. Fairness counters (`bucket.totalTrips`, `vendor.allocatedTrips`) are fully reverted.
3. Capacity is restored.
4. A **per-trip** cooldown is recorded for 15 minutes (the vendor cannot receive *this specific trip* again during cooldown).
5. Allocation re-runs immediately for the same trip, selecting the next eligible vendor.

The cooldown is **trip-scoped**: the vendor remains eligible for other trips during the cooldown window.

---

## Authentication & Authorization

| Role | Capabilities |
|---|---|
| `USER` | Allocate trips, reject trips |
| `ADMIN` | Everything USER can do, plus: manage vendors/zones/shares/capacity, view fairness reports |

- Passwords are BCrypt-hashed and never returned in responses.
- JWTs are signed with HS512 and expire in 24 hours (configurable via `APP_JWT_EXPIRATION`).
- All registration creates USER accounts. ADMIN accounts must be seeded directly.

---

## Redis Caching

Only **read-heavy, slow-changing configuration** is cached. Dynamic allocation state is never cached.

| Cache Key | Data | TTL |
|---|---|---|
| `vendors` | Active vendor list | 10 min |
| `activeZones` | Active zone list | 10 min |
| `vendorZoneShares` | Share configurations | 10 min |
| `dailyReports` | Daily fairness report | 5 min |
| `monthlyReports` | Monthly fairness report | 5 min |

Cache is evicted immediately on any mutation (create/update/toggle/configure).

**Redis is never a source of truth.** MySQL always is. If Redis is down, every cache operation falls back to MySQL transparently.

---

## Fairness Reporting

| Endpoint | Description |
|---|---|
| `GET /api/reports/daily?date=YYYY-MM-DD` | Per-vendor daily metrics |
| `GET /api/reports/monthly?year=YYYY&month=MM` | Per-vendor monthly metrics |

Each report row includes: vendor code/name, zone, trip type, promised %, actual trips, actual %, expected trips, running shortfall (bp), current capacity.

Reports are **read-only** and computed dynamically. No report data is permanently stored.

---

## Monitoring & Error Handling

### Actuator Endpoints (public)

| Endpoint | Description |
|---|---|
| `/actuator/health` | Application and dependency health (DB, Redis) |
| `/actuator/info` | Application name and version |
| `/actuator/metrics` | JVM and HTTP metrics |

Sensitive endpoints (`/actuator/env`, `/actuator/beans`, etc.) are explicitly excluded.

### Error Response Format

All errors return a consistent JSON body:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "No eligible vendor found for allocation",
  "path": "/api/trips/allocate"
}
```

| HTTP Status | Condition |
|---|---|
| 400 | Validation error or business rule violation |
| 401 | Missing, invalid, or expired JWT |
| 403 | Insufficient role |
| 404 | Resource not found |
| 500 | Unexpected server error (no stack trace exposed) |

### Logging
- Allocation success/failure: `INFO`/`WARN`
- Rejection: `INFO`
- Business violations: `WARN`
- Auth failures: `WARN`
- Unexpected exceptions: `ERROR` with full stack trace in server logs (never in API response)
- Passwords, tokens, and secrets are never logged.

---

## Database / ER Overview

Key tables:

| Table | Purpose |
|---|---|
| `vendors` | Vendor master data |
| `zones` | Distance-based zone definitions |
| `vendor_zone_shares` | Contracted share (basis points) per vendor/zone/type |
| `allocation_buckets` | Cumulative trip counter per zone/type |
| `vendor_allocation_state` | Per-vendor allocated trips in a bucket |
| `vendor_capacity` | Available trip capacity per vendor |
| `trips` | Trip records |
| `trip_allocations` | Which vendor received which trip |
| `trip_rejections` | Rejection history + cooldown expiry |
| `users` | Authentication accounts |

See [`docs/ER_DIAGRAM.md`](docs/ER_DIAGRAM.md) for the full entity-relationship diagram.

---

## API & Swagger

**Swagger UI:** [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html)

**OpenAPI JSON:** [`http://localhost:8080/v3/api-docs`](http://localhost:8080/v3/api-docs)

**To use protected endpoints in Swagger UI:**
1. Call `POST /api/auth/login` with your credentials.
2. Copy the `token` field from the response.
3. Click **Authorize** (top right) and paste the token (without `Bearer `).

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (dev default in properties) | HS512 signing secret — **always override in production** |
| `JWT_EXPIRATION` | `86400000` | JWT lifetime in milliseconds (24 h) |
| `SPRING_DATASOURCE_URL` | H2 in-memory (tests) | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | — | MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | — | MySQL password |
| `SPRING_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_REDIS_PORT` | `6379` | Redis port |

---

## How to Run Locally

**Prerequisites:** Java 21, Maven 3.9+, MySQL 8, Redis (optional)

```bash
# Clone the repo
cd backend

# Set environment variables (or edit application.properties)
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/fairtripdb
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=yourpassword
export JWT_SECRET=your-very-long-secret-key

# Run
mvn spring-boot:run
```

The application starts on port **8080**.

Swagger UI: http://localhost:8080/swagger-ui.html

---

## How to Run Tests

```bash
cd backend
mvn test
```

The test suite uses H2 (in-memory) and does not require MySQL or Redis to be running. All 39 tests pass without any external services.

---

## Assumptions

1. **Zone matching** uses the trip `distance` field. A trip must fall within exactly one active zone's `[minDistance, maxDistance)` range.
2. **Share configuration must sum to 10 000 bp** per bucket. Partial shares are not supported.
3. **Missing VendorCapacity = unlimited capacity** — documented intentional behavior.
4. **Registration always creates USER accounts.** ADMINs must be seeded.
5. **Carry-forward is permanent.** Fairness counters are never reset (by design — shortfalls carry forward indefinitely).
6. **Trip distance is not stored** on the trip entity after allocation. Rejection re-runs use the zone's `minDistance` as a representative value.

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Integer basis points | Eliminates floating-point rounding errors in fairness arithmetic |
| Pessimistic locking | Prevents double-allocation under concurrent requests without application-level distributed locks |
| Redis as cache-only | Keeps complexity low; MySQL is always authoritative |
| Per-trip rejection cooldown | Prevents a vendor from permanently blocking a trip while still being eligible for other trips |
| No token refresh | Stateless simplicity; 24-hour expiry is sufficient for the use case |
| No report persistence | Reports are always fresh from DB; avoids a separate reporting store |
| `@Transactional(readOnly=true)` on reports | Keeps the JPA session open for lazy-loaded associations during report generation |

---

## Demo Flow

```
1. Register: POST /api/auth/register  { "email": "admin@demo.com", "password": "Pass123!" }
2. Login:    POST /api/auth/login     → copy token
3. Create zone:    POST /api/zones    { "code":"Z1", "minDistance": 0, "maxDistance": 10, ... }
4. Create vendors: POST /api/vendors  (V-ALPHA, V-BETA, V-GAMMA)
5. Configure shares: PUT /api/zones/{id}/shares
   { "tripType":"NORMAL", "shares": [
       {"vendorId":1, "basisPoints":5000},
       {"vendorId":2, "basisPoints":3000},
       {"vendorId":3, "basisPoints":2000}
   ]}
6. Allocate trips: POST /api/trips/allocate  { "externalTripId":"T001", "distance":"5.0", "tripType":"NORMAL" }
   → Repeate 10 times with unique externalTripId
7. View report: GET /api/reports/daily?date=TODAY
   → V-ALPHA: 5 trips (50%), V-BETA: 3 trips (30%), V-GAMMA: 2 trips (20%)
```
