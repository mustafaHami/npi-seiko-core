package my.lokalix.planning.core.controllers;

import jakarta.annotation.Resource;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.indicator.IndicatorService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("indicators")
public class IndicatorController {

  @Resource private IndicatorService indicatorService;

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/monthly-completed-cost/graph")
  public ResponseEntity<SWGraph> retrieveCostRequestsCountIndicatorAsGraph(
      @RequestBody SWIndicatorsBody body) {
    SWGraph graph = indicatorService.buildCostRequestsCountIndicatorAsGraph(body);
    return new ResponseEntity<>(graph, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/monthly-completed-cost-lead-time/graph")
  public ResponseEntity<SWGraphWhisker> retrieveCostRequestsLeadTimeIndicatorAsGraph(
      @RequestBody SWIndicatorsBody body) {
    SWGraphWhisker graph = indicatorService.buildCostRequestsLeadTimeIndicatorAsGraph(body);
    return new ResponseEntity<>(graph, HttpStatus.OK);
  }
}
