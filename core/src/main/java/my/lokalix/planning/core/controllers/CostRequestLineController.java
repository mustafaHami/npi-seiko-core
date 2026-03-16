package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.DownloadedFileOutput;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.CostRequestLineService;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("cost-requests/{uid}/lines")
public class CostRequestLineController {

  private final CostRequestLineService costRequestLineService;
  private final AppConfigurationProperties appConfigurationProperties;

  @Secured({
    UserRole.SecurityConstants.PLANNING,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/production-bom/export")
  public ResponseEntity<byte[]> exportProductionBomOfCostRequestLine(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) throws IOException {
    byte[] fileBytes = costRequestLineService.exportProductionBomOfCostRequestLine(uid, lineUid);

    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());

    String filename =
        "production-bom-cost-"
            + datetime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".xlsx";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "attachment; filename=" + filename);
    headers.add("filename", filename);

    return ResponseEntity.ok()
        .headers(headers)
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(fileBytes);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWCostRequestLine> createCostRequestLine(
      @PathVariable final UUID uid, @Valid @RequestBody final SWCostRequestLineCreate body)
      throws Exception {
    SWCostRequestLine result = costRequestLineService.createCostRequestLine(uid, body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{lineUid}")
  public ResponseEntity<SWCostRequestLine> retrieveCostRequestLine(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    SWCostRequestLine result = costRequestLineService.retrieveCostRequestLine(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{lineUid}")
  public ResponseEntity<SWCostRequestLine> updateCostRequestLine(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody final SWCostRequestLineUpdate body)
      throws Exception {
    SWCostRequestLine result = costRequestLineService.updateCostRequestLine(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{lineUid}/validate-for-estimation")
  public ResponseEntity<Void> validateCostRequestForEstimation(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    costRequestLineService.validateCostRequestLineForEstimation(uid, lineUid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{lineUid}/validate-estimation")
  public ResponseEntity<SWCostRequestLine> validateEstimationCostRequestLine(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    SWCostRequestLine result =
        costRequestLineService.validateEstimationCostRequestLine(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{lineUid}/validate-for-ready-for-markup")
  public ResponseEntity<Void> validateCostRequestLineForReadyForMarkup(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    costRequestLineService.validateCostRequestLineForReadyForMarkup(uid, lineUid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{lineUid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveCostRequestLineFilesMetadata(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    List<SWFileInfo> filesMetadata =
        costRequestLineService.retrieveCostRequestLineFilesMetadata(uid, lineUid);
    return new ResponseEntity<>(filesMetadata, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/files/upload")
  public ResponseEntity<List<SWFileInfo>> uploadCostRequestLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @RequestParam("files") MultipartFile[] files)
      throws Exception {
    return new ResponseEntity<>(
        costRequestLineService.uploadCostRequestLineFiles(uid, lineUid, files), HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/files/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadCostRequestLineFiles(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    org.springframework.core.io.Resource resource =
        costRequestLineService.downloadCostRequestLineFiles(uid, lineUid, fileUids);
    String mimeType = Files.probeContentType(resource.getFile().toPath());
    if (mimeType == null) {
      mimeType = "application/octet-stream";
    }
    String encodedFilename = UriUtils.encode(resource.getFilename(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(mimeType))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .body(resource);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/files/delete")
  public ResponseEntity<List<SWFileInfo>> deleteCostRequestLineFiles(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    return new ResponseEntity<>(
        costRequestLineService.deleteCostRequestLineFiles(uid, lineUid, fileUids), HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/download-quotation-breakdown")
  public ResponseEntity<byte[]> downloadQuotationBreakdown(
      @PathVariable UUID uid, @PathVariable UUID lineUid) throws IOException {
    DownloadedFileOutput output = costRequestLineService.downloadQuotationBreakdown(uid, lineUid);

    // Set up the headers
    String encodedFilename = UriUtils.encode(output.getFileName(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .body(output.getFileContent());
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/outsource")
  public ResponseEntity<List<SWCostRequestLine>> outsourceCostRequestLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    List<SWCostRequestLine> result = costRequestLineService.outsourceCostRequestLine(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/revert-for-reestimation")
  public ResponseEntity<List<SWCostRequestLine>> revertCostRequestLineForReestimation(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @RequestParam SWCostRequestStatus costRequestStatus) {
    List<SWCostRequestLine> result =
        costRequestLineService.revertCostRequestLineForReestimation(
            uid, lineUid, costRequestStatus);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{lineUid}/costs")
  public ResponseEntity<List<SWCostRequestLineCostingPerQuantity>> retrieveCostRequestLineCosts(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    List<SWCostRequestLineCostingPerQuantity> result =
        costRequestLineService.retrieveCostRequestLineCosts(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/processes")
  public ResponseEntity<List<SWProcessCostLine>> createProcessCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody SWProcessCostLineCreate body) {
    List<SWProcessCostLine> result =
        costRequestLineService.createProcessCostLine(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/processes/{costingLineUid}")
  public ResponseEntity<List<SWProcessCostLine>> updateProcessCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody SWProcessCostLineUpdate body) {
    List<SWProcessCostLine> result =
        costRequestLineService.updateProcessCostLine(uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/processes/{costingLineUid}/delete")
  public ResponseEntity<List<SWProcessCostLine>> deleteProcessCostLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWProcessCostLine> result =
        costRequestLineService.deleteProcessCostLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/toolings")
  public ResponseEntity<List<SWToolingCostLine>> createToolingCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody SWToolingCostLineCreate body) {
    List<SWToolingCostLine> result =
        costRequestLineService.createToolingCostLine(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/toolings/{costingLineUid}")
  public ResponseEntity<List<SWToolingCostLine>> updateToolingCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody SWToolingCostLineUpdate body) {
    List<SWToolingCostLine> result =
        costRequestLineService.updateToolingCostLine(uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/toolings/{costingLineUid}/delete")
  public ResponseEntity<List<SWToolingCostLine>> deleteToolingCostLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWToolingCostLine> result =
        costRequestLineService.deleteToolingCostLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/others")
  public ResponseEntity<List<SWOtherCostLine>> createOtherCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody SWOtherCostLineCreate body) {
    List<SWOtherCostLine> result = costRequestLineService.createOtherCostLine(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/others/{costingLineUid}")
  public ResponseEntity<List<SWOtherCostLine>> updateOtherCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody SWOtherCostLineUpdate body) {
    List<SWOtherCostLine> result =
        costRequestLineService.updateOtherCostLine(uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/others/{costingLineUid}/delete")
  public ResponseEntity<List<SWOtherCostLine>> deleteOtherCostLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWOtherCostLine> result =
        costRequestLineService.deleteOtherCostLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/others/{costingLineUid}/mask-unmask")
  public ResponseEntity<List<SWOtherCostLine>> maskUnmaskOtherCostLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWOtherCostLine> result =
        costRequestLineService.maskUnmaskOtherCostLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{lineUid}/materials")
  public ResponseEntity<List<SWMaterialCostLine>> retrieveCostRequestLineMaterials(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    List<SWMaterialCostLine> result =
        costRequestLineService.retrieveCostRequestLineMaterials(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/materials")
  public ResponseEntity<List<SWMaterialCostLine>> createMaterialCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody SWMaterialCostLineCreate body) {
    List<SWMaterialCostLine> result =
        costRequestLineService.createMaterialCostLine(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/materials/{costingLineUid}")
  public ResponseEntity<List<SWMaterialCostLine>> updateMaterialCostLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody SWMaterialCostLineUpdate body) {
    List<SWMaterialCostLine> result =
        costRequestLineService.updateMaterialCostLine(uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/materials/{costingLineUid}/supplier")
  public ResponseEntity<List<SWMaterialCostLine>> chooseMaterialCostLineSupplier(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody SWMaterialCostLineSupplierUpdate body) {
    List<SWMaterialCostLine> result =
        costRequestLineService.chooseMaterialCostLineSupplier(uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/materials/{costingLineUid}/material-substitute/supplier")
  public ResponseEntity<List<SWMaterialCostLine>>
      chooseMaterialCostLineSupplierOfMaterialSubstitute(
          @PathVariable UUID uid,
          @PathVariable UUID lineUid,
          @PathVariable UUID costingLineUid,
          @Valid @RequestBody SWMaterialCostLineSupplierUpdate body) {
    List<SWMaterialCostLine> result =
        costRequestLineService.chooseMaterialCostLineSupplierOfMaterialSubstitute(
            uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/materials/{costingLineUid}/mark-unmark")
  public ResponseEntity<List<SWMaterialCostLine>> markOrUnmarkUsedMaterialCostLineForQuote(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWMaterialCostLine> result =
        costRequestLineService.markOrUnmarkUsedMaterialCostLineForQuote(
            uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/materials/{costingLineUid}/delete")
  public ResponseEntity<List<SWMaterialCostLine>> deleteMaterialCostLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWMaterialCostLine> result =
        costRequestLineService.deleteMaterialCostLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{lineUid}/materials/{costingLineUid}/material-substitute")
  public ResponseEntity<SWMaterialCostLine> retrieveMaterialLineMaterialSubstitute(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    SWMaterialCostLine result =
        costRequestLineService.retrieveMaterialLineMaterialSubstitute(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/materials/{costingLineUid}/material-substitute")
  public ResponseEntity<SWMaterialCostLine> createMaterialSubstituteMaterialLine(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody SWMaterialSubstituteCreate body) {
    SWMaterialCostLine result =
        costRequestLineService.createMaterialSubstituteMaterialLine(
            uid, lineUid, costingLineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/materials/{costingLineUid}/material-substitute/delete")
  public ResponseEntity<SWMaterialCostLine> deleteMaterialSubstituteMaterialLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    SWMaterialCostLine result =
        costRequestLineService.deleteMaterialSubstituteMaterialLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{lineUid}/estimation-details")
  public ResponseEntity<List<SWEstimationDetailsPerShipmentToCustomer>>
      retrieveCostRequestLineEstimationDetails(
          @PathVariable UUID uid, @PathVariable UUID lineUid, @RequestParam String currencyCode) {
    List<SWEstimationDetailsPerShipmentToCustomer> result =
        costRequestLineService.retrieveCostRequestLineEstimationDetails(uid, lineUid, currencyCode);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/validate-price")
  public ResponseEntity<String> validateCostRequestLinePrice(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    costRequestLineService.validateCostRequestLinePrice(uid, lineUid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/approve-price-by-management")
  public ResponseEntity<SWCostRequestLine> approvePriceByManagement(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    SWCostRequestLine result = costRequestLineService.approvePriceByManagement(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/reject-price-by-management")
  public ResponseEntity<SWCostRequestLine> rejectPriceByManagement(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @RequestBody SWRejectBody body) {
    SWCostRequestLine result = costRequestLineService.rejectPriceByManagement(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/delete")
  public ResponseEntity<Void> deleteCostRequestLine(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    costRequestLineService.deleteCostRequestLine(uid, lineUid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/abort")
  public ResponseEntity<SWCostRequestLine> abortCostRequestLine(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    SWCostRequestLine result = costRequestLineService.abortCostRequestLine(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/set-markup")
  public ResponseEntity<List<SWEstimationDetailsPerShipmentToCustomer>> setCostRequestLineMarkup(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @RequestParam String currencyCode,
      @Valid @RequestBody SWSetMarkupBody body) {
    List<SWEstimationDetailsPerShipmentToCustomer> result =
        costRequestLineService.setCostRequestLineMarkup(
            uid, lineUid, currencyCode, body.getMarkup());
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/set-tooling-strategy")
  public ResponseEntity<SWCostRequestLine> setCostRequestLineToolingStrategy(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWSetToolingStrategyBody body) {
    SWCostRequestLine result =
        costRequestLineService.setCostRequestLineToolingStrategy(uid, lineUid, body.getStrategy());
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/set-tooling-markup")
  public ResponseEntity<List<SWEstimationDetailsPerShipmentToCustomer>>
      setCostRequestLineToolingMarkup(
          @PathVariable final UUID uid,
          @PathVariable final UUID lineUid,
          @RequestParam String currencyCode,
          @Valid @RequestBody SWSetMarkupBody body) {
    List<SWEstimationDetailsPerShipmentToCustomer> result =
        costRequestLineService.setCostRequestLineToolingMarkup(
            uid, lineUid, currencyCode, body.getMarkup());
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/toolings/{costingLineUid}/outsource")
  public ResponseEntity<List<SWToolingCostLine>> outsourceToolingCostLine(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWToolingCostLine> result =
        costRequestLineService.outsourceToolingCostLine(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{lineUid}/toolings/{costingLineUid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveToolingCostLineFilesMetadata(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID costingLineUid) {
    List<SWFileInfo> filesMetadata =
        costRequestLineService.retrieveToolingCostLineFilesMetadata(uid, lineUid, costingLineUid);
    return new ResponseEntity<>(filesMetadata, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/toolings/{costingLineUid}/files/upload")
  public ResponseEntity<List<SWFileInfo>> uploadToolingCostLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @RequestParam("files") MultipartFile[] files)
      throws Exception {
    return new ResponseEntity<>(
        costRequestLineService.uploadToolingCostLineFiles(uid, lineUid, costingLineUid, files),
        HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/toolings/{costingLineUid}/files/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadToolingCostLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    org.springframework.core.io.Resource resource =
        costRequestLineService.downloadToolingCostLineFiles(uid, lineUid, costingLineUid, fileUids);
    String mimeType = Files.probeContentType(resource.getFile().toPath());
    if (mimeType == null) {
      mimeType = "application/octet-stream";
    }
    String encodedFilename = UriUtils.encode(resource.getFilename(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(mimeType))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .body(resource);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{lineUid}/messages")
  public ResponseEntity<List<SWMessage>> retrieveCostRequestLineMessages(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    List<SWMessage> messages = costRequestLineService.retrieveMessages(uid, lineUid);
    return new ResponseEntity<>(messages, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/messages")
  public ResponseEntity<List<SWMessage>> createCostRequestLineMessage(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody SWMessageCreate body) {
    List<SWMessage> messages = costRequestLineService.createMessage(uid, lineUid, body);
    return new ResponseEntity<>(messages, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{lineUid}/messages/{messageUid}")
  public ResponseEntity<SWMessage> updateCostRequestLineMessage(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID messageUid,
      @Valid @RequestBody SWMessageUpdate body) {
    SWMessage result = costRequestLineService.updateMessage(uid, lineUid, messageUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/messages/{messageUid}/delete")
  public ResponseEntity<SWMessage> deleteCostRequestLineMessage(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID messageUid) {
    SWMessage result = costRequestLineService.deleteMessage(uid, lineUid, messageUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/messages/{messageUid}/undo")
  public ResponseEntity<SWMessage> undoCostRequestLineMessage(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @PathVariable UUID messageUid) {
    SWMessage result = costRequestLineService.undoMessage(uid, lineUid, messageUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{lineUid}/toolings/{costingLineUid}/files/delete")
  public ResponseEntity<List<SWFileInfo>> deleteToolingCostLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @PathVariable UUID costingLineUid,
      @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    return new ResponseEntity<>(
        costRequestLineService.deleteToolingCostLineFiles(uid, lineUid, costingLineUid, fileUids),
        HttpStatus.OK);
  }
}
