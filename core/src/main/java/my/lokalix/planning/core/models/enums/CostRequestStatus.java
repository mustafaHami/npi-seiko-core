package my.lokalix.planning.core.models.enums;

import java.util.List;
import lombok.Getter;

@Getter
public enum CostRequestStatus {
  PENDING_INFORMATION("PENDING_INFORMATION", "Pending information"),

  READY_FOR_REVIEW("READY_FOR_REVIEW", "Ready for review"),

  READY_TO_ESTIMATE("READY_TO_ESTIMATE", "Ready to estimate"),

  READY_TO_VALIDATE("READY_TO_VALIDATE", "Ready to validate"),

  READY_FOR_MARKUP("READY_FOR_MARKUP", "Ready for markup"),

  PENDING_APPROVAL("PENDING_APPROVAL", "Pending approval"),

  PRICE_APPROVED("PRICE_APPROVED", "Price approved"),

  PRICE_REJECTED("PRICE_REJECTED", "Price rejected"),

  PENDING_REESTIMATION("PENDING_REESTIMATION", "Pending re-estimation"),

  READY_TO_QUOTE("READY_TO_QUOTE", "Ready to quote"),

  ACTIVE("ACTIVE", "Active"),

  WON("WON", "Won"),

  LOST("LOST", "Lost"),
  NEW_REVISION_CREATED("NEW_REVISION_CREATED", "New revision"),

  ABORTED("ABORTED", "Aborted");
  private final String value;
  private final String humanReadableValue;

  CostRequestStatus(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static CostRequestStatus fromValue(String value) {
    for (CostRequestStatus b : CostRequestStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static List<CostRequestStatus> getStatusesBeforeMarkup() {
    return List.of(PENDING_INFORMATION, READY_FOR_REVIEW, READY_TO_ESTIMATE, READY_TO_VALIDATE);
  }

  public static List<CostRequestStatus> getEstimatedStatuses() {
    return List.of(
        READY_FOR_MARKUP,
        READY_TO_QUOTE,
        ACTIVE,
        WON,
        LOST,
        PRICE_APPROVED,
        PRICE_REJECTED,
        PENDING_APPROVAL);
  }

  public static List<CostRequestStatus> getAfterEngineeringEstimation() {
    return List.of(
        READY_TO_VALIDATE,
        READY_FOR_MARKUP,
        READY_TO_QUOTE,
        ACTIVE,
        WON,
        LOST,
        PRICE_APPROVED,
        PRICE_REJECTED,
        PENDING_APPROVAL);
  }

  public static List<CostRequestStatus> getDataFreezeStatuses() {
    return List.of(ABORTED, ACTIVE, WON, LOST, NEW_REVISION_CREATED);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public boolean isDataFreezeStatus() {
    return getDataFreezeStatuses().contains(this);
  }

  public boolean isActiveDataFreezeStatus() {
    return this.equals(ACTIVE)
        || this.equals(WON)
        || this.equals(LOST)
        || this.equals(NEW_REVISION_CREATED);
  }

  public boolean isFinalStatus() {
    return this.equals(ABORTED)
        || this.equals(WON)
        || this.equals(LOST)
        || this.equals(NEW_REVISION_CREATED);
  }

  public String finalStatus() {
    return "ABORTED, ACTIVE, WON, LOST";
  }

  public boolean isPendingAndUpdatable() {
    return this.equals(PENDING_INFORMATION) || this.equals(READY_FOR_REVIEW);
  }

  public boolean isDeletable() {
    return this.equals(PENDING_INFORMATION) || this.equals(READY_FOR_REVIEW);
  }

  public boolean isMarkupUpdatable() {
    return this.equals(READY_FOR_MARKUP) || this.equals(PRICE_REJECTED);
  }

  public boolean isEstimatedStatus() {
    return this.equals(READY_FOR_MARKUP)
        || this.equals(READY_TO_QUOTE)
        || this.equals(ACTIVE)
        || this.equals(WON)
        || this.equals(LOST)
        || this.equals(NEW_REVISION_CREATED)
        || this.equals(PRICE_APPROVED)
        || this.equals(PRICE_REJECTED)
        || this.equals(PENDING_APPROVAL);
  }

  public boolean isAfterEngineeringEstimation() {
    return this.equals(READY_TO_VALIDATE)
        || this.equals(READY_FOR_MARKUP)
        || this.equals(READY_TO_QUOTE)
        || this.equals(ACTIVE)
        || this.equals(WON)
        || this.equals(LOST)
        || this.equals(NEW_REVISION_CREATED)
        || this.equals(PRICE_APPROVED)
        || this.equals(PRICE_REJECTED)
        || this.equals(PENDING_APPROVAL);
  }
}
