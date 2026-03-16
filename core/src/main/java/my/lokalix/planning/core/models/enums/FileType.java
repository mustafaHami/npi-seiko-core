package my.lokalix.planning.core.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FileType {
  QUOTATION_PDF("QUOTATION_PDF"),

  ANY("ANY");

  private final String value;

  FileType(String value) {
    this.value = value;
  }

  @JsonCreator
  public static FileType fromValue(String value) {
    for (FileType b : FileType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
