# FirstClub Membership Program - Sequence Diagrams

## Sequence Diagram 1: Subscribe Operation

```
Client              Controller           Service              LockManager         Repository
  │                    │                   │                      │                   │
  ├─ POST /subscribe──>│                   │                      │                   │
  │                    │                   │                      │                   │
  │                    ├─ subscribe()───────>│                      │                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ executeWithUserLock─>│                   │
  │                    │                   │                      │                   │
  │                    │                   │<─ Lock acquired ─────│                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ findActiveByUserId────────────────────>│
  │                    │                   │<─ Optional.empty() ──────────────────────│
  │                    │                   │                      │                   │
  │                    │                   ├─ getPlanOrThrow()    │                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ getTierOrThrow()    │                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ calculateExpiry()   │                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ new UserMembership()│                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ save(membership)─────────────────────>│
  │                    │                   │<─ saved membership ──────────────────────│
  │                    │                   │                      │                   │
  │                    │                   │<─ Release Lock ──────│                   │
  │                    │                   │                      │                   │
  │                    │<─ MembershipResponse ────────────────────────────────────────│
  │<─ 201 Created ─────│                   │                      │                   │
  │                    │                   │                      │                   │
```

## Sequence Diagram 2: Tier Upgrade

```
Client              Controller           Service              LockManager         Repository      Strategy
  │                    │                   │                      │                   │                 │
  ├─ POST /upgrade────>│                   │                      │                   │                 │
  │                    │                   │                      │                   │                 │
  │                    ├─ upgradeTier()───>│                      │                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ executeWithUserLock─>│                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   │<─ Lock acquired ─────│                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ findActiveByUserId────────────────────>│                 │
  │                    │                   │<─ UserMembership ──────────────────────│                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ getTierOrThrow()    │                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ currentTier.upgrade()─>│                 │                 │
  │                    │                   │<─ New TierLevel ─────│                 │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ getTierByLevelOrThrow()               │                 │
  │                    │                   │<─ New Tier ──────────│                 │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ membership.setTierId()               │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ RETRY LOOP (max 3x)─────────────────────────────────────│
  │                    │                   │  saveWithVersionCheck()               │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ [if version OK]     │                   │                 │
  │                    │                   │    save(membership)────────────────────>│                 │
  │                    │                   │<─ Optional<Saved> ──────────────────────│                 │
  │                    │                   │                      │                   │                 │
  │                    │                   │<─ Release Lock ──────│                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │<─ MembershipResponse ────────────────────────────────────────────────────────│
  │<─ 200 OK ──────────│                   │                      │                   │                 │
  │                    │                   │                      │                   │                 │
```

## Sequence Diagram 3: Tier Evaluation & Auto-Upgrade

```
Client              Controller           Service              LockManager         Repository      Strategy
  │                    │                   │                      │                   │                 │
  ├─ POST /eval-tier──>│                   │                      │                   │                 │
  │                    │                   │                      │                   │                 │
  │                    ├─ evaluateTier()──>│                      │                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ executeWithUserLock─>│                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   │<─ Lock acquired ─────│                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ new TierEvaluationContext()           │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ evaluationStrategy.evaluateEligibleTier()─────────────>│
  │                    │                   │                      │                   │                 │
  │                    │                   │                      │                   │  Evaluate based on:
  │                    │                   │                      │                   │  - orderCount
  │                    │                   │                      │                   │  - monthlyOrderValue
  │                    │                   │                      │                   │  - cohort
  │                    │                   │<─ TierLevel ─────────────────────────────────────────────│
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ findByLevel()────────────────────────>│                 │
  │                    │                   │<─ MembershipTier ──────────────────────│                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ findActiveByUserId()────────────────>│                 │
  │                    │                   │<─ UserMembership ──────────────────────│                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ getTierOrThrow()    │                   │                 │
  │                    │                   │ (current tier)       │                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ eligibleTier > currentTier?           │                 │
  │                    │                   │ → YES (PLATINUM > SILVER)              │                 │
  │                    │                   │                      │                   │                 │
  │                    │                   ├─ RETRY LOOP (max 3x)──────────────────────────────────── │
  │                    │                   │  membership.setTierId(eligibleTier)    │                 │
  │                    │                   │  saveWithVersionCheck()───────────────>│                 │
  │                    │                   │<─ Optional<Saved> ──────────────────────│                 │
  │                    │                   │                      │                   │                 │
  │                    │                   │<─ Release Lock ──────│                   │                 │
  │                    │                   │                      │                   │                 │
  │                    │<─ TierEvaluationResponse ─────────────────────────────────────────────────────│
  │                    │  {eligibleTier, previousTier, appliedTier, tierUpgraded, membership}         │
  │<─ 200 OK ──────────│                   │                      │                   │                 │
  │                    │                   │                      │                   │                 │
```

## Sequence Diagram 4: Get Membership (with auto-expiry check)

```
Client              Controller           Service              Repository         Model
  │                    │                   │                      │                   │
  ├─ GET /users/123──>│                   │                      │                   │
  │                    │                   │                      │                   │
  │                    ├─ getMembership()─>│                      │                   │
  │                    │                   │                      │                   │
  │                    │                   ├─ findActiveByUserId────────────────────>│
  │                    │                   │<─ Optional<UserMembership>────────────│
  │                    │                   │                      │                   │
  │                    │                   ├─ membership.markExpiredIfNeeded()──────────────────>│
  │                    │                   │                      │                   │
  │                    │                   │                      │                   ├─ Check if now > expiryDate
  │                    │                   │                      │                   │
  │                    │                   │                      │                   ├─ If expired:
  │                    │                   │                      │                   │   status = EXPIRED
  │                    │                   │                      │                   │
  │                    │                   ├─ Check status == EXPIRED?               │
  │                    │                   │                      │                   │
  │                    │                   ├─ [if expired]        │                   │
  │                    │                   │  save(membership)────────────────────>│
  │                    │                   │                      │                   │
  │                    │                   ├─ Throw ResourceNotFound exception      │
  │                    │                   │                      │                   │
  │                    │<─ Exception ──────│                      │                   │
  │<─ 404 Not Found ───│                   │                      │                   │
  │                    │                   │                      │                   │
```

## Sequence Diagram 5: Concurrent Requests (Different Users)

```
User A                User B
  │                     │
  ├─ POST /subscribe   ├─ POST /subscribe
  │    for userA       │    for userB
  │                     │
  ├─ Lock for A ──────>│
  │                     │
  │ Process A...       ├─ Lock for B ──────>
  │                     │
  │ Save for A...      │ Process B...
  │                     │
  │ Unlock A ◄─────────┤
  │                     │ Save for B...
  │                     │
  │                     ├─ Unlock B ◄──────
  │                     │
  Result: PARALLEL EXECUTION!
```

## Sequence Diagram 6: Concurrent Requests (Same User - Race Condition Prevention)

```
Request 1              Request 2           LockManager        Repository
(upgrade tier)         (downgrade tier)
  │                      │                    │                  │
  ├─ Lock User A────────>│                    │                  │
  │                      │                    │                  │
  │ Execute...           ├─ Wait for lock    │                  │
  │                      │ (queued)           │                  │
  │ Read v=5             │                    │                  │
  │ Modify               │                    │                  │
  │ saveWithVersionCheck()─────────────────────────────────────>│
  │<─ Saved (v=6) ──────│                    │                  │
  │                      │                    │                  │
  │ Unlock User A◄──────┤                    │                  │
  │                      │                    │                  │
  │                      ├─ Lock acquired ───>│                  │
  │                      │                    │                  │
  │                      │ Read v=6           │                  │
  │                      │ (gets latest!)     │                  │
  │                      │ Modify             │                  │
  │                      │ saveWithVersionCheck()─────────────────────>│
  │                      │<─ Saved (v=7) ────│                  │
  │                      │                    │                  │
  │                      ├─ Unlock User A◄──│                  │
  │                      │                    │                  │
  Result: NO RACE CONDITION - Both operations serialized!
```

## Sequence Diagram 7: Optimistic Lock Failure & Retry

```
Thread 1 (Initial)      Thread 2 (Interfering)    Repository      Thread 1 (Retry)
  │                           │                        │               │
  ├─ Read v=5                 │                        │               │
  │ Modify tier               │                        │               │
  │ saveWithVersionCheck()────────────────────────────>│               │
  │                           │                        │               │
  │                           ├─ Read v=5              │               │
  │                           │ Modify plan            │               │
  │                           │ saveWithVersionCheck()──────┐          │
  │                           │<─ OK (v=6) ─────────────────┤         │
  │                           │                        │   │          │
  │<─ FAIL (v != 5) ──────────│                        │   │          │
  │                           │                        │   │          │
  ├─ Retry 1 ────────────────────────────────────────────>│          │
  │ Read v=6 (latest!)        │                        │           │
  │ Modify tier               │                        │           │
  │ saveWithVersionCheck()────────────────────────────────────────>│
  │<─ OK (v=7) ───────────────────────────────────────────────────│
  │                           │                        │           │
  Result: AUTOMATIC RETRY succeeds with latest version!
```

## Sequence Diagram 8: Get Catalog

```
Client              Controller           Service              Repository
  │                    │                   │                      │
  ├─ GET /catalog────>│                   │                      │
  │                    │                   │                      │
  │                    ├─ getCatalog()───>│                      │
  │                    │                   │                      │
  │                    │                   ├─ findAllActive()────────────>│
  │                    │                   │<─ List<Plan> ────────────────│
  │                    │                   │                      │
  │                    │                   ├─ findAll()─────────────────>│
  │                    │                   │<─ List<Tier> ────────────────│
  │                    │                   │                      │
  │                    │                   ├─ Sort by duration   │
  │                    │                   ├─ Sort by tier level │
  │                    │                   │                      │
  │                    │                   ├─ Map to responses   │
  │                    │                   │                      │
  │                    │<─ CatalogResponse ─────────────────────│
  │<─ 200 OK ──────────│ {plans, tiers}    │                      │
  │                    │                   │                      │
```

## Sequence Diagram 9: Cancel Membership

```
Client              Controller           Service              LockManager      Repository
  │                    │                   │                      │                  │
  ├─ POST /cancel────>│                   │                      │                  │
  │                    │                   │                      │                  │
  │                    ├─ cancel()────────>│                      │                  │
  │                    │                   │                      │                  │
  │                    │                   ├─ executeWithUserLock─>│                  │
  │                    │                   │                      │                  │
  │                    │                   │<─ Lock acquired ─────│                  │
  │                    │                   │                      │                  │
  │                    │                   ├─ findActiveByUserId──────────────────>│
  │                    │                   │<─ UserMembership ──────────────────────│
  │                    │                   │                      │                  │
  │                    │                   ├─ Check isActive()    │                  │
  │                    │                   │                      │                  │
  │                    │                   ├─ membership.cancel() │                  │
  │                    │                   │ (status=CANCELLED)   │                  │
  │                    │                   │                      │                  │
  │                    │                   ├─ RETRY LOOP          │                  │
  │                    │                   │  saveWithVersionCheck()──────────────>│
  │                    │                   │<─ Optional<Saved> ──────────────────────│
  │                    │                   │                      │                  │
  │                    │                   │<─ Release Lock ──────│                  │
  │                    │                   │                      │                  │
  │                    │<─ MembershipResponse ──────────────────────────────────────│
  │<─ 200 OK ──────────│ (status=CANCELLED)│                      │                  │
  │                    │                   │                      │                  │
```

---

## Key Observations from Sequences

### ✅ Locking
- Every mutation operation acquires per-user lock
- Lock is released after operation completes
- Prevents concurrent modifications for same user

### ✅ Retries
- saveWithVersionCheck() failures trigger retry
- Automatic re-read of latest version
- Max 3 attempts before throwing exception

### ✅ Validation
- Active membership check before modifications
- Tier existence validation
- Status checks (ACTIVE, EXPIRED, CANCELLED)

### ✅ Concurrency Safety
- Same user: Serialized via locking
- Different users: Fully concurrent
- Version conflicts handled gracefully


