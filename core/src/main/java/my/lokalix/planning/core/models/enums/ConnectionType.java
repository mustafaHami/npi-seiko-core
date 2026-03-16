package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum ConnectionType {
  WEBSITE("WEBSITE"),

  APPLICATION("APPLICATION");

  private final String value;

  ConnectionType(String value) {
    this.value = value;
  }

  public static ConnectionType fromValue(String value) {
    for (ConnectionType b : ConnectionType.values()) {
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
