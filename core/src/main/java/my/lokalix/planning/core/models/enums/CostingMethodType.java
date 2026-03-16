package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum CostingMethodType {
  BUDGETARY("BUDGETARY", "Budgetary"),

  HV("HV", "High Volume"),

  LV("LV", "Low Volume"),

  NPI("NPI", "New Product Introduction");

  private final String value;
  private final String humanReadableValue;

  CostingMethodType(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static CostingMethodType fromValue(String value) {
    for (CostingMethodType b : CostingMethodType.values()) {
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
