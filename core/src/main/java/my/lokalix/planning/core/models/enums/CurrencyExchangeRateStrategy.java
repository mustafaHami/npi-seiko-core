package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum CurrencyExchangeRateStrategy {
  AUTOMATICALLY_UPDATED("AUTOMATICALLY_UPDATED", "Automatically Updated"),

  MANUALLY_UPDATED("MANUALLY_UPDATED", "Manually Updated");

  private final String value;
  private final String humanReadableValue;

  CurrencyExchangeRateStrategy(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static CurrencyExchangeRateStrategy fromValue(String value) {
    for (CurrencyExchangeRateStrategy b : CurrencyExchangeRateStrategy.values()) {
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
