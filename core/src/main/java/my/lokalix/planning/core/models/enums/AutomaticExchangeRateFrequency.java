package my.lokalix.planning.core.models.enums;

import lombok.Getter;

public enum AutomaticExchangeRateFrequency {
  EVERY_HOUR("EVERY_HOUR", "Every Hour"),

  EVERY_DAY("EVERY_DAY", "Every Day"),

  EVERY_WEEK("EVERY_WEEK", "Every Week");

  private final String value;
  @Getter private final String humanReadableValue;

  AutomaticExchangeRateFrequency(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public String getValue() {
    return value;
  }

  public static AutomaticExchangeRateFrequency fromValue(String value) {
    for (AutomaticExchangeRateFrequency b : AutomaticExchangeRateFrequency.values()) {
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
