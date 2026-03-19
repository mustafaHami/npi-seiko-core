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

---

## Forecast Delivery Date

The forecast is **dynamically recalculated** on every process line status update. It answers the question: *"Starting from today, when will we deliver?"*

### Start Date (`baseDate`)

| Material Purchase Status | Base date used |
|---|---|
| `NOT_STARTED` | `max(today, materialPurchaseEstimatedDate)` |
| `IN_PROGRESS` | `max(today, materialLatestDeliveryDate)` — confirmed date from the supplier |
| `COMPLETED` | `today` — materials already received |

### Calculation

```
Forecast = baseDate + Σ remaining days across all unfinished steps

  Per step:
    - COMPLETED   → 0 days (already done)
    - IN_PROGRESS → remainingTimeInDays (manually updated by the operator)
    - NOT_STARTED → planTimeInDays (initial estimated duration)

  Steps included:
    Material Receiving   → materialReceivingPlanTimeInDays
    Production           → productionPlanTimeInDays
    Testing              → testingPlanTimeInDays
    Shipping             → shippingDuration (derived)
    Customer Approval    → customerApprovalDuration (derived)
```

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
Forecast = May 5 + 7 (prod) + 2 (test) + 2 (ship) + 6 (approval)
         = May 5 + 17 business days
         = May 28
→ 28-day delay vs planned delivery date
```
