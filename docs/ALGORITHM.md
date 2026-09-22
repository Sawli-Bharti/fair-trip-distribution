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

## Eligibility Constraints
* **Capacity**: A vendor must have vailableCapacity > 0 (if tracked) to be evaluated. **Note: If a vendor has no VendorCapacity record, they are treated as having unlimited capacity.**
* **Cooldown**: A vendor currently in an active rejection cooldown cannot be selected for any trip.
* If a vendor is owed but ineligible, the algorithm proceeds to evaluate the next eligible vendor in line.

## Shortfall Calculation
For an eligible vendor in a bucket, the shortfall represents how many basis points of a trip they are owed:
shortfall = (bucket.totalTrips * targetBasisPoints) - (vendor.allocatedTrips * 10000)

* Large positive shortfall = vendor is most behind their promised share.
* Negative shortfall = vendor is ahead of their promised share.
* Calculations use safe integer/long arithmetic without floating-point conversion.

## Deterministic Tie-breaking
When two or more eligible vendors have exactly the same shortfall:
1. **Priority**: Vendor with lower priority value wins.
2. **Vendor ID**: If priority is identical, the vendor with the lower Vendor ID wins.
Randomness is never used. The same state always yields the exact same allocation.

## Rejection and Reallocation
If a vendor rejects a trip offer:
1. Their vailableCapacity is restored.
2. Fairness counters (ucket.totalTrips and endorState.allocatedTrips) are fully reverted.
3. The vendor is placed in a global cooldown block.
4. The trip immediately re-runs allocation targeting the next most eligible vendor.

## Carry-Forward and State Updates
The allocation state is strictly **cumulative**. Bucket total trips and vendor allocated trips are never reset.
This ensures continuous carry-forward fairness across days.

Update execution strictly follows:
1. Calculate shortfalls based on current totals among eligible vendors.
2. Select the most-owed eligible vendor.
3. Increment bucket 	otalTrips.
4. Increment vendor llocatedTrips.
5. Decrement vendor vailableCapacity.
6. Persist via a single atomic transaction.

## Complexity
The expected straightforward complexity is **O(V)** per trip, where V is the number of candidate vendors. Sorting/scanning shortfalls is bounded by the number of active vendors.

## Concurrency Strategy
* Concurrent allocations on the same bucket serialize counter updates using pessimistic row locking (@Lock(LockModeType.PESSIMISTIC_WRITE)).
* VendorCapacity rows for eligible candidate vendors are fetched under a batch PESSIMISTIC_WRITE lock guaranteeing single-slot safe consumption under heavy concurrency without deadlocks (ordered inherently by endorId).
