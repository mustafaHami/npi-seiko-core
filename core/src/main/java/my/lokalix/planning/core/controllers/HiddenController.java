package my.lokalix.planning.core.controllers;

import java.io.IOException;
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

  @Secured({UserRole.SecurityConstants.SUPER_ADMINISTRATOR})
  @PostMapping("/npi-orders/seed")
  public ResponseEntity<List<SWNpiOrder>> seedNpiOrders() {
    List<SWNpiOrderCreate> seeds =
        List.of(
            buildNpiOrderCreate(
                "PO-2024-001",
                "WO-001",
                "PN-A100",
                "Seiko Watch Case A",
                "Seiko",
                50,
                -30,
                5,
                3,
                14,
                7,
                2,
                3),
            buildNpiOrderCreate(
                "PO-2024-002",
                "WO-002",
                "PN-B200",
                "Seiko Dial B200",
                "Seiko",
                100,
                -20,
                5,
                3,
                14,
                7,
                2,
                3),
            buildNpiOrderCreate(
                "PO-2024-003",
                "WO-003",
                "PN-C300",
                "Crown Assembly C300",
                "Grand Seiko",
                25,
                -10,
                5,
                3,
                14,
                7,
                2,
                3),
            buildNpiOrderCreate(
                "PO-2024-004",
                "WO-004",
                "PN-D400",
                "Movement Holder D400",
                "Seiko Instruments",
                200,
                -5,
                5,
                3,
                14,
                7,
                2,
                3),
            buildNpiOrderCreate(
                "PO-2025-001",
                "WO-005",
                "PN-E500",
                "Bracelet Link E500",
                "Seiko",
                75,
                0,
                5,
                3,
                14,
                7,
                2,
                3));

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
      int orderDateOffsetDays,
      int materialPurchase,
      int materialReceiving,
      int production,
      int testing,
      int shipping,
      int customerApproval) {
    double totalHours =
        materialPurchase + materialReceiving + production + testing + shipping + customerApproval;
    SWNpiOrderCreate body = new SWNpiOrderCreate();
    body.setPurchaseOrderNumber(poNumber);
    body.setWorkOrderId(workOrderId);
    body.setPartNumber(partNumber);
    body.setProductName(productName);
    body.setCustomerId(null);
    body.setQuantity(quantity);
    body.setMaterialPurchasePlanTimeInDays(BigDecimal.valueOf(materialPurchase));
    body.setMaterialReceivingPlanTimeInDays(BigDecimal.valueOf(materialReceiving));
    body.setProductionPlanTimeInDays(BigDecimal.valueOf(production));
    body.setTestingPlanTimeInDays(BigDecimal.valueOf(testing));
    body.setShippingPlanTimeInDays(BigDecimal.valueOf(shipping));
    body.setCustomerApprovalPlanTimeInDays(BigDecimal.valueOf(customerApproval));
    body.setOrderDate(LocalDate.now().plusDays(orderDateOffsetDays));
    body.setTargetDeliveryDate(
        LocalDate.now().plusDays((long) (orderDateOffsetDays + totalHours + 5)));
    return body;
  }
}
