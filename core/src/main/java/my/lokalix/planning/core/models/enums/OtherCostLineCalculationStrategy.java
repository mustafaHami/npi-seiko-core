package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum OtherCostLineCalculationStrategy {
  AS_IS("AS_IS", "As is"),

  MULTIPLIED_BY_QUANTITY("MULTIPLIED_BY_QUANTITY", "Multiplied by Qty"),

  DIVIDED_BY_QUANTITY("DIVIDED_BY_QUANTITY", "Divided by Qty"),
  ;

  private final String value;
  private final String humanReadableValue;

  OtherCostLineCalculationStrategy(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static OtherCostLineCalculationStrategy fromValue(String value) {
    for (OtherCostLineCalculationStrategy b : OtherCostLineCalculationStrategy.values()) {
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
