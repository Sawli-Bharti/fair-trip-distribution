# AI Context: Fair Trip Distribution

## Project Purpose
A backend case-study project to fairly distribute cab trips among multiple vendors according to their contracted shares. Handles zones, trip types, capacity, rejection, carry-forward, concurrency, and deterministic allocation.

## Tech Stack
* Java 21
* Spring Boot
* Maven
* MySQL
* Redis
* Spring Data JPA
* Spring Security
* Spring Web
* Validation
* Actuator

## Architecture
* MVC/layered architecture: Controller -> Service -> Repository -> MySQL.
* Business logic kept out of controllers; DB access inside repositories; Allocation logic inside services.

## Core Algorithm Decisions
1. **Algorithm:** Most-Owed-First with carry-forward.
2. **Bucket:** Zone + Trip Type.
3. **Trip Types:** NORMAL, ESCORT.
4. **Fairness Representation:** Integer basis points (100% = 10,000 bp). No floating-point.
5. **Shortfall:** `(bucket.totalTrips * targetBasisPoints) / (vendor.allocatedTrips * 10000)`
6. **Selection:** Vendor with highest eligible shortfall.
7. **Tie-breaking:** Deterministic.

## Database Decisions
* MySQL is the authoritative source of truth.
* Redis is a cache/performance layer only.
* Concurrency handled via MySQL transactions and row-level locking.
* **Phase 1 Schema Decisions:**
  * Created standard `@Entity` classes for User, Vendor, Zone, VendorZoneShare, Trip, VendorCapacity, AllocationBucket, VendorAllocationState, TripAllocation, TripRejection.
  * Used `int` basis points for fairness calculations (no floats).
  * Unique constraints applied (e.g. `externalTripId`, `VendorZoneShare`, `AllocationBucket`).
  * Repositories set up to support future row-locking without implementing it prematurely.

## Important Constraints
* Capacity: Vendor with no capacity cannot receive a trip.
* Rejection: Rejected vendor cannot receive same trip during cooldown.
* Carry-forward: Allocation state persists across days.
* Idempotency: Same external trip not allocated twice.

## Project Phases
* **Phase 0:** Project foundation (Maven Spring Boot init, structure, docs) - Completed.
* **Phase 1:** Database entities and repositories - Completed.
* **Phase 2:** Vendor and Zone Configuration - Completed.
* **Phase 3:** Trip Allocation Engine - Completed.
  * Core allocation service implementing Most-Owed-First shortfall calculation using strict integer math (`long`).
  * Idempotency checking via `externalTripId`.
  * Deterministic tie-breaking on `priority` and `vendorId`.
  * Atomic `@Transactional` persist containing pessimistic write locks to safely carry forward running totals in a concurrent environment.
  * Extensively tested using multi-vendor share combinations.
* **Phase 4:** Rejections, Cooldown, and Capacity Consumption - Completed.
  * Vendor Capacity acts as a strict eligibility constraint. Consume capacity upon allocation under pessimistic lock.
  * Rejection completely reverses allocation state counters correctly and injects a per-trip cooldown (does not block unrelated trips).
  * Fully atomic and heavily concurrency-tested.
* **Phase 5 (Current):** Authentication, Caching, and Dashboards (if requested).

## Important Decisions
* `maxDistance` on Zone was made strictly nullable to cleanly accommodate open-ended zones (e.g., FAR).
* `priority` field was added to Vendor to accommodate the deterministic priority-based tie-breaking rule.
