package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.ShipmentLocationService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("shipment-locations")
public class ShipmentLocationController {

  private final ShipmentLocationService shipmentLocationService;

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
  public ResponseEntity<List<SWShipmentLocation>> listShipmentLocations() {
    List<SWShipmentLocation> result = shipmentLocationService.listShipmentLocations();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWShipmentLocationsPaginated> searchShipmentLocations(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestBody SWBasicSearch body) {
    SWShipmentLocationsPaginated result =
        shipmentLocationService.searchShipmentLocations(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWShipmentLocation> createShipmentLocation(
      @Valid @RequestBody final SWShipmentLocationCreate body) {
    SWShipmentLocation result = shipmentLocationService.createShipmentLocation(body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWShipmentLocation> retrieveShipmentLocation(@PathVariable final UUID uid) {
    SWShipmentLocation result = shipmentLocationService.retrieveShipmentLocation(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWShipmentLocation> updateShipmentLocation(
      @PathVariable final UUID uid, @Valid @RequestBody final SWShipmentLocationUpdate body) {
    SWShipmentLocation result = shipmentLocationService.updateShipmentLocation(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<Void> archiveShipmentLocation(@PathVariable final UUID uid) {
    shipmentLocationService.archiveShipmentLocation(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
