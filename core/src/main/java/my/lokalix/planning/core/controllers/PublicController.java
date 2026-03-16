package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import my.lokalix.planning.core.exceptions.user.UserInvalidTokenException;
import my.lokalix.planning.core.models.enums.ConnectionType;
import my.lokalix.planning.core.services.UserService;
import my.zkonsulting.planning.generated.model.SWLoggedUser;
import my.zkonsulting.planning.generated.model.SWLoginDetails;
import my.zkonsulting.planning.generated.model.SWUserForgotPassword;
import my.zkonsulting.planning.generated.model.SWUserResetPassword;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("public")
@AllArgsConstructor
public class PublicController {

  private final UserService userService;

  @PostMapping("/login")
  public ResponseEntity<SWLoggedUser> loginUser(@Valid @RequestBody SWLoginDetails body) {
    SWLoggedUser loggedUser = userService.login(body, ConnectionType.WEBSITE);
    return ResponseEntity.ok(loggedUser);
  }

  @PostMapping("/app/login")
  public ResponseEntity<SWLoggedUser> appLoginUser(@Valid @RequestBody SWLoginDetails body) {
    SWLoggedUser loggedUser = userService.login(body, ConnectionType.APPLICATION);
    return ResponseEntity.ok(loggedUser);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<String> forgotUserPassword(@Valid @RequestBody SWUserForgotPassword body) {
    userService.manageUserForgotPasswordRequest(body.getEmail());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<String> resetUserPassword(@Valid @RequestBody SWUserResetPassword body)
      throws UserInvalidTokenException {
    userService.resetUserPassword(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @PostMapping("/set-first-password")
  public ResponseEntity<String> setFirstUserPassword(@Valid @RequestBody SWUserResetPassword body)
      throws UserInvalidTokenException {
    userService.setUserPassword(body);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
