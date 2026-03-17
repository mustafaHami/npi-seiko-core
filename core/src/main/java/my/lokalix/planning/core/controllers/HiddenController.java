package my.lokalix.planning.core.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.NpiOrderMapper;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.services.*;
import my.zkonsulting.planning.generated.model.SWNpiOrder;
import my.zkonsulting.planning.generated.model.SWNpiOrderCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("hidden")
public class HiddenController {

  private final LicenseService licenseService;
  private final NpiOrderRepository npiOrderRepository;
  private final NpiOrderMapper npiOrderMapper;
  private final NpiOrderService npiOrderService;
  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/license")
  public ResponseEntity<String> upsertLicense(@RequestParam long activeUsersLimit)
      throws IllegalBlockSizeException, BadPaddingException {
    licenseService.updateLicense(activeUsersLimit);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @GetMapping("/license")
  public ResponseEntity<Long> retrieveCurrentNumberOfLicenses()
      throws IllegalBlockSizeException, BadPaddingException {
    long nbLicenses = licenseService.retrieveCurrentLicenseMaxNumberOfActiveUsers();
    return new ResponseEntity<>(nbLicenses, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @GetMapping("/npi-orders")
  public ResponseEntity<List<SWNpiOrder>> getAllNpiOrders() {
    List<SWNpiOrder> orders = npiOrderMapper.toListSWNpiOrder(npiOrderRepository.findAll());
    return new ResponseEntity<>(orders, HttpStatus.OK);
  }
}
