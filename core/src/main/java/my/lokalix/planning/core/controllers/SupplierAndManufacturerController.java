package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.SupplierAndManufacturerService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("suppliers-manufacturers")
public class SupplierAndManufacturerController {

  private final SupplierAndManufacturerService supplierAndManufacturerService;

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
  public ResponseEntity<List<SWSupplierAndManufacturer>> listSuppliersOrManufacturers(
      @RequestParam SWSupplierAndManufacturerType supplierAndManufacturer) {
    List<SWSupplierAndManufacturer> result =
        supplierAndManufacturerService.listSuppliersOrManufacturers(supplierAndManufacturer);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWSuppliersAndManufacturersPaginated> searchManufacturers(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestBody SWBasicSearch body) {
    SWSuppliersAndManufacturersPaginated result =
        supplierAndManufacturerService.searchSuppliersAndManufacturers(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWSupplierAndManufacturer> createSupplierAndManufacturer(
      @Valid @RequestBody final SWSupplierAndManufacturerCreate body) {
    SWSupplierAndManufacturer result =
        supplierAndManufacturerService.createSupplierManufacturer(body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWSupplierAndManufacturer> retrieveSupplierManufacturer(
      @PathVariable final UUID uid) {
    SWSupplierAndManufacturer result =
        supplierAndManufacturerService.retrieveSupplierManufacturer(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWSupplierAndManufacturer> updateSupplierManufacturer(
      @PathVariable final UUID uid,
      @Valid @RequestBody final SWSupplierAndManufacturerUpdate body) {
    SWSupplierAndManufacturer result =
        supplierAndManufacturerService.updateSupplierManufacturer(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<Void> archiveSupplierManufacturer(@PathVariable final UUID uid) {
    supplierAndManufacturerService.archiveSupplierManufacturer(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/exists-by-name")
  public ResponseEntity<UUID> existSupplierManufacturerByName(@RequestBody SWStringBody body) {
    return new ResponseEntity<>(
        supplierAndManufacturerService.existSupplierManufacturerByName(body), HttpStatus.OK);
  }
}
