# Allocation Algorithm

## Core Concept
The allocation algorithm uses a **Most-Owed-First with carry-forward** approach.

## Allocation Bucket
Allocations are tracked within a specific **Bucket** defined by:
* Zone
* Trip Type (NORMAL, ESCORT)

## Fairness Representation
Fairness is tracked using integer basis points to ensure precision without floating-point errors.
* 100% share = 10,000 basis points.

## Shortfall Calculation
The core metric for deciding the next vendor is the **shortfall**.

```
shortfall = (bucket.totalTrips * targetBasisPoints) / (vendor.allocatedTrips * 10000)
```

## Selection Criteria
1. The vendor with the highest eligible shortfall is selected.
2. **Tie-breaking** must be deterministic (e.g., using vendor ID or creation timestamp).
3. **Capacity:** A vendor with no available capacity cannot receive a trip.
4. **Rejection:** A rejected vendor cannot receive the same trip during its cooldown period.

## State Management
* **Carry-forward:** Allocation state persists across days.
* **Idempotency:** The same external trip must not be allocated twice.
* **Concurrency:** MySQL transactions and appropriate row-level locking will protect allocation state and vendor capacity.
