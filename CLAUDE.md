# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**NPI Seiko Core** is an NPI (New Product Introduction) order management system. It is the operational follow-up
application that comes after the quotation/estimation phase (handled by a separate "Cost Seiko" app). Once a product has
been quoted and approved, an NPI order is created here to track the full manufacturing lifecycle.

### Business Domain

An **NPI Order** represents a manufacturing work order for a new product, identified by a purchase order number (PO),
work order ID, part number, and quantity. Each NPI order goes through a **process workflow** composed of sequential
stages (ProcessInstanceLines) such as:

- Material purchasing & receiving
- Manufacturing and testing
- Validation document uploads
- Shipping
- Customer approval

**NPI Order statuses:** `READY_TO_PRODUCTION` → `STARTED` → `COMPLETED` (or `ABORTED`)

**Process line statuses:** `NOT_STARTED` → `IN_PROGRESS` → `COMPLETED` (or `ABORTED`)

A **Dashboard** provides KPIs: open NPI count, average lead time, and NPI count by process stage status.

### User Roles (NPI-specific, beyond admin roles)

In addition to `ADMINISTRATOR` and `SUPER_ADMINISTRATOR`, the system supports NPI-specific roles:
`PROJECT_MANAGER`, `ENGINEERING`, `PROCUREMENT`, `PLANNING`, `MANAGEMENT`

### Key API Resources

- `/npi-orders` — CRUD + search + archive/abort operations
- `/npi-orders/{uid}/process` — Retrieve the process instance (workflow steps) for an NPI order
- `/npi-orders/{uid}/process/lines/{lineUid}/status` — Update a process step status
- `/npi-orders/{uid}/files` — File attachments at the NPI order level
- `/npi-orders/{uid}/process/lines/{lineUid}/files` — File attachments per process step
- `/dashboard` — Aggregated KPIs

---

Multi-module Spring Boot 4.0.2 application with Java 25, using OpenAPI-first design for API contract management. The
application uses JWT-based authentication, PostgreSQL database, and follows clean architecture principles with strict
layer separation.

## Critical Rules (READ FIRST)

**Before writing any code, remember these mandatory rules:**

1. **Mappers:** ALWAYS use abstract classes, NEVER interfaces
   ```java
   @Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
   public abstract class YourMapper { }
   ```

2. **Entity IDs:** ALWAYS initialize with `= UUID.randomUUID()` and add `@Setter(AccessLevel.NONE)`
   ```java

@Setter(AccessLevel.NONE)
@Id
private final UUID entityId = UUID.randomUUID();

```

3. **EqualsAndHashCode:** ALWAYS use only the primary key field
   ```java
   @EqualsAndHashCode(of = "entityId")
   public class EntityNameEntity { }
   ```

4. **String Field Validation:** ALWAYS use `@NotBlank` for String fields, NEVER `@NotNull`
   ```java
   @NotBlank
   @Column(nullable = false)
   private String name;
   ```

5. **Entity Creation Dates:** ALWAYS initialize with `TimeUtils.nowOffsetDateTimeUTC()` and `@Setter(AccessLevel.NONE)`
   ```java

@Setter(AccessLevel.NONE)
@NotNull
@Column(nullable = false)
private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

```

6. **API Security:** ALWAYS specify which roles can access each endpoint
   ```java
   // For specific roles only:
   @Secured({
       UserRole.SecurityConstants.ADMINISTRATOR,
       UserRole.SecurityConstants.SUPER_ADMINISTRATOR
   })
   @GetMapping("/endpoint")
   public ResponseEntity<Type> method() { }

   // For ALL authenticated users - simply omit @Secured:
   @GetMapping("/endpoint")  // No @Secured = all authenticated users
   public ResponseEntity<Type> method() { }
   ```

**Note:** Available roles are `ADMINISTRATOR` and `SUPER_ADMINISTRATOR`

7. **Exceptions:** ONLY use `EntityNotFoundException` and `EntityExistsException`. For all other errors:
   ```java
   throw new GenericWithMessageException("message", SWCustomErrorCode.GENERIC_ERROR);
   ```

8. **EntityRetrievalHelper:** MANDATORY - For EVERY new entity, add retrieval method(s) to `EntityRetrievalHelper.java`
   ```java
   public EntityName getMustExistEntityNameById(UUID id) {
       return repository.findById(id)
           .orElseThrow(() -> new EntityNotFoundException("EntityName not found"));
   }
   ```

9. **String and Collection Utilities:** ALWAYS use Apache Commons utilities for validation checks
   ```java
   // For String validation - use org.apache.commons.lang3.StringUtils
   if (StringUtils.isBlank(searchText)) { }
   if (StringUtils.isNotBlank(name)) { }
   if (StringUtils.isEmpty(value)) { }

   // For Collection validation - use org.apache.commons.collections4.CollectionUtils
   if (CollectionUtils.isEmpty(list)) { }
   if (CollectionUtils.isNotEmpty(items)) { }
   ```
   **Never use:** `string == null || string.isEmpty()`, `list == null || list.isEmpty()`, etc.

10. **Enum Structure:** ALWAYS follow this mandatory structure for enums
   ```java
   import lombok.Getter;

   public enum YourEnum {
     VALUE_ONE("VALUE_ONE", "Value One"),

     VALUE_TWO("VALUE_TWO", "Value Two");

     private final String value;
     @Getter private final String humanReadableValue;

     YourEnum(String value, String humanReadableValue) {
       this.value = value;
       this.humanReadableValue = humanReadableValue;
     }

     public String getValue() {
       return value;
     }

     public static YourEnum fromValue(String value) {
       for (YourEnum b : YourEnum.values()) {
         if (b.value.equals(value)) {
           return b;
         }
       }
       throw new IllegalArgumentException("Unexpected value '" + value + "'");
     }

     @Override
     public String toString() {
       return String.valueOf(value);
     }

     // Optional: Add business logic methods
     public boolean isFinalStatus() {
       return this.equals(VALUE_TWO);
     }
   }
   ```

**Mandatory elements:**

- Two fields: `value` (String) and `humanReadableValue` (String with @Getter)
- Constructor taking both parameters
- `getValue()` method
- `fromValue(String value)` static method
- `toString()` returning the value
- Optional business logic methods (e.g., `isFinalStatus()`, `isActive()`, etc.)

11. **JPA Relationships:** ALWAYS use LAZY fetch for @ManyToOne relationships

   ```java

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id", nullable = false)
private ParentEntity parent;
   ```

This prevents N+1 query problems and improves performance. Never use EAGER fetch unless absolutely necessary.

12. **Mapping:** ALWAYS use MapStruct for DTO <-> Entity mapping.

13. **BigDecimal Columns:** ALWAYS annotate `BigDecimal` fields with `@Column(precision = 25, scale = 6)` (nullable) or
    `@Column(nullable = false, precision = 25, scale = 6)` (non-nullable). NEVER use a bare `@Column` or no annotation
    on a BigDecimal field.

   ```java
   // Nullable BigDecimal:
@Column(precision = 25, scale = 6)
private BigDecimal rate;

// Non-nullable BigDecimal:
@NotNull
@Column(nullable = false, precision = 25, scale = 6)
private BigDecimal price;
   ```

14. **Ordered OneToMany Collections — `indexId` Pattern:** When a `@OneToMany` collection needs to maintain a
    user-defined order, ALWAYS use an `indexId` field managed through dedicated add/remove methods on the parent entity.
    NEVER use a database-level `orderIndex` column on the child entity or sort by `creationDate`.

   **Child entity:**
   ```java
   @Column(nullable = false, name = "index_id")
   private int indexId;
   ```

   **Parent entity:**
   ```java
   @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
   @OrderBy("indexId ASC")
   private List<ChildEntity> children = new ArrayList<>();

   public void addChild(@NotNull ChildEntity child) {
     child.setIndexId(children.size()); // assigns next available index
     child.setParent(this);
     children.add(child);
   }

   public void removeChild(@NotNull ChildEntity child) {
     children.remove(child);
     child.setParent(null);
     reindexChildren();
   }

   private void reindexChildren() {
     for (int i = 0; i < children.size(); i++) {
       children.get(i).setIndexId(i);
     }
   }
   ```

   **Rules:**
   - ALWAYS call `addChild()` / `removeChild()` — never manually set `indexId` or manipulate the list directly
   - ALWAYS add `@OrderBy("indexId ASC")` on the `@OneToMany` annotation
   - ALWAYS use `CascadeType.ALL` + `orphanRemoval = true` so JPA persists/removes children automatically
   - NEVER expose a separate repository `saveAll()` for the children when using this pattern — rely on cascade

   **See:** `NpiOrderEntity.processLines` + `NpiOrderEntity.addProcessLine()` / `removeProcessLine()` as the reference
   implementation.

15. **Validators:** ALWAYS put business validation logic in a dedicated validator class in `services/validator/`, NEVER
    as private methods in the service. Inject the validator into the service.

   ```java
   // In services/validator/YourEntityValidator.java
@Service
@RequiredArgsConstructor
public class YourEntityValidator {
    private final AppConfigurationProperties appConfigurationProperties; // inject if needed

    public void validateSomething(SomeType value) {
        if (/* invalid */) {
            throw new GenericWithMessageException("message", SWCustomErrorCode.GENERIC_ERROR);
        }
    }
}

// In the service
@RequiredArgsConstructor
@Service
public class YourEntityService {
    private final YourEntityValidator yourEntityValidator;

    public void someMethod(...) {
        yourEntityValidator.validateSomething(value);
        // ...
    }
}
   ```

## Build and Development Commands

### Building the Application

```bash
# Clean and build entire project (both swagger and core modules)
mvn clean install

# Build only the core module (requires swagger to be built first)
mvn clean install -pl core

# Skip tests during build
mvn clean install -DskipTests

# Generate OpenAPI models only (swagger module)
mvn clean install -pl swagger
```

### Running the Application

```bash
# Run with dev profile (from core directory)
cd core
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with production profile
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

**Default Ports:**

- Development: `9090` (configured in `application-dev.properties`)
- Production: `80` (configured in container)

### Docker Operations

```bash
# Build Docker image
docker build -t planning-core-image:1.0.0 .

# Run with docker-compose (includes PostgreSQL)
docker-compose up

# Deploy image to remote server
REMOTE_HOST=<host-ip> ./deploy-image.sh planning-core-image:1.0.0
```

### Database

**Development Database:**

- URL: `jdbc:postgresql://localhost:5432/cost_seiko`
- Username: `planning`
- Password: `planning`

**Docker Database:**

- Port: `5444:5432`
- Same credentials as development

## Architecture

### Module Structure

**Two-module Maven project:**

1. **`swagger/` module**: OpenAPI specification and code generation
    - Contains: `swagger-npi-seiko.yaml` (1158 lines)
    - Generates: Model classes with `SW` prefix (e.g., `SWUser`, `SWLoginDetails`)
    - Package: `my.zkonsulting.planning.generated.model`
    - Build output: Generated POJOs with Jackson annotations, validation, and serialization

2. **`core/` module**: Main application logic
    - Depends on: `swagger` module
    - Contains: Controllers, Services, Repositories, Entities, Security, Configuration

### Layer Architecture

**Package Base:** `my.lokalix.planning.core`

**Layers:**

- **Controllers** (`controllers/`) - REST endpoints using generated DTOs
    - `PublicController` - Unauthenticated endpoints (`/public/**`)
    - `UserController` - User management endpoints
    - `HiddenController` - Internal endpoints

- **Services** (`services/`) - Business logic layer
    - Main services (e.g., `UserService`, `EmailService`, `LicenseService`)
    - `services/helper/` - Reusable helper services
    - `services/validator/` - Validation logic (e.g., `CostRequestValidator`)
    - `services/cron/` - Scheduled jobs

- **Repositories** (`repositories/`) - Data access with Spring Data JPA
    - Extend `JpaRepository<Entity, UUID>`
    - UUID primary keys on all entities
    - Custom queries using `@Query` with JPQL
    - `repositories/admin/` - Admin-specific repositories

- **Entities** (`models/entities/`) - JPA entities with Lombok
    - All use UUID primary keys
    - Enums stored as strings (`@Enumerated(EnumType.STRING)`)
    - `entities/admin/` - Admin domain entities

- **Mappers** (`mappers/`) - MapStruct abstract classes
    - Convert between generated DTOs (`SW*`) and entities (`*Entity`)
    - Component model: SPRING (dependency injection)
    - Generated implementations in `target/generated-sources/annotations`
    - **IMPORTANT:** Always use abstract classes, not interfaces

- **Security** (`security/`)
    - `JwtUtil` - JWT creation and validation (JJWT library)
    - `JwtAuthorizationFilter` - Request filter for JWT validation
    - `LoggedUserDetailsService` - Access current user info
    - `CustomUserDetailsService` - UserDetailsService implementation

- **Configuration** (`configurations/`)
    - `SecurityConfig` - Spring Security setup
    - `AppConfigurationProperties` - Type-safe configuration with validation

- **Exception Handling** (`exceptions/`, `handlers/`)
    - `GlobalExceptionHandler` - Centralized exception handling with Java 21 pattern matching
    - Domain-specific exceptions (e.g., `exceptions/user/`, `exceptions/file/`)

- **Utils** (`utils/`)
    - `TimeUtils` - Timezone-aware date/time operations (all methods support configurable timezone)
    - `ExcelUtils` - Apache POI utilities for Excel operations
    - `GlobalConstants` - Application-wide constants

### OpenAPI Code Generation Workflow

**Critical workflow for understanding DTO/Entity separation:**

1. Define API contract in `swagger/src/main/resources/swagger-npi-seiko.yaml`
2. Maven build generates Java models with `SW` prefix (e.g., `User` schema → `SWUser` class)
3. Controllers import and use generated models: `import my.zkonsulting.planning.generated.model.*`
4. MapStruct converts between generated DTOs and JPA entities
5. Services work with entities; controllers work with generated DTOs

**When modifying APIs:**

- Edit `swagger-npi-seiko.yaml` to change API contracts
- Run `mvn clean install -pl swagger` to regenerate models
- Update MapStruct mappers if new fields added
- Controllers automatically use updated DTOs

**Important:** Never manually edit generated `SW*` classes - they are regenerated on every build.

**OpenAPI Best Practices:**

1. **Reusable Path Parameters:** Define commonly used path parameters in the `components/parameters` section:
   ```yaml
   components:
     parameters:
       uidParam:
         in: path
         name: uid
         required: true
         schema:
           $ref: '#/components/schemas/Uid'
       lineUidParam:
         in: path
         name: lineUid
         required: true
         schema:
           $ref: '#/components/schemas/Uid'
   ```
   Then reference them in paths:
   ```yaml
   /cost-requests/{uid}/lines/{lineUid}:
     parameters:
       - $ref: '#/components/parameters/uidParam'
       - $ref: '#/components/parameters/lineUidParam'
   ```

2. **Nested Resource Creation:** When creating parent resources with nested children, include optional arrays in Create
   DTOs:
   ```yaml
   CostRequestCreate:
     properties:
       # ... parent fields
       lines:
         type: array
         items:
           $ref: '#/components/schemas/CostRequestLineCreate'
   ```
   The service must handle creating both parent and children in a single transaction.

3. **Status Validation Endpoints:** For status transitions with business logic (like freezing snapshots), create
   dedicated validation endpoints:
   ```yaml
   /cost-requests/{uid}/lines/{lineUid}/validate:
     post:
       summary: Validate a cost request line (set to ESTIMATED and freeze currency)
       operationId: validateCostRequestLine
   ```
   These endpoints encapsulate complex domain rules (e.g., currency freezing, parent status updates).

### Security Implementation

**JWT-Based Stateless Authentication:**

- Login flow: `PublicController.loginUser()` → `UserService.login()`
- Token validity: 24 hours
- Token storage: `AuthTokenEntity` tracks all active tokens with device type
- HMAC SHA-256 signing with Base64-encoded secret from config

**Authorization Filter:**

- Validates JWT on every request (except `/public/**`)
- Checks token validity in database
- Handles disconnection reasons: role change, deactivation, other device login, logout
- Sets `CustomUsernamePasswordAuthenticationToken` in SecurityContext with userId

**Security Configuration:**

- CSRF disabled (stateless API)
- CORS enabled (allow all origins/methods in dev)
- `/public/**` endpoints permit all
- All other endpoints require authentication
- BCrypt password encoder
- Method-level security with `@Secured` annotations

**Current User Access:**
Use `LoggedUserDetailsService` injected bean:

```java

@Autowired
private LoggedUserDetailsService loggedUserDetailsService;

UUID userId = loggedUserDetailsService.getLoggedUserId();
String login = loggedUserDetailsService.getLoggedUserLogin();
UserEntity user = loggedUserDetailsService.getLoggedUserEntity();
boolean isAdmin = loggedUserDetailsService.hasRole(UserRole.ADMINISTRATOR);
```

**User Roles:** Defined in `UserRole` enum with security constants for `@Secured` annotations.

### Database Patterns

**Entity Conventions:**

- UUID primary keys with initialization: `@Id private UUID entityId = UUID.randomUUID();`
- **IMPORTANT:** Add `@Setter(AccessLevel.NONE)` on the ID field to prevent modification
- **IMPORTANT:** Use `@EqualsAndHashCode(of = "entityId")` with ONLY the primary key field
- Lombok annotations: `@Getter`, `@Setter`, `@Entity`
- Bean Validation:
    - `@NotBlank` for String fields (NOT `@NotNull`)
    - `@NotNull` for non-String fields (Integer, Boolean, Enum, etc.)
- Enums as strings: `@Enumerated(EnumType.STRING)`
- **IMPORTANT:** All `@ManyToOne` relationships MUST use `fetch = FetchType.LAZY` to prevent N+1 queries
- Explicit column definitions for important fields
- Creation date fields: Use `private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();` with
  `@Setter(AccessLevel.NONE)`

**Repository Conventions:**

- Extend `JpaRepository<Entity, UUID>`
- Method name queries (e.g., `findByLoginIgnoreCase`)
- `@Query` for complex JPQL queries
- Pagination with `Page<T>` and `Pageable`
- Test account filtering pattern using `username-suffix-for-tests-only` config

**Transaction Management:**

- `@Transactional` on service methods
- `spring.jpa.open-in-view=false` - prevents lazy loading outside transactions

### Configuration Management

**Type-Safe Configuration:**

- `AppConfigurationProperties` class with `@ConfigurationProperties`
- Bean Validation on config values (`@NotBlank`, `@NotNull`)
- Nested configuration for external services (e.g., SMTP2GO)

**Key Configuration Values:**

- `app-timezone` - Default timezone (Asia/Singapore)
- `jwt-secret-key` - Base64-encoded JWT signing secret
- `license-secret-key` - AES encryption key for licenses
- `super-admin-login/password` - Initial admin account
- `username-suffix-for-tests-only` - Filter test accounts (default: `@fortestonly.lokal`)
- `app-base-url` - Base URL for email links
- `starting-year` - Application starting year

**Profile-Based Properties:**

- `application.properties` - Base configuration
- `application-dev.properties` - Development overrides
- `application-production.properties` - Production settings

### Common Utilities

**TimeUtils:**
All date/time operations are timezone-aware. Use these methods instead of creating OffsetDateTime/LocalDate directly:

```java
OffsetDateTime now = TimeUtils.nowOffsetDateTime(zoneId);
OffsetDateTime nowUtc = TimeUtils.nowOffsetDateTimeUTC(); // For database timestamps
LocalDate today = TimeUtils.nowLocalDate(zoneId);
OffsetDateTime startOfDay = TimeUtils.toOffsetDateTimeStartOfDay(localDate, zoneId);
```

**ExcelUtils:**
Provides cell reading, styling, and template processing with Apache POI.

**EntityRetrievalHelper (CRITICAL):**

**MANDATORY REQUIREMENT:** For every new entity created, you MUST add corresponding retrieval methods to
`EntityRetrievalHelper.java`.

**Naming Convention Pattern:**

- By ID: `getMustExist[EntityName]ById(UUID id)`
- By Code: `getMustExist[EntityName]ByCode(String code)`
- By Name: `getMustExist[EntityName]ByName(String name)`
- By other field: `getMustExist[EntityName]By[FieldName]([Type] fieldValue)`

**Example usage:**

```java
// Always use EntityRetrievalHelper instead of repository.findById directly
CustomerEntity customer = entityRetrievalHelper.getMustExistCustomerById(customerId);
// Throws EntityNotFoundException if not found with consistent error message
```

**When to add methods:**

- Every time you create a new entity, add at least `getMustExist[Entity]ById`
- Add additional methods for any field you commonly retrieve by (code, name, etc.)
- Follow the existing naming pattern strictly

**GlobalConstants:**

- `HEADER_UID = "uid"` - Custom header for entity IDs
- `ALLOWED_FILE_EXTENSIONS` - List of allowed file extensions

### Email System

**Technology:** Thymeleaf templates + Reactive WebFlux client

**Templates Location:** `core/src/main/resources/email-templates/`

**Email Service:**

- Uses SMTP2GO API for delivery
- Reactive WebClient for async processing
- Template processing with SpringTemplateEngine
- Configuration via `smtp2go.*` properties

### License Management

- **Encryption:** AES encryption for license data
- **Storage:** Encrypted in database (`LicenseEntity`)
- **Purpose:** Controls max active users
- **Key:** Configured via `license-secret-key` property

## Development Patterns

### Creating a New Entity

**MANDATORY Steps when creating a new entity:**

1. **Create the Entity class** with proper conventions:

```java

@Getter
@Setter
@Entity
@Table(name = "table_name")
@EqualsAndHashCode(of = "entityNameId")  // ALWAYS use only the primary key
public class EntityNameEntity {
    @Setter(AccessLevel.NONE)
    @Id
    private final UUID entityNameId = UUID.randomUUID();

    @NotBlank  // Use @NotBlank for String fields, NOT @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull  // Use @NotNull for non-String fields
    @Column(nullable = false)
    private Integer quantity;

    @Setter(AccessLevel.NONE)
    @NotNull
    @Column(nullable = false)
    private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();
}
```

2. **Create the Repository interface**:

```java
public interface EntityNameRepository extends JpaRepository<EntityNameEntity, UUID> {
    Optional<EntityNameEntity> findByCode(String code);
}
```

3. **Add retrieval methods to EntityRetrievalHelper** (CRITICAL):

```java
public EntityNameEntity getMustExistEntityNameById(UUID uid) {
    return entityNameRepository
            .findById(uid)
            .orElseThrow(() -> new EntityNotFoundException("EntityName not found"));
}

public EntityNameEntity getMustExistEntityNameByCode(String code) {
    return entityNameRepository
            .findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("EntityName with code " + code + " not found"));
}
```

4. **Create MapStruct mapper as abstract class**:

```java

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EntityNameMapper {

    public abstract EntityNameEntity toEntityName(SWEntityNameCreate dto);

    @Mapping(source = "entityNameId", target = "uid")
    public abstract SWEntityName toSwEntityName(EntityNameEntity entity);

    public abstract List<SWEntityName> toListSwEntityName(List<EntityNameEntity> entities);

    public abstract void updateEntityFromDto(SWEntityNameUpdate dto, @MappingTarget EntityNameEntity entity);
}
```

### Implementing Search Functionality

**MANDATORY ARCHITECTURE:** All searchable entities must implement the searchable concatenated fields pattern for
consistent and performant full-text search.
**MANDATORY ARCHITECTURE:** All searchable methods must return non-archive entities.

**Pattern Overview:**

1. Add a `searchableConcatenatedFields` column to the entity
2. Implement `buildSearchableConcatenation()` method that concatenates all searchable fields
3. Use `@PrePersist` and `@PreUpdate` to automatically update the search field on save
4. Write repository queries using triple-quoted strings that search on this field

**Step 1: Add searchable field to Entity**

```java

@Getter
@Setter
@Entity
@Table(name = "entity_name")
@EqualsAndHashCode(of = "entityId")
public class EntityNameEntity {
    // ... other fields ...

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String searchableConcatenatedFields;

    @PrePersist
    @PreUpdate
    private void onSave() {
        buildSearchableConcatenation();
    }

    public void buildSearchableConcatenation() {
        StringBuilder sb = new StringBuilder();
        // Add all searchable fields
        if (StringUtils.isNotBlank(name)) {
            sb.append(name);
            sb.append(" ");
        }
        if (StringUtils.isNotBlank(code)) {
            sb.append(code);
            sb.append(" ");
        }
        // Add other searchable fields as needed
        searchableConcatenatedFields = sb.toString().trim();
    }
}
```

**Step 2: Add search query to Repository using triple-quoted strings**

```java
public interface EntityNameRepository extends JpaRepository<EntityNameEntity, UUID> {
    @Query(
            """
                    SELECT e FROM EntityNameEntity e
                    WHERE LOWER(e.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    """)
    Page<EntityNameEntity> findBySearch(Pageable pageable, @Param("searchText") String searchText);
}
```

**Step 3: Implement search method in Service with separate pagination logic**

```java

@Transactional
public SWEntityNamesPaginated searchEntityNames(int offset, int limit, SWEntityNamesSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "code");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<EntityNameEntity> paginatedResults;

    if (StringUtils.isBlank(search.getSearchText())) {
        paginatedResults = repository.findAll(pageable);
    } else {
        paginatedResults = repository.findBySearch(pageable, search.getSearchText());
    }

    return populateEntityNamesPaginatedResults(paginatedResults);
}

private SWEntityNamesPaginated populateEntityNamesPaginatedResults(
        Page<EntityNameEntity> paginatedResults) {
    SWEntityNamesPaginated result = new SWEntityNamesPaginated();
    result.setResults(mapper.toListDto(paginatedResults.getContent()));
    result.setPage(paginatedResults.getNumber());
    result.setPerPage(paginatedResults.getSize());
    result.setTotal((int) paginatedResults.getTotalElements());
    result.setHasPrev(paginatedResults.hasPrevious());
    result.setHasNext(paginatedResults.hasNext());
    return result;
}
```

**Step 4: Add search endpoint in Controller**

```java

@PostMapping("/entity-names/search")
public ResponseEntity<SWEntityNamesPaginated> searchEntityNames(
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "20") int limit,
        @RequestBody SWEntityNamesSearch body) {
    SWEntityNamesPaginated result = service.searchEntityNames(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
}
```

**Key Points:**

- **Triple-quoted strings (`"""`)**: ALWAYS use for `@Query` JPQL queries - improves readability
- **Automatic updates**: The `@PrePersist` and `@PreUpdate` hooks ensure the search field is always current
- **Case-insensitive search**: Use `LOWER()` in queries for consistent search behavior
- **Flexible concatenation**: Include all fields users might search on (name, code, description, etc.)
- **Performance**: Single TEXT column search is more efficient than multiple LIKE clauses on individual columns
- **Separate pagination logic**: ALWAYS extract pagination result mapping into a private `populateXxxPaginatedResults()`
  method for cleaner code and reusability

### Parent-Child Status Management

**Pattern:** When child entities have status fields that affect parent entity status, implement automatic parent status
updates.

**Use Case:** CostRequest with CostRequestLines - when all lines are ESTIMATED, the parent CostRequest should
automatically become ESTIMATED.

**Implementation:**

1. **Create nested resources during parent creation:**
   ```java
   @Transactional
   public SWCostRequest createCostRequest(SWCostRequestCreate body) {
       CostRequestEntity entity = costRequestMapper.toCostRequestEntity(body);
       CostRequestEntity savedCostRequest = costRequestRepository.save(entity);

       // Create lines if provided
       if (body.getLines() != null && !body.getLines().isEmpty()) {
           for (SWCostRequestLineCreate lineDto : body.getLines()) {
               // Create each line linked to parent
               CostRequestLineEntity lineEntity = new CostRequestLineEntity();
               lineEntity.setCostRequest(savedCostRequest);
               // ... set other fields
               costRequestLineRepository.save(lineEntity);
           }
       }

       // Reload to include lines
       return costRequestMapper.toSWCostRequest(
           entityRetrievalHelper.getMustExistCostRequestById(savedCostRequest.getId())
       );
   }
   ```

2. **Dedicated validation endpoint for status transitions:**
   ```java
   @Transactional
   public SWCostRequestLine validateCostRequestLine(UUID costRequestUid, UUID lineUid) {
       CostRequestLineEntity lineEntity = entityRetrievalHelper.getMustExistCostRequestLineById(lineUid);

       // Verify ownership
       if (!lineEntity.getCostRequest().getCostRequestId().equals(costRequestUid)) {
           throw new GenericWithMessageException("Line does not belong to cost request",
               SWCustomErrorCode.GENERIC_ERROR);
       }

       // Set to ESTIMATED and perform domain logic (e.g., freeze snapshots)
       lineEntity.setStatus(CostRequestStatus.ESTIMATED);
       // ... freeze currency snapshot logic

       CostRequestLineEntity savedLine = costRequestLineRepository.save(lineEntity);

       // Check and update parent status
       checkAndUpdateParentStatus(lineEntity.getCostRequest());

       return costRequestLineMapper.toSWCostRequestLine(savedLine);
   }
   ```

3. **Private helper method to check parent status:**
   ```java
   private void checkAndUpdateParentStatus(CostRequestEntity costRequest) {
       boolean allLinesCompleted = costRequest.getLines().stream()
           .allMatch(line -> line.getStatus() == CostRequestStatus.ESTIMATED);

       if (allLinesCompleted && !costRequest.getLines().isEmpty()
           && costRequest.getStatus() != CostRequestStatus.ESTIMATED) {
           costRequest.setStatus(CostRequestStatus.ESTIMATED);
           costRequestRepository.save(costRequest);
       }
   }
   ```

4. **Call parent status check after any child status update:**
    - After `validateCostRequestLine()`
    - After `updateCostRequestLine()` if status changed to ESTIMATED
    - Ensures parent status is always consistent with children

**Key Points:**

- Use dedicated validation endpoints for complex status transitions
- Always verify child belongs to parent before operations
- Automatically update parent status when all children meet criteria
- Encapsulate domain logic (snapshot freezing) in validation endpoints
- Keep parent status consistent with children in real-time

### Adding a New API Endpoint

1. Define endpoint in `swagger/src/main/resources/swagger-npi-seiko.yaml`
2. Rebuild swagger module: `mvn clean install -pl swagger`
3. Create controller method with proper security annotation:

**For specific roles only:**

```java

@Secured({
        UserRole.SecurityConstants.ADMINISTRATOR,
        UserRole.SecurityConstants.SUPER_ADMINISTRATOR
})
@PostMapping("/customers")
public ResponseEntity<SWCustomer> createCustomer(@Valid @RequestBody SWCustomerCreate body) {
    // implementation
}
```

**For ALL authenticated users (omit @Secured):**

```java
// No @Secured annotation means all authenticated users can access
@GetMapping("/customers")
public ResponseEntity<List<SWCustomer>> getCustomers() {
    // implementation
}
```

**Available Roles:**

- `UserRole.SecurityConstants.ADMINISTRATOR`
- `UserRole.SecurityConstants.SUPER_ADMINISTRATOR`

4. Implement service method (use entities internally)
5. Add MapStruct mapper methods if new entity/DTO mapping needed
6. Add repository methods if new queries needed

### MapStruct Mapper Configuration

**IMPORTANT:** Always create mappers as **abstract classes**, NOT interfaces.

**Required annotations:**

```java

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class YourMapper {
    // abstract methods here
}
```

**List mappings with qualifiers:** When a list mapping method needs to use a specific named single-item mapping (e.g. to
ignore certain fields), ALWAYS use `@IterableMapping(qualifiedByName = ...)` paired with `@Named` on the single-item
method. Never use manual streams in the caller.

```java
// List method references the named single-item method
@IterableMapping(qualifiedByName = "toSwEntityForRole")
public abstract List<@Valid SWEntity> toListSwEntityForRole(List<SWEntity> sources);

// Single-item method carries the actual mapping rules
@Named("toSwEntityForRole")
@Mapping(target = "sensitiveField", ignore = true)
public abstract SWEntity toSwEntityForRole(SWEntity source);
```

**Annotation processor setup in `core/pom.xml` requires specific order:**

```xml

<annotationProcessorPaths>
    <path><!-- lombok --></path>
    <path><!-- mapstruct-processor --></path>
    <path><!-- lombok-mapstruct-binding --></path>
</annotationProcessorPaths>
```

This ensures Lombok generates getters/setters before MapStruct processes mappings.

### Exception Handling

**CRITICAL RULE:** For all exceptions except `EntityNotFoundException` and `EntityExistsException`, throw directly
using:

```java
throw new GenericWithMessageException("Your error message",SWCustomErrorCode.GENERIC_ERROR);
```

**Only use these two standard exceptions:**

- `EntityNotFoundException` - When an entity is not found by ID or other criteria
- `EntityExistsException` - When trying to create an entity that already exists

**Do NOT create custom exception classes** unless there is a specific business requirement.

`GlobalExceptionHandler` automatically converts all exceptions to `SWCustomError` responses using Java 21 pattern
matching.

### Working with Timezones

Always use timezone from configuration (`app-timezone`) via `TimeUtils`:

- Store timestamps in UTC in database (`nowOffsetDateTimeUTC()`)
- Convert to user's timezone for display using `TimeUtils` methods
- Use `toOffsetDateTimeStartOfDay/EndOfDay` for date range queries

### File Handling

- Excel templates: `core/src/main/resources/excel-templates/`
- File storage paths: Configured via `cost-request-files-path-directory`, `cost-request-line-files-path-directory` etc
- Validation: Check against `GlobalConstants.ALLOWED_FILE_EXTENSIONS`
- Use `FileHelper` service for upload/download operations
