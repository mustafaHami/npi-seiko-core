package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum DisconnectionReasonType {
  ROLE_CHANGED("ROLE_CHANGED"),

  DEACTIVATE("APPLICATION"),

  CONNECTED_OTHER_DEVICE("CONNECTED_OTHER_DEVICE"),

  LOGOUT("LOGOUT");

  private final String value;

  DisconnectionReasonType(String value) {
    this.value = value;
  }

  public static DisconnectionReasonType fromValue(String value) {
    for (DisconnectionReasonType b : DisconnectionReasonType.values()) {
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
