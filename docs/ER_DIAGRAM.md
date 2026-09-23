# Database Entity Relationship Diagram

## Entity Relationship Diagram

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR passwordHash
        VARCHAR role
        DATETIME createdAt
    }

    vendors {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
        BOOLEAN isActive
        INT priority
        DATETIME createdAt
    }

    zones {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
        DECIMAL minDistance
        DECIMAL maxDistance
        BOOLEAN isActive
    }

    vendor_zone_shares {
        BIGINT id PK
        BIGINT vendor_id FK
        BIGINT zone_id FK
        VARCHAR trip_type
        INT targetBasisPoints
    }

    allocation_buckets {
        BIGINT id PK
        BIGINT zone_id FK
        VARCHAR trip_type
        BIGINT totalTrips
    }

    vendor_allocation_state {
        BIGINT id PK
        BIGINT bucket_id FK
        BIGINT vendor_id FK
        BIGINT allocatedTrips
    }

    vendor_capacity {
        BIGINT id PK
        BIGINT vendor_id FK
        INT totalCapacity
        INT availableCapacity
        DATETIME lastUpdated
    }

    trips {
        BIGINT id PK
        VARCHAR external_trip_id UK
        BIGINT zone_id FK
        VARCHAR trip_type
        VARCHAR status
        DATETIME createdAt
    }

    trip_allocations {
        BIGINT id PK
        BIGINT trip_id FK
        BIGINT vendor_id FK
        VARCHAR status
        DATETIME allocatedAt
    }

    trip_rejections {
        BIGINT id PK
        BIGINT trip_id FK
        BIGINT vendor_id FK
        DATETIME rejectedAt
        DATETIME cooldownUntil
        VARCHAR reason
    }

    vendors ||--o| vendor_capacity : "has"
    vendors ||--o{ vendor_zone_shares : "contracted in"
    vendors ||--o{ vendor_allocation_state : "tracked by"
    vendors ||--o{ trip_allocations : "receives"
    vendors ||--o{ trip_rejections : "rejects"

    zones ||--o{ vendor_zone_shares : "configured for"
    zones ||--o{ allocation_buckets : "groups into"
    zones ||--o{ trips : "belongs to"

    allocation_buckets ||--o{ vendor_allocation_state : "contains"

    trips ||--|| trip_allocations : "allocated via"
    trips ||--o{ trip_rejections : "rejected by"
```

## Unique Constraints

| Table | Unique Constraint |
|---|---|
| `users` | `email` |
| `vendors` | `code` |
| `zones` | `code` |
| `vendor_zone_shares` | `(vendor_id, zone_id, trip_type)` |
| `allocation_buckets` | `(zone_id, trip_type)` |
| `vendor_allocation_state` | `(bucket_id, vendor_id)` |
| `vendor_capacity` | `vendor_id` (one record per vendor) |
| `trips` | `external_trip_id` |
| `trip_allocations` | `trip_id` (one allocation per trip) |

## Key Relationships

- **`vendor_zone_shares`** — Defines the contracted share (in basis points) for each `Vendor` × `Zone` × `TripType` combination. Shares for the same zone and trip type must sum to exactly 10 000 bp.
- **`allocation_buckets`** — One cumulative counter per `Zone` × `TripType`. Tracks total trips ever allocated in that stream. Never reset.
- **`vendor_allocation_state`** — One row per vendor per bucket. Tracks how many trips that vendor has been allocated in that stream. The shortfall calculation compares this against the bucket total.
- **`vendor_capacity`** — Optional one-to-one with `vendors`. If absent, the vendor is treated as having unlimited capacity.
- **`trip_allocations`** — One-to-one with `trips`. Records which vendor received the trip and the current allocation status (`SUCCESS` / `REJECTED`).
- **`trip_rejections`** — Many-to-one with both `trips` and `vendors`. Each row records a rejection event with a `cooldownUntil` timestamp. The cooldown is scoped to the specific `(vendor, trip)` pair — the vendor remains eligible for other trips.

## Database Source of Truth

MySQL is the authoritative source of truth. Redis is used only as a performance cache for slow-changing configuration data (`vendors`, `zones`, `vendor_zone_shares`). Dynamic allocation state is never cached.
