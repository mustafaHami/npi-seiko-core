package my.lokalix.planning.core.models.enums;

import lombok.Getter;

public enum MaterialStatus {
  TO_BE_ESTIMATED("TO_BE_ESTIMATED", "To Be Estimated"),
  ESTIMATED("ESTIMATED", "Estimated");

  private final String value;
  @Getter private final String humanReadableValue;

  MaterialStatus(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public String getValue() {
    return value;
  }

  public static MaterialStatus fromValue(String value) {
    for (MaterialStatus b : MaterialStatus.values()) {
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
