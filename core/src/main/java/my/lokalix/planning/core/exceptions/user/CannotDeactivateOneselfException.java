package my.lokalix.planning.core.exceptions.user;

public class CannotDeactivateOneselfException extends RuntimeException implements UserException {
  public CannotDeactivateOneselfException(String s) {
    super(s);
  }
}
