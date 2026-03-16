package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.UserService;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.zkonsulting.planning.generated.model.*;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("users")
public class UserController {

  private final UserService userService;

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWUsersPaginated> searchUsers(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "ACTIVE_ONLY") SWActiveFilter activeFilter,
      @RequestBody SWBasicSearch body) {
    SWUsersPaginated usersPaginated = userService.searchUsers(offset, limit, activeFilter, body);
    return new ResponseEntity<>(usersPaginated, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWUser> retrieveUser(@PathVariable UUID uid) {
    SWUser user = userService.retrieveUser(uid);
    return new ResponseEntity<>(user, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/resend-user-creation-email")
  public ResponseEntity<String> resendUserCreationEmail(@PathVariable UUID uid)
      throws BadRequestException {
    userService.resendUserCreationEmail(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/email")
  public ResponseEntity<String> createUserWithEmail(
      @Valid @RequestBody SWUserWithEmailCreate body) {
    UUID uid = userService.createUserWithEmail(body);
    HttpHeaders headers = new HttpHeaders();
    headers.add(GlobalConstants.HEADER_UID, uid.toString());
    return new ResponseEntity<>(headers, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/username")
  public ResponseEntity<String> createUserWithUsername(
      @Valid @RequestBody SWUserWithUsernameCreate body) {
    UUID uid = userService.createUserWithUsername(body);
    HttpHeaders headers = new HttpHeaders();
    headers.add(GlobalConstants.HEADER_UID, uid.toString());
    return new ResponseEntity<>(headers, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/activate")
  public ResponseEntity<String> activateUser(@PathVariable UUID uid) {
    userService.activateUser(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/deactivate")
  public ResponseEntity<String> deactivateUser(@PathVariable UUID uid) {
    userService.deactivateUser(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/email/{uid}")
  public ResponseEntity<SWUser> updateUserWithEmail(
      @PathVariable UUID uid, @Valid @RequestBody SWUserWithEmailUpdate body)
      throws BadRequestException {
    SWUser updatedUser = userService.updateUserWithEmail(uid, body);
    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/username/{uid}")
  public ResponseEntity<SWUser> updateUserWithUsername(
      @PathVariable UUID uid, @Valid @RequestBody SWUserWithUsernameUpdate body)
      throws BadRequestException {
    SWUser updatedUser = userService.updateUserWithUsername(uid, body);
    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/username/{uid}/password")
  public ResponseEntity<SWUser> updateUserPassword(
      @PathVariable UUID uid, @Valid @RequestBody SWUserUsernameResetPassword body)
      throws BadRequestException {
    SWUser updatedUser = userService.updateUserPassword(uid, body);
    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/check-license-validity")
  public ResponseEntity<Boolean> checkLicenseValidity() throws BadRequestException {
    boolean licenseValidity = userService.checkLicenseValidity();
    return new ResponseEntity<>(licenseValidity, HttpStatus.OK);
  }

  @PostMapping("/logout")
  public ResponseEntity<String> logoutUser() {
    userService.logoutUser();
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
