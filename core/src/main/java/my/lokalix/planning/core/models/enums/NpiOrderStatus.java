package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum NpiOrderStatus {
  READY_TO_START("READY_TO_START", "Ready to start"),

  STARTED("STARTED", "Started"),

  COMPLETED("COMPLETED", "Completed"),

  ABORTED("ABORTED", "Aborted");

  private final String value;
  @Getter private final String humanReadableValue;

  NpiOrderStatus(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public String getValue() {
    return value;
  }

  public static NpiOrderStatus fromValue(String value) {
    for (NpiOrderStatus b : NpiOrderStatus.values()) {
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

  public boolean isFinalStatus() {
    return this.equals(COMPLETED) || this.equals(ABORTED);
  }

  public boolean isAbortable() {
    return this.equals(READY_TO_START) || this.equals(STARTED);
  }
}
