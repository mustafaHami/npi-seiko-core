package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum OutsourcingStatus {
  TO_BE_ESTIMATED("TO_BE_ESTIMATED", "To Be Estimated"),
  ESTIMATED("ESTIMATED", "Estimated"),
  REJECTED("REJECTED", "Rejected");

  private final String value;
  private final String humanReadableValue;

  OutsourcingStatus(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static OutsourcingStatus fromValue(String value) {
    for (OutsourcingStatus b : OutsourcingStatus.values()) {
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

  public boolean isEstimated() {
    return this.equals(ESTIMATED);
  }
}
