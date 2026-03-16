package my.lokalix.planning.core.exceptions;

public class ShouldNeverHappenException extends RuntimeException {

  public ShouldNeverHappenException() {
    super();
  }

  public ShouldNeverHappenException(Throwable e) {
    super(e);
  }
}
