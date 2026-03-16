package my.lokalix.planning.core.models.enums;

import lombok.Getter;

public enum MarkupApprovalStrategy {
  FOR_ALL_QUOTATIONS("FOR_ALL_QUOTATIONS", "For All Quotations"),

  BASED_ON_CUSTOM_RULES("BASED_ON_CUSTOM_RULES", "Based On Custom Rules");

  private final String value;
  @Getter private final String humanReadableValue;

  MarkupApprovalStrategy(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public String getValue() {
    return value;
  }

  public static MarkupApprovalStrategy fromValue(String value) {
    for (MarkupApprovalStrategy b : MarkupApprovalStrategy.values()) {
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
