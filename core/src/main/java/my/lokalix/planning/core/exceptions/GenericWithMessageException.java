package my.lokalix.planning.core.exceptions;

import lombok.Getter;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;

@Getter
public class GenericWithMessageException extends RuntimeException {
  private final SWCustomErrorCode code;

  public GenericWithMessageException(String msg, SWCustomErrorCode code) {
    super(msg);
    this.code = code;
  }

  public GenericWithMessageException(String msg) {
    super(msg);
    this.code = SWCustomErrorCode.GENERIC_ERROR;
  }
}
