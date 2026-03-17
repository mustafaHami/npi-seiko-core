package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.NpiOrderService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("npi-orders")
public class NpiOrderController {

  private final NpiOrderService npiOrderService;

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWNpiOrder> createNpiOrder(@Valid @RequestBody SWNpiOrderCreate body) {
    SWNpiOrder result = npiOrderService.createNpiOrder(body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWNpiOrder> retrieveNpiOrder(@PathVariable final UUID uid) {
    SWNpiOrder result = npiOrderService.retrieveNpiOrder(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWNpiOrder> updateNpiOrder(
      @PathVariable final UUID uid, @Valid @RequestBody SWNpiOrderUpdate body) {
    SWNpiOrder result = npiOrderService.updateNpiOrder(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWNpiOrdersPaginated> searchNpiOrders(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam SWArchivedFilter archivedFilter,
      @RequestBody SWNpiOrderSearch body) {
    SWNpiOrdersPaginated result = npiOrderService.searchNpiOrders(offset, limit, archivedFilter, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<SWNpiOrder> archiveNpiOrder(@PathVariable final UUID uid) {
    SWNpiOrder result = npiOrderService.archiveNpiOrder(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/abort")
  public ResponseEntity<SWNpiOrder> abortNpiOrder(@PathVariable final UUID uid) {
    SWNpiOrder result = npiOrderService.abortNpiOrder(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

}
