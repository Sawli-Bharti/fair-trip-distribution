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
For a vendor in a bucket, the shortfall represents how many basis points of a trip they are owed:
shortfall = (bucket.totalTrips * targetBasisPoints) - (vendor.allocatedTrips * 10000)

* Large positive shortfall = vendor is most behind their promised share.
* Negative shortfall = vendor is ahead of their promised share.
* Calculations use safe integer/long arithmetic without floating-point conversion.

## Deterministic Tie-breaking
When two or more eligible vendors have exactly the same shortfall:
1. **Priority**: Vendor with lower priority value wins.
2. **Vendor ID**: If priority is identical, the vendor with the lower Vendor ID wins.
Randomness is never used. The same state always yields the exact same allocation.

## Carry-Forward and State Updates
The allocation state is strictly **cumulative**. Bucket total trips and vendor allocated trips are never reset.
This ensures continuous carry-forward fairness across days.

Update execution strictly follows:
1. Calculate shortfalls based on current totals.
2. Select the most-owed eligible vendor.
3. Increment bucket 	otalTrips.
4. Increment vendor llocatedTrips.
5. Persist via a single atomic transaction.

## Complexity
The expected straightforward complexity is **O(V)** per trip, where V is the number of candidate vendors. Sorting/scanning shortfalls is bounded by the number of active vendors.

## Assumptions
* Zone matching evaluates based on distance: minDistance <= distance < maxDistance. (FAR zone has no upper bound, i.e., maxDistance is null).
* Concurrent allocations on the same bucket are serialized using pessimistic row locking to prevent race conditions in counters.
