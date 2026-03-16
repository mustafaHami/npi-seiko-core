package my.lokalix.planning.core.models.enums;

import java.io.Serializable;
import lombok.Getter;

@Getter
public enum ToolingStrategy implements Serializable {
  AMORTIZED("AMORTIZED"),

  SEPARATED("SEPARATED");

  private final String value;

  ToolingStrategy(String value) {
    this.value = value;
  }

  public static ToolingStrategy fromValue(String value) {
    for (ToolingStrategy b : ToolingStrategy.values()) {
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
