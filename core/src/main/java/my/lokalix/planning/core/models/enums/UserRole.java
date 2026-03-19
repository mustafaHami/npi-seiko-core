package my.lokalix.planning.core.models.enums;

import lombok.Getter;

@Getter
public enum UserRole {
  ENGINEERING(Constants.ENGINEERING, SecurityConstants.ENGINEERING),
  PROCUREMENT(Constants.PROCUREMENT, SecurityConstants.PROCUREMENT),
  ADMINISTRATOR(Constants.ADMINISTRATOR, SecurityConstants.ADMINISTRATOR),
  SUPER_ADMINISTRATOR(Constants.SUPER_ADMINISTRATOR, SecurityConstants.SUPER_ADMINISTRATOR);

  private final String value;

  private final String securityValue;

  UserRole(String value, String securityValue) {
    this.value = value;
    this.securityValue = securityValue;
  }

  public static UserRole fromValue(String value) {
    for (UserRole b : UserRole.values()) {
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

  public static class Constants {
    public static final String ENGINEERING = "ENGINEERING";
    public static final String PROCUREMENT = "PROCUREMENT";
    public static final String ADMINISTRATOR = "ADMINISTRATOR";
    public static final String SUPER_ADMINISTRATOR = "SUPER_ADMINISTRATOR";
  }

  public static class SecurityConstants {
    public static final String ENGINEERING = "ROLE_ENGINEERING";
    public static final String PROCUREMENT = "ROLE_PROCUREMENT";
    public static final String ADMINISTRATOR = "ROLE_ADMINISTRATOR";
    public static final String SUPER_ADMINISTRATOR = "ROLE_SUPER_ADMINISTRATOR";
  }
}
