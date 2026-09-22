# Database Entity Relationship Diagram

## Planned Entities

* **users**: System users and authentication.
* **vendors**: Cab vendor details.
* **zones**: Geographic zones for trips.
* **vendor_zone_shares**: Contracted basis points (fairness share) for each vendor per zone and trip type.
* **trips**: Record of all trips (pending, allocated, completed).
* **vendor_capacity**: Current available capacity for each vendor.
* **allocation_buckets**: Aggregated metrics per Zone + Trip Type.
* **vendor_allocation_state**: Current allocated trips and state per vendor per bucket (for shortfall calculation).
* **trip_allocations**: Record of which trip was allocated to which vendor.
* **trip_rejections**: Record of trips rejected by vendors, including cooldown timestamps.

## Database Source of Truth
MySQL is the authoritative source of truth. Redis is only used as a performance/cache layer.
