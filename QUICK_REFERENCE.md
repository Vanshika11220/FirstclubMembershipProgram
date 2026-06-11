# FirstClub Membership Program - Quick Reference Guide

## 📋 Quick Navigation

### Files Created
1. **UML_DIAGRAM.puml** - Complete UML class diagram (PlantUML format)
2. **ARCHITECTURE.md** - Detailed architecture documentation
3. **VISUAL_SUMMARY.md** - Visual diagrams and ASCII architecture
4. **QUICK_REFERENCE.md** - This file

---

## 🏗️ Project at a Glance

**Type**: Spring Boot REST API for Membership Management  
**Language**: Java 17+  
**Architecture**: Layered (Controller → Service → Repository → Domain)  
**Key Features**:
- Multi-tier membership system (Silver, Gold, Platinum)
- Benefit management
- Automatic tier evaluation
- Per-user concurrency control
- Optimistic locking with retries

---

## 📦 Core Components

### Domain Layer
| Class | Purpose |
|-------|---------|
| `UserMembership` | User's active membership with status, plan, tier, dates |
| `MembershipPlan` | Subscription plan (monthly, quarterly, yearly) |
| `MembershipTier` | Tier level with associated benefits |
| `Benefit` | Individual benefit with type and configuration |

### Enums
| Enum | Values |
|------|--------|
| `MembershipStatus` | ACTIVE, CANCELLED, EXPIRED |
| `TierLevel` | SILVER(1), GOLD(2), PLATINUM(3) |
| `BenefitType` | FREE_DELIVERY, DISCOUNT_PERCENTAGE, EXCLUSIVE_DEALS, EARLY_ACCESS_SALES, PRIORITY_SUPPORT |
| `PlanDuration` | MONTHLY(1), QUARTERLY(3), YEARLY(12) |

### Service Layer
| Service | Key Methods |
|---------|------------|
| `SubscriptionService` | subscribe, getMembership, upgradeTier, downgradeTier, cancel |
| `MembershipCatalogService` | getCatalog, getPlanOrThrow, getTierOrThrow, getTierByLevelOrThrow |
| `TierEvaluationService` | evaluateAndApply |

### Repository Layer
| Interface | Key Methods |
|-----------|------------|
| `UserMembershipRepository` | findActiveByUserId, findById, save, saveWithVersionCheck |
| `MembershipPlanRepository` | findAllActive, findById, save |
| `MembershipTierRepository` | findAll, findById, findByLevel, save |

---

## 🔌 REST API Endpoints

```
GET    /api/v1/membership/catalog
       → Returns all active plans and tiers

POST   /api/v1/membership/subscribe
       Body: { userId, planId, tierId }
       → Creates new membership

GET    /api/v1/membership/users/{userId}
       → Gets user's active membership

POST   /api/v1/membership/users/{userId}/upgrade
       → Upgrades user to next tier

POST   /api/v1/membership/users/{userId}/downgrade
       → Downgrades user to previous tier

POST   /api/v1/membership/users/{userId}/cancel
       → Cancels membership

POST   /api/v1/membership/users/{userId}/evaluate-tier
       Body: { orderCount, monthlyOrderValue, cohort }
       → Evaluates and applies tier upgrade if eligible
```

---

## 🔐 Concurrency Strategy

### Problem
Multiple concurrent requests for same user → Race conditions

### Solution: Multi-Layer Approach

1. **Per-User Locks** (UserLockManager)
   - `ConcurrentHashMap<String, ReentrantLock>` per user
   - Serializes operations for same user
   - Allows parallelism across different users

2. **Optimistic Versioning** (UserMembership)
   - `version` field tracks changes
   - `saveWithVersionCheck()` validates version before save
   - Prevents stale updates

3. **Retry Logic** (Services)
   - MAX_OPTIMISTIC_RETRIES = 3
   - Automatic retry on version mismatch
   - User-friendly error on final failure

### Flow
```
Lock User A → Execute → Check Version → Retry if needed → Unlock
```

---

## 🎯 Tier Evaluation Rules

### ConfigurableTierEvaluationStrategy

**PLATINUM** ≥ one of:
- 15+ orders
- ≥ $15,000 monthly value
- Cohort in [PREMIUM, VIP]

**GOLD** ≥ one of:
- 5+ orders
- ≥ $5,000 monthly value

**SILVER** (default)

---

## 🏭 Design Patterns Used

| Pattern | Implementation | Purpose |
|---------|----------------|---------|
| **Strategy** | TierEvaluationStrategy | Pluggable tier evaluation logic |
| **Factory** | BenefitFactory | Consistent benefit creation |
| **Repository** | 3 Repository interfaces | Abstract persistence |
| **Value Object** | MembershipPlan, Tier, Benefit | Immutable models |
| **DTO** | Request/Response records | Data transfer |

---

## 🛡️ Exception Handling

```
MembershipException (base)
├── ResourceNotFoundException (Code: "NOT_FOUND")
└── ConcurrentModificationException (Code: "CONCURRENT_MODIFICATION")
```

**Where thrown:**
- Services for validation failures
- Repository for data access issues
- Controller for invalid inputs

---

## 📊 Data Models

### UserMembership Fields
- `id`: UUID (final)
- `userId`: String (final)
- `planId`: String (mutable)
- `tierId`: String (mutable)
- `status`: MembershipStatus
- `startDate`: Instant (final)
- `expiryDate`: Instant
- `version`: long (for optimistic locking)

### MembershipPlan Fields
- `id`: String
- `name`: String
- `duration`: PlanDuration (MONTHLY, QUARTERLY, YEARLY)
- `price`: BigDecimal
- `active`: boolean

### MembershipTier Fields
- `id`: String
- `name`: String
- `level`: TierLevel (SILVER, GOLD, PLATINUM)
- `benefits`: List<Benefit> (immutable)

---

## 🔄 Common Workflows

### Subscribe New User
```
1. POST /subscribe with userId, planId, tierId
2. Acquire user lock
3. Check no active membership exists
4. Validate plan and tier exist
5. Create UserMembership
6. Calculate expiry (PlanDuration.addTo())
7. Save to repository
8. Release lock
9. Return MembershipResponse
```

### Evaluate and Upgrade Tier
```
1. POST /evaluate-tier with orderCount, monthlyOrderValue, cohort
2. Acquire user lock
3. Create TierEvaluationContext
4. Call strategy.evaluateEligibleTier() → returns TierLevel
5. Get current tier
6. If higher tier eligible:
   - Update membership.tierId
   - saveWithVersionCheck() with retry logic
7. Return TierEvaluationResponse with details
8. Release lock
```

### Upgrade/Downgrade Tier
```
1. POST /upgrade or /downgrade with userId
2. Acquire user lock
3. Get current membership
4. Calculate next tier
5. saveWithVersionCheck() with retry logic
6. Return MembershipResponse
7. Release lock
```

---

## 🗂️ Package Organization

```
Domain Layer
├── model/: UserMembership, MembershipPlan, MembershipTier, Benefit
└── enums/: MembershipStatus, TierLevel, BenefitType, PlanDuration

Persistence Layer
├── repository/: 3 Repository interfaces
└── inmemory/: In-memory implementations

Service Layer
├── SubscriptionService: Core membership operations
├── MembershipCatalogService: Catalog retrieval
└── TierEvaluationService: Tier evaluation logic

Presentation Layer
├── controller/: MembershipController (REST endpoints)
├── dto/: Request/Response objects
├── factory/: BenefitFactory
├── strategy/: Tier evaluation strategy
├── concurrency/: UserLockManager
└── exception/: Custom exceptions
```

---

## ✅ Validation

### SubscribeRequest
- `userId`: @NotBlank
- `planId`: @NotBlank
- `tierId`: @NotBlank

### TierEvaluationRequest
- `orderCount`: @Min(0)
- `monthlyOrderValue`: @NotNull
- `cohort`: String (optional)

### Business Validations
- No duplicate active membership
- Plan must be active
- Tier must exist
- Cannot upgrade beyond PLATINUM
- Cannot downgrade below SILVER
- No downgrade via evaluate-tier (only upgrade)

---

## 📈 Performance Considerations

| Aspect | Strategy |
|--------|----------|
| **Locking** | Per-user fine-grained locks (avoid global contention) |
| **Retries** | Max 3 attempts on optimistic lock failure |
| **Catalog** | Could be cached at controller level |
| **Async** | Evaluation could be async with event publishing |
| **Scalability** | Stateless services, horizontal scaling ready |

---

## 🔧 Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.x |
| Language | Java 17+ |
| Web | Spring MVC (REST) |
| Validation | Jakarta Bean Validation |
| Concurrency | `java.util.concurrent` |
| Data | Records for DTOs |

---

## 🚀 Key Features

✅ **Multi-Tier System**
- 3 tiers: SILVER, GOLD, PLATINUM
- Configurable evaluation rules
- Automatic tier upgrade capability

✅ **Benefit Management**
- 5 benefit types predefined
- Configurable benefit parameters
- Factory pattern for consistent creation

✅ **Concurrency Safety**
- Per-user locking prevents race conditions
- Optimistic versioning for safe concurrent updates
- Automatic retry logic for transient failures

✅ **Clean Architecture**
- Layered separation of concerns
- Repository pattern for data abstraction
- Strategy pattern for flexible evaluation

✅ **Error Handling**
- Custom exceptions with error codes
- Comprehensive validation
- User-friendly error messages

---

## 📚 How to Use the UML Diagram

### Option 1: Online Viewer (Recommended)
1. Go to https://www.plantuml.com/plantuml/uml/
2. Copy entire content from `UML_DIAGRAM.puml`
3. Paste into online editor
4. Diagram auto-renders

### Option 2: IDE Integration
1. Install PlantUML plugin in your IDE
2. Open `UML_DIAGRAM.puml`
3. Click to view diagram preview

### Option 3: CLI
```bash
plantuml UML_DIAGRAM.puml
# Generates UML_DIAGRAM.png or .svg
```

---

## 📖 Documentation Files

| File | Content |
|------|---------|
| `UML_DIAGRAM.puml` | Complete PlantUML class diagram |
| `ARCHITECTURE.md` | Detailed architecture & design patterns |
| `VISUAL_SUMMARY.md` | ASCII diagrams & visual flows |
| `QUICK_REFERENCE.md` | This quick reference |

---

## 💡 Interview Tips

### Design Questions to Ask
- How to scale to millions of users?
- How to handle tier expiry?
- How to add loyalty points system?
- How to make benefit redemption?
- How to support bulk operations?

### Strength Areas to Highlight
1. **Concurrency Handling**: Per-user locks + optimistic versioning
2. **Clean Architecture**: Layered design with clear separation
3. **Design Patterns**: Strategy, Factory, Repository
4. **Error Handling**: Custom exceptions with error codes
5. **Scalability**: Stateless services, horizontal ready
6. **Type Safety**: Enums, value objects, immutability

### Code Quality
✅ Constructor injection for testability  
✅ Immutable value objects  
✅ Repository abstraction  
✅ Strategy pattern for extensibility  
✅ Optimistic locking for performance  
✅ Per-user locking for concurrency  

---

## 🎓 Learning Path

1. Start with **Domain Models** (understand entities)
2. Understand **Enums** (type-safe alternatives)
3. Study **Service Layer** (business logic)
4. Learn **Concurrency Strategy** (locking + versioning)
5. Examine **Controller** (API contracts)
6. Trace through **Request/Response Flows**
7. Understand **Design Patterns** used

---

## 📞 Summary

This is a **production-quality membership management system** demonstrating:
- Clean architecture principles
- Advanced concurrency handling
- Design pattern applications
- RESTful API design
- Error handling best practices

Perfect for system design interviews and as a reference for building scalable Java applications!


