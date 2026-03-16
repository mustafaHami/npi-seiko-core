# Cost Request & Currency Implementation Guide

## ✅ Files Already Created (Base Architecture)

### Entities

- ✅ `CurrencyInterface.java` - Interface for currency typing
- ✅ `CurrencyEntity.java` - Currency entity (implements CurrencyInterface)
- ✅ `CurrencySnapshot.java` - Embeddable for frozen currency
- ✅ `CostRequestEntity.java` - Entity with OneToMany to lines
- ✅ `CostRequestLineEntity.java` - Entity with getCurrency() and freezeCurrencySnapshot()
- ✅ `CostRequestMethodType.java` + `CostRequestStatus.java` - Enums

### Repositories

- ✅ `CurrencyRepository.java`, `CostRequestRepository.java`, `CostRequestLineRepository.java`

### Mappers

- ✅ `CurrencyMapper.java`, `CostRequestMapper.java`, `CostRequestLineMapper.java`

### EntityRetrievalHelper

- ✅ Retrieval methods added

---

## 📋 Files To Create

### 1. CurrencyService.java

**Dependencies:** CurrencyMapper, CurrencyRepository, EntityRetrievalHelper

**Methods:**

- `createCurrency(body)` - Check code uniqueness, map, save
- `retrieveCurrency(uid)` - Retrieve by ID
- `updateCurrency(uid, body)` - Check code uniqueness if changed, update
- `archiveCurrency(uid)` - Set archived=true + archivedAt
- `searchCurrencies(offset, limit, search)` - Pagination, sort by code ASC, with populateCurrenciesPaginatedResults()

---

### 2. CostRequestService.java

**IMPORTANT:** Handles both CostRequest AND CostRequestLines (everything in same service)

**Dependencies:** CostRequestMapper, CostRequestLineMapper, CostRequestRepository, CostRequestLineRepository,
CurrencyRepository, EntityRetrievalHelper

**CostRequest Methods:**

- `createCostRequest(body)` - Map + save
- `retrieveCostRequest(uid)` - **IMPORTANT:** Mapper must include lines automatically
- `updateCostRequest(uid, body)` - Update
- `searchCostRequests(offset, limit, search)` - **IMPORTANT:** Results include lines for each CostRequest, sort by
  creationDate DESC

**CostRequestLine Methods:**

- `createCostRequestLine(costRequestUid, body)` - Check parent exists, check currency exists, create line
- `retrieveCostRequestLine(costRequestUid, lineUid)` - Check belongs to parent
- `updateCostRequestLine(costRequestUid, lineUid, body)` - **DOMAIN RULE:** If status → COMPLETED, call
  line.freezeCurrencySnapshot(currentCurrency)
- **NO SEARCH** for lines - they come with parent

---

### 3. CostRequestMapper Required Modifications

**IMPORTANT:** Mapper must automatically map lines!

In `CostRequestMapper.java`, add:

- Automatic lines mapping via `@Mapping(source = "lines", target = "lines")`
- MapStruct must use CostRequestLineMapper to map the collection

---

### 4. CurrencyController.java

**Endpoints:** POST /search, POST /, GET /{uid}, PUT /{uid}, POST /{uid}/archive

**Security:** ADMINISTRATOR + SUPER_ADMINISTRATOR on all endpoints

---

### 5. CostRequestController.java

**IMPORTANT:** Single controller for CostRequest AND Lines

**CostRequest Endpoints:**

- POST /cost-requests/search
- POST /cost-requests
- GET /cost-requests/{uid} (returns with lines)
- PUT /cost-requests/{uid}

**CostRequestLine Endpoints:**

- POST /cost-requests/{uid}/lines
- GET /cost-requests/{uid}/lines/{lineUid}
- PUT /cost-requests/{uid}/lines/{lineUid}

**NO:** /lines/search (lines come with parent)

---

## 🔧 Swagger - Key Points

### CostRequest schema must include lines:

```yaml
CostRequest:
  properties:
    # ... other fields
    lines:
      type: array
      items:
        $ref: '#/components/schemas/CostRequestLine'
```

### No CostRequestLinesPaginated nor /lines/search

Lines are retrieved via parent CostRequest.

### Endpoints to add:

**Currency:** /search, /, /{uid} (GET+PUT), /{uid}/archive

**CostRequest:** /search (returns with lines), /, /{uid} (GET+PUT)

**CostRequestLine:** /{uid}/lines (POST), /{uid}/lines/{lineUid} (GET+PUT)

---

## 🎯 Critical Points

### 1. Lines in CostRequest

When doing GET or search on CostRequest, mapper MUST automatically include lines via MapStruct.

### 2. Freeze logic

In `updateCostRequestLine()`, if status becomes COMPLETED:

```java
if(newStatus ==COMPLETED &&oldStatus !=COMPLETED){
CurrencyEntity currency = entityRetrievalHelper.getMustExistCurrencyById(line.getCurrencyUid());
    line.

freezeCurrencySnapshot(currency);
}
```

### 3. getCurrency() method

CostRequestLineEntity already has this method: returns currencySnapshot if exists, otherwise currentCurrency.

### 4. No orphan lines

Lines always belong to a CostRequest parent. Check ownership in retrieve/update.

---

## ✅ Checklist

- [ ] `CurrencyService.java` (5 methods)
- [ ] `CostRequestService.java` (7 methods: 4 for CostRequest + 3 for lines)
- [ ] Modify `CostRequestMapper` to auto-map lines
- [ ] `CurrencyController.java` (5 endpoints)
- [ ] `CostRequestController.java` (7 endpoints: 4 CostRequest + 3 lines)
- [ ] Update swagger with schemas + endpoints
- [ ] Build swagger: `mvn clean install -pl swagger`
- [ ] Test compilation: `mvn clean install -pl core -DskipTests`

---

## 🏗️ Final Architecture

```
CurrencyService + CurrencyController
    └─> Standalone currency management

CostRequestService + CostRequestController
    ├─> CostRequest (search returns with lines)
    └─> CostRequestLines (individual create/retrieve/update)
```

Happy coding! 🚀
