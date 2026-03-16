package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.MaterialCategoryService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("material-categories")
public class MaterialCategoryController {

  private final MaterialCategoryService materialCategoryService;

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
  public ResponseEntity<List<SWMaterialCategory>> listMaterialCategories() {
    List<SWMaterialCategory> result = materialCategoryService.listMaterialCategories();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWMaterialCategoriesPaginated> searchMaterialCategories(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestBody SWBasicSearch body) {
    SWMaterialCategoriesPaginated result =
        materialCategoryService.searchMaterialCategories(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<Void> createMaterialCategory(
      @Valid @RequestBody final SWMaterialCategoryCreate body) {
    materialCategoryService.createMaterialCategory(body);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWMaterialCategory> retrieveMaterialCategory(@PathVariable final UUID uid) {
    SWMaterialCategory result = materialCategoryService.retrieveMaterialCategory(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWMaterialCategory> updateMaterialCategory(
      @PathVariable final UUID uid, @Valid @RequestBody final SWMaterialCategoryUpdate body) {
    SWMaterialCategory result = materialCategoryService.updateMaterialCategory(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<Void> archiveMaterialCategory(@PathVariable final UUID uid) {
    materialCategoryService.archiveMaterialCategory(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/exists-by-name")
  public ResponseEntity<UUID> existMaterialCategoryByName(@RequestBody SWStringBody body) {
    return new ResponseEntity<>(
        materialCategoryService.existMaterialCategoryByName(body), HttpStatus.OK);
  }
}
