package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.MaterialService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("materials")
public class MaterialController {

  private final MaterialService materialService;

  @Secured({
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.PLANNING,
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWMaterialsPaginated> searchMaterials(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) List<SWMaterialStatus> statuses,
      @RequestParam(required = false) SWMaterialType type,
      @RequestBody SWBasicSearch body) {
    SWMaterialsPaginated result =
        materialService.searchMaterials(offset, limit, body, statuses, type);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWMaterial> retrieveMaterial(@PathVariable final UUID uid) {
    SWMaterial result = materialService.retrieveMaterial(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWMaterial> updateMaterial(
      @PathVariable final UUID uid, @Valid @RequestBody final SWMaterialUpdate body) {
    SWMaterial result = materialService.updateMaterial(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWMaterial> createMaterial(
      @Valid @RequestBody final SWMaterialCreate body) {
    SWMaterial result = materialService.createMaterial(body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<SWMaterial> archiveMaterial(@PathVariable final UUID uid) {
    SWMaterial result = materialService.archiveMaterial(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.PLANNING,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/auto-complete")
  public ResponseEntity<java.util.List<SWMaterial>> autoCompleteMaterial(
      @Valid @RequestBody final SWAutoCompleteMaterialBody body) {
    java.util.List<SWMaterial> result = materialService.autoCompleteMaterial(body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
