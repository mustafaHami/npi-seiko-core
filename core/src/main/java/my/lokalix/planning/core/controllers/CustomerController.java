package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.CustomerService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("customers")
public class CustomerController {

  private final CustomerService customerService;

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping
  public ResponseEntity<List<SWCustomer>> listCustomers() {
    List<SWCustomer> result = customerService.listCustomers();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWCustomersPaginated> searchCustomers(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestBody SWBasicSearch body) {
    SWCustomersPaginated result = customerService.searchCustomers(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWCustomer> createCustomer(
      @Valid @RequestBody final SWCustomerCreate body) {
    SWCustomer result = customerService.createCustomer(body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWCustomer> retrieveCustomer(@PathVariable final UUID uid) {
    SWCustomer result = customerService.retrieveCustomer(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWCustomer> updateCustomer(
      @PathVariable final UUID uid, @Valid @RequestBody final SWCustomerUpdate body) {
    SWCustomer result = customerService.updateCustomer(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<Void> archiveCustomer(@PathVariable final UUID uid) {
    customerService.archiveCustomer(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}/requestor-names")
  public ResponseEntity<List<String>> retrieveRequestorNamesFromCustomer(
      @PathVariable final UUID uid) {
    List<String> results = customerService.retrieveRequestorNamesFromCustomer(uid);
    return new ResponseEntity<>(results, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}/requestor-names")
  public ResponseEntity<String> setRequestorNamesToCustomer(
      @PathVariable final UUID uid, @Valid @RequestBody final List<String> body) {
    customerService.setRequestorNamesToCustomer(uid, body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/add-requestor-names")
  public ResponseEntity<String> addRequestorNamesToCustomer(
      @PathVariable final UUID uid, @Valid @RequestBody final List<String> body) {
    customerService.addRequestorNamesToCustomer(uid, body);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
