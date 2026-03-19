package my.lokalix.planning.core.controllers;

import java.io.IOException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("hidden")
public class HiddenController {

  private final LicenseService licenseService;
  private final NpiOrderService npiOrderService;
  private final CustomerService customerService;

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
  @PostMapping("/customers/upload-excel")
  public ResponseEntity<Integer> uploadCustomersFromExcel(@RequestParam("file") MultipartFile file)
      throws IOException {
    int count = customerService.uploadCustomersFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }
}
