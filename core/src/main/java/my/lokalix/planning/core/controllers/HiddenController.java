package my.lokalix.planning.core.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.enums.UserRole;
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
  @PostMapping("/npi-orders/seed")
  public ResponseEntity<List<SWNpiOrder>> seedNpiOrders() {
    List<SWNpiOrderCreate> seeds =
        List.of(
            buildNpiOrderCreate(
                "PO-2024-001", "WO-001", "PN-A100", "Seiko Watch Case A", "Seiko", 50, 14, 7, -30),
            buildNpiOrderCreate(
                "PO-2024-002", "WO-002", "PN-B200", "Seiko Dial B200", "Seiko", 100, 10, 5, -20),
            buildNpiOrderCreate(
                "PO-2024-003",
                "WO-003",
                "PN-C300",
                "Crown Assembly C300",
                "Grand Seiko",
                25,
                21,
                10,
                -10),
            buildNpiOrderCreate(
                "PO-2024-004",
                "WO-004",
                "PN-D400",
                "Movement Holder D400",
                "Seiko Instruments",
                200,
                7,
                3,
                -5),
            buildNpiOrderCreate(
                "PO-2025-001", "WO-005", "PN-E500", "Bracelet Link E500", "Seiko", 75, 14, 7, 0));

    List<SWNpiOrder> created = seeds.stream().map(npiOrderService::createNpiOrder).toList();
    return new ResponseEntity<>(created, HttpStatus.OK);
  }

  private SWNpiOrderCreate buildNpiOrderCreate(
      String poNumber,
      String workOrderId,
      String partNumber,
      String productName,
      String customerName,
      int quantity,
      int productionPlanTime,
      int testingPlanTime,
      int orderDateOffsetDays) {
    SWNpiOrderCreate body = new SWNpiOrderCreate();
    body.setPurchaseOrderNumber(poNumber);
    body.setWorkOrderId(workOrderId);
    body.setPartNumber(partNumber);
    body.setProductName(productName);
    body.setCustomerName(customerName);
    body.setQuantity(quantity);
    body.setProductionPlanTime(new BigDecimal(productionPlanTime));
    body.setTestingPlanTime(new BigDecimal(testingPlanTime));
    body.setOrderDate(LocalDate.now().plusDays(orderDateOffsetDays));
    body.setTargetDeliveryDate(
        LocalDate.now().plusDays(orderDateOffsetDays + productionPlanTime + testingPlanTime + 5));
    return body;
  }
}
