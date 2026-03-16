package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.GlobalConfigService;
import my.zkonsulting.planning.generated.model.SWCurrency;
import my.zkonsulting.planning.generated.model.SWGlobalConfig;
import my.zkonsulting.planning.generated.model.SWGlobalConfigPatch;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("global-config")
public class GlobalConfigController {

  private final GlobalConfigService globalConfigService;

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.PLANNING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping
  public ResponseEntity<SWGlobalConfig> getGlobalConfig() {
    SWGlobalConfig result = globalConfigService.getGlobalConfig();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PatchMapping
  public ResponseEntity<SWGlobalConfig> patchGlobalConfig(
      @Valid @RequestBody final SWGlobalConfigPatch body) {
    SWGlobalConfig result = globalConfigService.patchGlobalConfig(body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.PLANNING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping(value = "/system-target-currency")
  public ResponseEntity<SWCurrency> getSystemTargetCurrencyCode() {
    SWCurrency result = globalConfigService.getSystemTargetCurrencyCode();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
