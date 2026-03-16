package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.UnitService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("units")
public class UnitController {

  private final UnitService unitService;

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
  public ResponseEntity<List<SWUnit>> listUnits() {
    List<SWUnit> result = unitService.listUnits();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWUnitsPaginated> searchUnits(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestBody SWBasicSearch body) {
    SWUnitsPaginated result = unitService.searchUnits(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<Void> createUnit(@Valid @RequestBody final SWUnitCreate body) {
    unitService.createUnit(body);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWUnit> retrieveUnit(@PathVariable final UUID uid) {
    SWUnit result = unitService.retrieveUnit(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWUnit> updateUnit(
      @PathVariable final UUID uid, @Valid @RequestBody final SWUnitUpdate body) {
    SWUnit result = unitService.updateUnit(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<Void> archiveUnit(@PathVariable final UUID uid) {
    unitService.archiveUnit(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/exists-by-name")
  public ResponseEntity<UUID> existUnitByName(@RequestBody SWStringBody body) {
    return new ResponseEntity<>(unitService.existUnitByName(body), HttpStatus.OK);
  }
}
