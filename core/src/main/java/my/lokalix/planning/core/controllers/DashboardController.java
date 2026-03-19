package my.lokalix.planning.core.controllers;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.DashboardService;
import my.zkonsulting.planning.generated.model.SWDashboard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping
  public ResponseEntity<SWDashboard> retrieveDashboard() {
    SWDashboard result = dashboardService.retrieveDashboard();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
