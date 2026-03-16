package my.lokalix.planning.core.handlers;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.exceptions.UserIsInactiveException;
import my.lokalix.planning.core.exceptions.user.CannotDeactivateOneselfException;
import my.lokalix.planning.core.exceptions.user.LicenseLimitExceededException;
import my.lokalix.planning.core.exceptions.user.UserException;
import my.lokalix.planning.core.exceptions.user.UserInvalidTokenException;
import my.zkonsulting.planning.generated.model.SWCustomError;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = Exception.class)
  protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
    // to bypass compilation issues (requiring the switch to handle null, which then causes
    // google-formatting to fail)
    assert ex != null;
    logger.error(ex.getMessage(), ex);
    SWCustomError error = new SWCustomError();
    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    switch (ex) {
      case GenericWithMessageException typedEx -> {
        error.setCode(typedEx.getCode());
        error.setMessage(typedEx.getMessage());
        httpStatus = HttpStatus.CONFLICT;
      }
      case InternalAuthenticationServiceException typedEx -> {
        if (typedEx.getCause() != null && typedEx.getCause() instanceof UserIsInactiveException) {
          error.setCode(SWCustomErrorCode.USER_IS_INACTIVE);
          error.setMessage(typedEx.getMessage());
        } else {
          error.setCode(SWCustomErrorCode.GENERIC_ERROR);
          error.setMessage("Authentication error");
        }
      }
      case AccessDeniedException ignored -> {
        httpStatus = HttpStatus.FORBIDDEN;
        error.setCode(SWCustomErrorCode.ACCESS_FORBIDDEN);
        error.setMessage("You do not have the necessary permissions");
      }
      case BadCredentialsException typedEx -> {
        error.setCode(SWCustomErrorCode.INVALID_EMAIL_OR_PASSWORD);
        error.setMessage("Invalid credentials");
      }
      case EntityNotFoundException typedEx -> {
        error.setCode(SWCustomErrorCode.ENTITY_NOT_FOUND);
        error.setMessage(typedEx.getMessage());
        httpStatus = HttpStatus.NOT_FOUND;
      }
      case EntityExistsException typedEx -> {
        error.setCode(SWCustomErrorCode.ENTITY_ALREADY_EXISTS);
        error.setMessage(typedEx.getMessage());
        httpStatus = HttpStatus.CONFLICT;
      }
      case UserException userException -> {
        manageUserException(userException, error);
        httpStatus = HttpStatus.CONFLICT;
      }
      default -> {
        error.setCode(SWCustomErrorCode.INTERNAL_ERROR);
        error.setMessage("Unexpected error, kindly contact your vendor");
      }
    }
    return handleExceptionInternal(ex, error, new HttpHeaders(), httpStatus, request);
  }

  private void manageUserException(UserException userException, SWCustomError error) {
    switch (userException) {
      case CannotDeactivateOneselfException typedEx -> {
        error.setCode(SWCustomErrorCode.USER_CANNOT_DEACTIVATE_ONESELF);
        error.setMessage(typedEx.getMessage());
      }
      case LicenseLimitExceededException typedEx -> {
        error.setCode(SWCustomErrorCode.LICENSE_LIMIT_EXCEEDED);
        error.setMessage(
            String.format(
                "License limit (%d active users) reached", typedEx.getLicenseLimitActiveUsers()));
      }
      case my.lokalix.planning.core.exceptions.user.UserIsInactiveException typedEx -> {
        error.setCode(SWCustomErrorCode.USER_IS_INACTIVE);
        error.setMessage("This account has been deactivated");
      }
      case UserInvalidTokenException typedEx -> {
        error.setCode(SWCustomErrorCode.USER_RESET_PASSWORD_INVALID_TOKEN);
        error.setMessage(typedEx.getMessage());
      }
      default -> throw new IllegalStateException("Unexpected value: " + userException);
    }
  }
}
