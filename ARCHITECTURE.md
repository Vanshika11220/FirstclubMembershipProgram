# FirstClub Membership Program - UML Diagram & Architecture Documentation

## Overview
This document describes the complete UML diagram and architecture of the FirstClub Membership Program, a Spring Boot application that manages user memberships with tiering, benefits, and concurrency control.

---

## UML Diagram

A comprehensive PlantUML diagram has been created in `UML_DIAGRAM.puml` which visualizes the entire project structure.

### How to View the Diagram

1. **Using PlantUML Online**: Go to https://www.plantuml.com/plantuml/uml/ and paste the content of `UML_DIAGRAM.puml`
2. **Using IDE Plugin**: Install PlantUML plugin in your IDE
3. **Using Command Line**: 
   ```bash
   plantuml UML_DIAGRAM.puml
   ```

---

## Architecture Overview

### 1. **Domain Layer** (`com.firstclub.membership.domain`)

#### Models (`domain/model/`)
- **UserMembership**: Core entity representing a user's active membership
  - Immutable ID and user ID
  - Mutable tier/plan references
  - Version tracking for optimistic locking
  - Status tracking (ACTIVE, CANCELLED, EXPIRED)
  - Methods to check validity and manage lifecycle

- **MembershipPlan**: Value object defining subscription plans
  - Immutable design
  - Duration, price, and active status
  - Factory method pattern (`of()`)

- **MembershipTier**: Value object defining membership tiers
  - Immutable design
  - Associated benefits list
  - Tier level (SILVER, GOLD, PLATINUM)

- **Benefit**: Value object for individual benefits
  - Benefit type and description
  - Configuration map for flexible attributes
  - Immutable with defensive copying

#### Enums (`domain/enums/`)
- **MembershipStatus**: `ACTIVE`, `CANCELLED`, `EXPIRED`
- **TierLevel**: `SILVER(1)`, `GOLD(2)`, `PLATINUM(3)`
  - Methods: `upgrade()`, `downgrade()`, `isHigherThan()`, `isLowerThan()`
- **BenefitType**: `FREE_DELIVERY`, `DISCOUNT_PERCENTAGE`, `EXCLUSIVE_DEALS`, `EARLY_ACCESS_SALES`, `PRIORITY_SUPPORT`
- **PlanDuration**: `MONTHLY(1)`, `QUARTERLY(3)`, `YEARLY(12)`
  - Method: `addTo(Instant)` for expiry calculation

---

### 2. **Repository Layer** (`com.firstclub.membership.repository`)

Three repository interfaces manage persistence:

- **UserMembershipRepository**
  - `findActiveByUserId(userId)`: Get active membership for a user
  - `findById(id)`: Get membership by ID
  - `save(membership)`: Persist membership
  - `saveWithVersionCheck(membership)`: Optimistic locking for concurrent updates

- **MembershipPlanRepository**
  - `findAllActive()`: List all active plans
  - `findById(id)`: Get plan by ID
  - `save(plan)`: Persist plan

- **MembershipTierRepository**
  - `findAll()`: List all tiers
  - `findById(id)`: Get tier by ID
  - `findByLevel(level)`: Get tier by tier level
  - `save(tier)`: Persist tier

---

### 3. **Service Layer** (`com.firstclub.membership.service`)

#### SubscriptionService
**Core business logic for membership management**
- `subscribe(request)`: Create new membership
  - Validates no active membership exists
  - Creates UserMembership with plan and tier
  - Uses per-user locking via UserLockManager
  
- `getMembership(userId)`: Retrieve active membership
  - Marks expired memberships automatically
  
- `upgradeTier(userId)` / `downgradeTier(userId)`: Tier modification
  - Uses optimistic locking with retries
  - Validates tier availability
  
- `cancel(userId)`: Cancel membership
  - Updates status to CANCELLED
  - Uses retry mechanism for concurrent scenarios

#### MembershipCatalogService
**Manages membership catalog data**
- `getCatalog()`: Returns all active plans and tiers sorted
- `getPlanOrThrow()`: Retrieve and validate plan exists
- `getTierOrThrow()`: Retrieve and validate tier exists
- `getTierByLevelOrThrow()`: Get tier by level enum

#### TierEvaluationService
**Evaluates and applies tier upgrades based on user behavior**
- `evaluateAndApply(userId, request)`: 
  - Uses TierEvaluationStrategy to determine eligible tier
  - Only upgrades (never downgrades)
  - Uses optimistic locking with retries
  - Returns detailed evaluation response

---

### 4. **Strategy Pattern** (`com.firstclub.membership.strategy`)

#### TierEvaluationStrategy (Interface)
Defines contract for tier evaluation logic

#### ConfigurableTierEvaluationStrategy (Implementation)
**Rules for tier qualification:**
- **PLATINUM**: 
  - 15+ orders, OR
  - ≥15,000 monthly value, OR
  - Cohort in [PREMIUM, VIP]
  
- **GOLD**: 
  - 5+ orders, OR
  - ≥5,000 monthly value
  
- **SILVER**: Default fallback

#### TierEvaluationContext (Data Class)
Immutable record containing evaluation criteria:
- `userId`, `orderCount`, `monthlyOrderValue`, `cohort`

---

### 5. **Factory Pattern** (`com.firstclub.membership.factory`)

#### BenefitFactory
Static factory for creating consistently configured benefits:
- `freeDelivery(minOrderValue)`: FREE_DELIVERY benefit
- `discountPercentage(percentage)`: DISCOUNT_PERCENTAGE benefit
- `exclusiveDeals()`: EXCLUSIVE_DEALS benefit
- `earlyAccessSales()`: EARLY_ACCESS_SALES benefit (24h early)
- `prioritySupport()`: PRIORITY_SUPPORT benefit (15min response)

---

### 6. **Concurrency Management** (`com.firstclub.membership.concurrency`)

#### UserLockManager
**Per-user fine-grained locking strategy**
- Maintains `ConcurrentHashMap<String, ReentrantLock>`
- `executeWithUserLock(userId, action)`: Generic method that:
  - Acquires per-user lock
  - Executes action
  - Releases lock
  - Cleans up if no queued threads
  
**Benefits**: 
- Serializes operations for same user (prevents race conditions)
- Allows concurrent operations across different users
- Automatic cleanup of idle locks

---

### 7. **DTO Layer** (`com.firstclub.membership.dto`)

#### Request DTOs
- **SubscribeRequest**: `userId`, `planId`, `tierId` (validated with @NotBlank)
- **TierEvaluationRequest**: `orderCount`, `monthlyOrderValue`, `cohort` (validated)

#### Response DTOs
- **MembershipResponse**: Complete membership snapshot with plan/tier details
- **TierResponse**: Tier details with associated benefits
- **PlanResponse**: Plan details (id, name, duration, price)
- **BenefitResponse**: Benefit details with configuration
- **CatalogResponse**: Container for plans and tiers lists
- **TierEvaluationResponse**: Evaluation result with:
  - `eligibleTier`: What tier user qualifies for
  - `previousTier`: User's current tier
  - `appliedTier`: Tier after evaluation
  - `tierUpgraded`: Boolean flag
  - `membership`: Updated membership snapshot

---

### 8. **Exception Handling** (`com.firstclub.membership.exception`)

#### MembershipException (Base)
- Custom exception with error code
- Parent for domain-specific exceptions

#### Derived Exceptions
- **ResourceNotFoundException**: Extends MembershipException
  - Error code: "NOT_FOUND"
  - Used when resources don't exist
  
- **ConcurrentModificationException**: Extends MembershipException
  - Error code: "CONCURRENT_MODIFICATION"
  - Used when optimistic lock fails

---

### 9. **Controller Layer** (`com.firstclub.membership.controller`)

#### MembershipController
REST API endpoints:

| Method | Endpoint | Handler |
|--------|----------|---------|
| GET | `/api/v1/membership/catalog` | `getCatalog()` |
| POST | `/api/v1/membership/subscribe` | `subscribe(request)` |
| GET | `/api/v1/membership/users/{userId}` | `getMembership(userId)` |
| POST | `/api/v1/membership/users/{userId}/upgrade` | `upgradeTier(userId)` |
| POST | `/api/v1/membership/users/{userId}/downgrade` | `downgradeTier(userId)` |
| POST | `/api/v1/membership/users/{userId}/cancel` | `cancel(userId)` |
| POST | `/api/v1/membership/users/{userId}/evaluate-tier` | `evaluateTier(userId, request)` |

---

## Key Design Patterns

### 1. **Strategy Pattern**
- `TierEvaluationStrategy` interface
- `ConfigurableTierEvaluationStrategy` implementation
- Allows flexible tier evaluation logic

### 2. **Factory Pattern**
- `BenefitFactory` for consistent benefit creation
- Encapsulates benefit configuration logic

### 3. **Repository Pattern**
- Abstraction over data persistence
- Interfaces: `UserMembershipRepository`, `MembershipPlanRepository`, `MembershipTierRepository`

### 4. **Value Objects**
- Immutable domain models: `MembershipPlan`, `MembershipTier`, `Benefit`
- Defensive copying for collections and maps

### 5. **Optimistic Locking**
- Version field in `UserMembership`
- `saveWithVersionCheck()` for concurrent updates
- Retry logic in services (MAX_OPTIMISTIC_RETRIES = 3)

### 6. **Per-User Locking**
- `UserLockManager` for fine-grained concurrency
- Serializes operations per user
- Prevents race conditions while allowing inter-user parallelism

---

## Data Flow Examples

### Subscribe Workflow
```
POST /subscribe
  ↓
MembershipController.subscribe(request)
  ↓
SubscriptionService.subscribe(request)
  ↓
UserLockManager.executeWithUserLock(userId)
  ├─ Check no active membership exists
  ├─ Validate plan and tier
  ├─ Create UserMembership with expiry
  └─ Save to repository
  ↓
Return MembershipResponse
```

### Tier Upgrade Workflow
```
POST /users/{userId}/upgrade
  ↓
MembershipController.upgradeTier(userId)
  ↓
SubscriptionService.upgradeTier(userId)
  ↓
UserLockManager.executeWithUserLock(userId)
  ├─ Get current membership
  ├─ Calculate next tier
  ├─ Update tier ID
  └─ saveWithVersionCheck() with retry logic
  ↓
Return MembershipResponse
```

### Tier Evaluation Workflow
```
POST /users/{userId}/evaluate-tier
  ↓
MembershipController.evaluateTier(userId, request)
  ↓
TierEvaluationService.evaluateAndApply(userId, request)
  ↓
UserLockManager.executeWithUserLock(userId)
  ├─ Create TierEvaluationContext
  ├─ Call ConfigurableTierEvaluationStrategy.evaluateEligibleTier()
  ├─ Compare with current tier
  ├─ If upgrade needed, use optimistic locking with retries
  └─ Save updated membership
  ↓
Return TierEvaluationResponse
```

---

## Concurrency & Thread Safety

### Per-User Locking Strategy
- **Problem**: Multiple requests for same user can cause race conditions
- **Solution**: UserLockManager maintains per-user ReentrantLocks
- **Benefits**:
  - Serializes mutations for same user
  - Allows parallel operations for different users
  - Minimal lock contention

### Optimistic Locking Strategy
- **Problem**: Pessimistic locking can reduce throughput
- **Solution**: Version field + optimistic checks
- **Implementation**:
  - `UserMembership.version` incremented on each save
  - `saveWithVersionCheck()` validates version matches before persistence
  - Service retries up to 3 times if version conflict detected

### Combined Approach
1. **Per-user pessimistic lock**: Prevents concurrent modifications for same user
2. **Optimistic version checks**: Provides safety during retry scenarios
3. **Retry logic**: Handles transient failures gracefully

---

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Records**: Used for DTOs (SubscribeRequest, TierEvaluationRequest, TierEvaluationContext, etc.)
- **Concurrency**: `java.util.concurrent` (ConcurrentHashMap, ReentrantLock)
- **Validation**: Jakarta Bean Validation (validation annotations)
- **API**: REST with Spring MVC

---

## Scalability Considerations

1. **Stateless Services**: Services are stateless and injectable
2. **Database**: Repository pattern allows easy persistence layer swaps
3. **Caching**: CatalogService results can be cached at controller level
4. **Locking**: Fine-grained per-user locking prevents global contention
5. **Async Processing**: Evaluation could be async with event publishing

---

## Future Enhancements

1. Add event publishing for membership changes
2. Implement subscription-based tier downgrade scheduling
3. Add benefit usage tracking and analytics
4. Implement member loyalty point system
5. Add tier expiry mechanics for time-bound tier updates
6. Implement bulk operations with batch processing

---

## Summary

The FirstClub Membership Program demonstrates excellent software engineering practices:

✅ **Clean Architecture**: Layered separation of concerns  
✅ **Design Patterns**: Strategy, Factory, Repository patterns  
✅ **Concurrency**: Per-user locking with optimistic versioning  
✅ **Type Safety**: Enums, value objects, immutability  
✅ **Error Handling**: Custom exceptions with error codes  
✅ **REST API**: Well-defined endpoints with proper status codes  
✅ **Testability**: Dependency injection, repository abstraction  
✅ **Extensibility**: Strategy pattern for evaluation logic  


