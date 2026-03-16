package my.lokalix.planning.core.exceptions;

import lombok.Getter;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.security.core.AuthenticationException;

@Getter
public class UserIsInactiveException extends AuthenticationException {

  private final SWCustomErrorCode code;

  public UserIsInactiveException(String msg, SWCustomErrorCode code) {
    super(msg);
    this.code = code;
  }
}
