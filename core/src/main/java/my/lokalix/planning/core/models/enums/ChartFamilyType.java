package my.lokalix.planning.core.models.enums;

import lombok.Getter;

public enum ChartFamilyType {
  FABRICATION("FABRICATION", "Fabrication"),

  RUBBER_TIP("RUBBER_TIP", "Rubber tip"),

  ASSEMBLY("ASSEMBLY", "Assembly"),

  ALL_AGGREGATED("ALL_AGGREGATED", "All (aggregated)");

  private final String value;
  @Getter private final String humanReadableValue;

  ChartFamilyType(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
