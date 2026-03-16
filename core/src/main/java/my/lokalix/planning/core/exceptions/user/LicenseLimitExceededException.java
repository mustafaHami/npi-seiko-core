package my.lokalix.planning.core.exceptions.user;

import lombok.Getter;

public class LicenseLimitExceededException extends RuntimeException implements UserException {

  @Getter private final long licenseLimitActiveUsers;

  public LicenseLimitExceededException(long licenseLimitActiveUsers) {
    super();
    this.licenseLimitActiveUsers = licenseLimitActiveUsers;
  }
}
