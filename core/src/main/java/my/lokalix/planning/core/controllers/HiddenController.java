package my.lokalix.planning.core.controllers;

import java.io.IOException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.MaterialUploadResult;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.*;
import my.lokalix.planning.core.services.indicator.IndicatorService;
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
  private final SupplierAndManufacturerService supplierAndManufacturerService;
  private final ProcessService processService;
  private final ExternalExchangeRateService externalExchangeRateService;
  private final MaterialService materialService;
  private final ShipmentMethodService shipmentMethodService;
  private final CurrencyService currencyService;
  private final MaterialCategoryService materialCategoryService;
  private final IndicatorService indicatorService;
  private final CustomerService customerService;
  private final TermsAndConditionsService termsAndConditionsService;

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
  @PostMapping("/manufacturers/upload-excel")
  public ResponseEntity<Integer> uploadManufacturersFromExcel(
      @RequestParam("file") MultipartFile file) throws IOException {
    int count = supplierAndManufacturerService.uploadManufacturersFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/processes/upload-excel")
  public ResponseEntity<Integer> uploadProcessesFromExcel(@RequestParam("file") MultipartFile file)
      throws IOException {
    int count = processService.uploadProcessesFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/exchange-rates-fetch")
  public ResponseEntity<Void> fetchExchangeRates() {
    externalExchangeRateService.fetchDailyRates();
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/material-categories/upload-excel")
  public ResponseEntity<Integer> uploadMaterialCategoriesFromExcel(
      @RequestParam("file") MultipartFile file) throws IOException {
    int count = materialCategoryService.uploadMaterialCategoriesFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/materials/upload-excel")
  public ResponseEntity<MaterialUploadResult> uploadMaterialsFromExcel(
      @RequestParam("file") MultipartFile file) throws IOException {
    MaterialUploadResult result = materialService.uploadMaterialsFromExcel(file);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/suppliers/upload-excel")
  public ResponseEntity<Integer> uploadSuppliersFromExcel(@RequestParam("file") MultipartFile file)
      throws IOException {
    int count = supplierAndManufacturerService.uploadSuppliersFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/shipment-methods/upload-excel")
  public ResponseEntity<Integer> uploadShipmentMethodsFromExcel(
      @RequestParam("file") MultipartFile file) throws IOException {
    int count = shipmentMethodService.uploadShipmentMethodsFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/currencies/upload-excel")
  public ResponseEntity<Integer> uploadCurrenciesFromExcel(@RequestParam("file") MultipartFile file)
      throws IOException {
    int count = currencyService.uploadCurrenciesFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/customers/upsert-default-terms-and-conditions")
  public ResponseEntity<Integer> upsertDefaultTermsAndConditions() {
    int count = termsAndConditionsService.upsertDefaultTermsAndConditions();
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/customers/upload-excel")
  public ResponseEntity<Integer> uploadCustomersFromExcel(@RequestParam("file") MultipartFile file)
      throws IOException {
    int count = customerService.uploadCustomersFromExcel(file);
    return new ResponseEntity<>(count, HttpStatus.OK);
  }

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/recalculate-monthly-cost-request")
  public ResponseEntity<String> recalculateMonthlyCostRequest() {
    indicatorService.recalculateMonthlyCostRequestsLeadTimeAndCountIndicator();
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
