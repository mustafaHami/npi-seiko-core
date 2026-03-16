package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.MaterialSupplierService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("materials/{uid}/suppliers")
public class MaterialSupplierController {

  private final MaterialSupplierService materialSupplierService;

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping
  public ResponseEntity<List<SWMaterialSupplier>> listMaterialSuppliers(
      @PathVariable final UUID uid) {
    List<SWMaterialSupplier> result = materialSupplierService.listMaterialSuppliers(uid);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWMaterialSupplier> createMaterialSupplier(
      @PathVariable final UUID uid, @Valid @RequestBody final SWMaterialSupplierCreate body) {
    SWMaterialSupplier result = materialSupplierService.createMaterialSupplier(uid, body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWMaterialSuppliersPaginated> searchMaterialSuppliers(
      @PathVariable final UUID uid,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestBody SWBasicSearch body) {
    SWMaterialSuppliersPaginated result =
        materialSupplierService.searchMaterialSuppliers(uid, offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{materialSupplierUid}")
  public ResponseEntity<SWMaterialSupplier> retrieveMaterialSupplier(
      @PathVariable final UUID uid, @PathVariable final UUID materialSupplierUid) {
    SWMaterialSupplier result =
        materialSupplierService.retrieveMaterialSupplier(uid, materialSupplierUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{materialSupplierUid}")
  public ResponseEntity<SWMaterialSupplier> updateMaterialSupplier(
      @PathVariable final UUID uid,
      @PathVariable final UUID materialSupplierUid,
      @Valid @RequestBody final SWMaterialSupplierUpdate body) {
    SWMaterialSupplier result =
        materialSupplierService.updateMaterialSupplier(uid, materialSupplierUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{materialSupplierUid}/archive")
  public ResponseEntity<Void> archiveMaterialSupplier(
      @PathVariable final UUID uid, @PathVariable final UUID materialSupplierUid) {
    materialSupplierService.archiveMaterialSupplier(uid, materialSupplierUid);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
