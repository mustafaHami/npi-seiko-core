# NPI Order — Date & Forecast Calculations

## The 6 Input Fields

When creating or updating an NPI order, the user provides:

| Field | Type | Description |
|---|---|---|
| `materialPurchaseEstimatedDate` | **Date** | Estimated date when materials will be delivered |
| `materialReceivingPlanTimeInDays` | **Days** | Duration to receive and inspect the materials |
| `productionPlanTimeInDays` | **Days** | Manufacturing duration |
| `testingPlanTimeInDays` | **Days** | Testing duration |
| `shippingEstimatedDate` | **Date** | Estimated shipping date |
| `customerApprovalEstimatedDate` | **Date** | Estimated customer approval date |

> All days are **business days** (Monday–Friday). Input dates cannot fall on a weekend.

---

## Planned Delivery Date

> **= `customerApprovalEstimatedDate`**

This is the contractual delivery date, set at creation time. It never changes unless the user explicitly updates the NPI order.

---

## Derived Durations (calculated automatically)

These durations are not entered by the user — they are **inferred** from the provided dates and days:

```
Estimated end of testing  = materialPurchaseEstimatedDate + materialReceivingDays
                                                          + productionDays
                                                          + testingDays

shippingDuration           = shippingEstimatedDate - Estimated end of testing
customerApprovalDuration   = customerApprovalEstimatedDate - shippingEstimatedDate
```

These derived durations are used to **proportionally weight the shipping and customer approval phases** in all future forecast calculations.

---

## Validations on Create / Update

1. **No input date can fall on a weekend**
2. `shippingEstimatedDate` ≥ `materialPurchaseEstimatedDate + receivingDays + productionDays + testingDays`
   → Shipping cannot happen before testing is complete
3. `customerApprovalEstimatedDate` > `shippingEstimatedDate`
   → Customer approval cannot precede shipment

## Validations on Process Line Status Update

All dates entered during status transitions are also validated:

| Transition | Field | Validations |
|---|---|---|
| Material Purchase → IN_PROGRESS | `materialLatestDeliveryDate` | Cannot fall on a weekend |
| Customer Approval → IN_PROGRESS | `startingCustomerApprovalDate` | Cannot fall on a weekend; must be ≥ actual shipping date |
| Shipment → COMPLETED | `shippingDate` | Cannot fall on a weekend; must be ≥ `materialLatestDeliveryDate` |
| Customer Approval → COMPLETED | `approvalCustomerDate` | Cannot fall on a weekend; must be ≥ `startingCustomerApprovalDate` |

---

## Role-Based Access Control

Write operations on process lines are restricted by role:

| Process Step | Allowed Roles |
|---|---|
| Material Purchase & Material Receiving | `PROCUREMENT`, `ADMINISTRATOR`, `SUPER_ADMINISTRATOR` |
| Production, Testing, Shipping, Customer Approval | `ENGINEERING`, `ADMINISTRATOR`, `SUPER_ADMINISTRATOR` |

This applies to: status updates, remaining time updates, file uploads, file deletions, and material delivery date imports.

---

## Forecast Delivery Date

The forecast is **dynamically recalculated** on every process line status update. It answers the question: *"Starting from today, when will we deliver?"*

### Phase 1 — Base Date (`baseDate`)

The starting point depends on the material purchase step status:

| Material Purchase Status | Base date used |
|---|---|
| `NOT_STARTED` | `max(today, materialPurchaseEstimatedDate)` |
| `IN_PROGRESS` | `max(today, materialLatestDeliveryDate)` — confirmed date from the supplier |
| `COMPLETED` | `today` — materials already received |

### Phase 1 — Accumulate remaining steps

All steps except customer approval (when IN_PROGRESS) are accumulated from `baseDate`:

```
Per step:
  - COMPLETED   → 0 days (already done)
  - IN_PROGRESS → remainingTimeInDays (manually updated by the operator)
  - NOT_STARTED → planTimeInDays (initial estimated duration)

Steps included:
  Material Receiving   → materialReceivingPlanTimeInDays
  Production           → productionPlanTimeInDays
  Testing              → testingPlanTimeInDays
  Shipping             → shippingDuration (derived)

endDateAfterPhase1 = addBusinessDays(baseDate, sum of above)
```

### Phase 2 — Customer Approval (when IN_PROGRESS)

When customer approval is IN_PROGRESS, its start date is fixed by `startingCustomerApprovalDate`.
This date must be respected even if the previous steps finish earlier.

```
approvalAnchor       = startingCustomerApprovalDate  if in the future
                     = today                          otherwise

customerApprovalStart = max(endDateAfterPhase1, approvalAnchor)

Forecast = addBusinessDays(customerApprovalStart, remainingApprovalDays)
```

If customer approval is **NOT_STARTED**, its `customerApprovalDuration` is simply added to phase 1 with no special anchor.

All days are added as **business days** — the resulting date will never land on a weekend.

---

## Concrete Example

| Parameter | Value |
|---|---|
| materialPurchaseEstimatedDate | March 28, 2026 (Friday) |
| materialReceivingDays | 3 days |
| productionDays | 10 days |
| testingDays | 2 days |
| shippingEstimatedDate | April 22, 2026 |
| customerApprovalEstimatedDate | April 30, 2026 |

```
Estimated end of testing   = March 28 + 15 business days = April 18
shippingDuration           = April 22 - April 18         = 2 business days
customerApprovalDuration   = April 30 - April 22         = 6 business days

Planned Delivery Date      = April 30, 2026

--- Forecast on March 19 (all steps NOT_STARTED) ---
baseDate = max(March 19, March 28) = March 28
Forecast = March 28 + 3 + 10 + 2 + 2 + 6 = March 28 + 23 business days = April 30 ✓

--- Forecast on May 5 (materials received, production IN_PROGRESS with 7 days remaining) ---
baseDate = today = May 5
Phase 1  = May 5 + 7 (prod) + 2 (test) + 2 (ship) = May 5 + 11 business days = May 20
Customer approval NOT_STARTED → add 6 days
Forecast = May 20 + 6 business days = May 28
→ 28-day delay vs planned delivery date

--- Forecast on May 20 (shipping complete, customer approval IN_PROGRESS with 4 days remaining,
    startingCustomerApprovalDate = May 25) ---
baseDate         = today = May 20
Phase 1          = May 20 + 0 (shipping done) = May 20
approvalAnchor   = May 25 (future → use it)
customerApprovalStart = max(May 20, May 25) = May 25
Forecast         = May 25 + 4 business days = May 29
→ Even though previous steps are done, approval cannot start before May 25
```
