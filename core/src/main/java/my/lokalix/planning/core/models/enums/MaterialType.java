package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum MaterialType {
  DIRECT("DIRECT", "Direct"),
  INDIRECT("INDIRECT", "Indirect");

  private final String value;
  private final String humanReadableValue;

  MaterialType(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static MaterialType fromValue(String value) {
    for (MaterialType b : MaterialType.values()) {
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
}
