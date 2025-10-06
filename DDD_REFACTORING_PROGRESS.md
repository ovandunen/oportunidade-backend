# DDD Refactoring - Work In Progress

## Pattern Analysis

After analyzing `ReferenceResource.java`, `ReferenceService.java`, and `ReferenceRepository.java`, I identified the following DDD pattern:

### Architecture Layers

```
Resource (REST API)
    ↓ uses
DomainService (Business Logic)
    ↓ uses
Repository (Data Access)
    ↓ maps between
Domain ←→ DomainEntity (via EntityMapper)
    ↓ exposes as
DTO (via DtoMapper)
```

### Key Components

1. **Domain** - Business logic layer (pure domain models)
2. **DomainEntity** - Persistence layer (JPA entities)
3. **DTO** - API layer (data transfer objects)
4. **Resource** - REST endpoints (extends base Resource class)
5. **DomainService** - Business logic (extends base DomainService class)
6. **Repository** - Data access (extends base Repository class, implements PanacheRepository)
7. **EntityMapper** - Maps Domain ↔ DomainEntity (uses MapStruct)
8. **DtoMapper** - Maps DTO ↔ Domain (uses MapStruct)

## Refactoring Progress

### ✅ Phase 1 - Completed

1. **Domain Models Created:**
   - `Order` - extends Domain
   - `PaymentTransaction` - extends Domain
   - `WebhookEvent` - extends Domain

2. **DomainEntity Classes Created:**
   - `OrderEntity` - extends DomainEntity
   - `PaymentTransactionEntity` - extends DomainEntity
   - `WebhookEventEntity` - extends DomainEntity
   - All with @NamedQueries for common queries

3. **EntityMappers Created (MapStruct):**
   - `OrderEntityMapper` - implements EntityMapper<Order, OrderEntity>
   - `PaymentTransactionEntityMapper` - implements EntityMapper<PaymentTransaction, PaymentTransactionEntity>
   - `WebhookEventEntityMapper` - implements EntityMapper<WebhookEvent, WebhookEventEntity>

4. **Repositories Refactored:**
   - `OrderRepository` - extends Repository<Order, OrderEntity>
   - `PaymentTransactionRepository` - extends Repository<PaymentTransaction, PaymentTransactionEntity>
   - `WebhookEventRepository` - extends Repository<WebhookEvent, WebhookEventEntity>
   - All using EntityManager and MapStruct mappers

### ✅ Phase 2 - Completed

1. **Created DTOs for API layer:**
   - `OrderDTO`
   - `PaymentTransactionDTO`
   - `WebhookEventDTO`

2. **Created DtoMappers (MapStruct):**
   - `OrderDtoMapper` - implements DtoMapper<OrderDTO, Order>
   - `PaymentTransactionDtoMapper` - implements DtoMapper<PaymentTransactionDTO, PaymentTransaction>
   - `WebhookEventDtoMapper` - implements DtoMapper<WebhookEventDTO, WebhookEvent>

3. **Refactored Services:**
   - `OrderService` - extends DomainService<Order, OrderRepository>
   - `PaymentTransactionService` - extends DomainService<PaymentTransaction, PaymentTransactionRepository>
   - `WebhookEventService` - extends DomainService<WebhookEvent, WebhookEventRepository>
   - `PaymentService` - refactored to use domain services
   - `WebhookEventServiceFacade` - facade over domain service

4. **Refactored Resources:**
   - `OrderResource` - extends Resource<Order, OrderService>
   - `PaymentTransactionResource` - extends Resource<PaymentTransaction, PaymentTransactionService>
   - `AppyPayWebhookResource` - updated to use service facade
   - `WebhookProcessor` - updated to use refactored services
   - `WebhookHealthCheck` - updated to use new repository

5. **Clean up:**
   - ✅ Removed old entity files from `entity_old/` directory
   - ✅ Removed old service files
   - ✅ Renamed refactored service files

### 🚧 Phase 3 - In Progress

1. **Update Tests:**
   - Need to refactor unit tests to work with new structure
   - Need to refactor integration tests to use new repositories/services
   - Need to update test builders

2. **Documentation:**
   - Update documentation to reflect DDD architecture

## Key Differences from Original Implementation

| Aspect | Original | Refactored (DDD) |
|--------|----------|------------------|
| Entity layer | JPA entities with Panache | DomainEntity (JPA) separate from Domain |
| Repository | PanacheRepositoryBase | Extends Repository base class |
| Service | Direct entity manipulation | DomainService with validation |
| Mapping | Manual or no mapping | MapStruct EntityMapper + DtoMapper |
| API layer | Entities exposed directly | DTOs for API, Domain for business logic |
| Separation | Mixed concerns | Clear layer separation |

## Benefits of Refactoring

1. **Separation of Concerns** - Domain logic separate from persistence
2. **Testability** - Domain models can be tested without database
3. **Consistency** - Follows established codebase patterns
4. **Maintainability** - Changes to persistence don't affect domain logic
5. **Type Safety** - MapStruct provides compile-time mapping validation
6. **Flexibility** - Can change persistence layer without affecting business logic

## Notes

- Old entity files moved to `entity_old/` for reference
- All new files follow existing naming conventions
- MapStruct mappers use `componentModel = "cdi"` for CDI integration
- Enum mapping handled in MapStruct mappers (String ↔ Enum)
- Repository methods use EntityManager for queries
- @PrePersist and @PreUpdate hooks remain in DomainEntity classes

## Next Steps

Continue with creating DTOs, DtoMappers, and refactoring Services and Resources to complete the DDD refactoring.
