package my.lokalix.planning.core.exceptions.user;

public class UserInvalidTokenException extends Exception implements UserException {
  public UserInvalidTokenException(String s) {
    super(s);
  }
}
