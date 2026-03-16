package my.lokalix.planning.core.exceptions.user;

public class UserIsInactiveException extends RuntimeException implements UserException {
  public UserIsInactiveException() {
    super();
  }

  public UserIsInactiveException(String message) {
    super(message);
  }
}
