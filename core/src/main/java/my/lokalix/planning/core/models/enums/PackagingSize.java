package my.lokalix.planning.core.models.enums;

import lombok.Getter;

public enum PackagingSize {
  SMALL("SMALL", "Small"),

  LARGE("LARGE", "Large");

  private final String value;
  @Getter private final String humanReadableValue;

  PackagingSize(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static PackagingSize fromValue(String value) {
    for (PackagingSize b : PackagingSize.values()) {
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
}
