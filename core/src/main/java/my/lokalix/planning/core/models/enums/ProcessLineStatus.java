package my.lokalix.planning.core.models.enums;

import lombok.Getter;

public enum ProcessLineStatus {
  NOT_STARTED("NOT_STARTED", "Not Started"),

  IN_PROGRESS("IN_PROGRESS", "In Progress"),

  COMPLETED("COMPLETED", "Completed"),
  ABORTED("ABORTED", "Aborted");

  private final String value;
  @Getter private final String humanReadableValue;

  ProcessLineStatus(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static ProcessLineStatus fromValue(String value) {
    for (ProcessLineStatus b : ProcessLineStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public boolean isFinalStatus() {
    return this.equals(COMPLETED) || this.equals(ABORTED);
  }

  public boolean isRegressionFrom(ProcessLineStatus current) {
    return this.ordinal() < current.ordinal();
  }
}
