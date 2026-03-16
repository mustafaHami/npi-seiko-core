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
import my.lokalix.planning.core.services.CostRequestService;
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
@RequestMapping("cost-requests")
public class CostRequestController {

  private final CostRequestService costRequestService;
  private final AppConfigurationProperties appConfigurationProperties;

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/open/export")
  public ResponseEntity<byte[]> exportOpenCostRequests() throws IOException {
    byte[] fileBytes = costRequestService.exportOpenCostRequests();

    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());

    // Set up the headers
    String filename =
        "quotation-status-"
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
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/archived/export")
  public ResponseEntity<byte[]> exportArchivedCostRequests() throws IOException {
    byte[] fileBytes = costRequestService.exportArchivedCostRequests();

    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());

    // Set up the headers
    String filename =
        "archive-cost-requests"
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
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/standard-bom/download")
  public ResponseEntity<byte[]> downloadStandardBom() throws IOException {
    byte[] fileBytes = costRequestService.downloadStandardBom();

    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());

    String filename =
        "standard-bom-" + datetime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";

    // Set up the headers
    String encodedFilename = UriUtils.encode(filename, StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .body(fileBytes);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/search")
  public ResponseEntity<SWCostRequestsPaginated> searchCostRequests(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "NON_ARCHIVED_ONLY") SWArchivedFilter archivedFilter,
      @RequestBody SWCostRequestSearch body) {
    SWCostRequestsPaginated result =
        costRequestService.searchCostRequests(offset, limit, archivedFilter, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/engineering/search")
  public ResponseEntity<SWCostRequestsPaginated> searchEngineeringCostRequests(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "NON_ARCHIVED_ONLY") SWArchivedFilter archivedFilter,
      @RequestBody SWCostRequestSearch body) {
    SWCostRequestsPaginated result =
        costRequestService.searchEngineeringCostRequests(offset, limit, archivedFilter, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping
  public ResponseEntity<SWCostRequest> createCostRequest(
      @Valid @RequestBody final SWCostRequestCreate body) throws Exception {
    SWCostRequest result = costRequestService.createCostRequest(body);
    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/{uid}")
  public ResponseEntity<SWCostRequest> retrieveCostRequest(@PathVariable final UUID uid) {
    SWCostRequest result = costRequestService.retrieveCostRequest(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/{uid}")
  public ResponseEntity<SWCostRequest> updateCostRequest(
      @PathVariable final UUID uid, @Valid @RequestBody final SWCostRequestUpdate body)
      throws Exception {
    SWCostRequest result = costRequestService.updateCostRequest(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/standard-bom/import")
  public ResponseEntity<Void> importCostRequestStandardBom(
      @PathVariable UUID uid, @RequestParam("files") MultipartFile[] files) throws Exception {
    costRequestService.importCostRequestStandardBom(uid, files);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/custom-bom/import")
  public ResponseEntity<Void> importCostRequestCustomBom(
      @PathVariable UUID uid, @RequestBody SWCustomBomImportBody body) throws Exception {
    costRequestService.importCostRequestCustomBom(uid, body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{uid}/substitute-material-comment")
  public ResponseEntity<SWSubstituteMaterialsComment> retrieveCostRequestSubstituteMaterialsComment(
      @PathVariable UUID uid) throws Exception {
    SWSubstituteMaterialsComment result =
        costRequestService.retrieveCostRequestSubstituteMaterialsComment(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/validate-for-review")
  public ResponseEntity<Void> validateCostRequestForReview(@PathVariable final UUID uid) {
    costRequestService.validateCostRequestForReview(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/validate-for-estimation")
  public ResponseEntity<Void> validateCostRequestForEstimation(@PathVariable final UUID uid) {
    costRequestService.validateCostRequestForEstimation(uid);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/archive")
  public ResponseEntity<SWCostRequest> archiveCostRequest(@PathVariable final UUID uid) {
    SWCostRequest result = costRequestService.archiveCostRequest(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/abort")
  public ResponseEntity<SWCostRequest> abortCostRequest(@PathVariable final UUID uid) {
    SWCostRequest result = costRequestService.abortCostRequest(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/new-revision")
  public ResponseEntity<SWCostRequest> createCostRequestNewRevision(@PathVariable final UUID uid)
      throws Exception {
    SWCostRequest result = costRequestService.createCostRequestNewRevision(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/{uid}/clone")
  public ResponseEntity<SWCostRequest> cloneCostRequest(
      @PathVariable final UUID uid, @RequestBody SWCostRequestClone body) throws Exception {
    SWCostRequest result = costRequestService.cloneCostRequest(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{uid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveCostRequestFilesMetadata(@PathVariable UUID uid) {
    List<SWFileInfo> filesMetadata = costRequestService.retrieveCostRequestFilesMetadata(uid);
    return new ResponseEntity<>(filesMetadata, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/files/upload")
  public ResponseEntity<List<SWFileInfo>> uploadCostRequestFiles(
      @PathVariable UUID uid, @RequestParam("files") MultipartFile[] files) throws Exception {
    return new ResponseEntity<>(
        costRequestService.uploadCostRequestFiles(uid, files), HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/files/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadCostRequestFiles(
      @PathVariable UUID uid, @Valid @RequestBody List<UUID> fileUids) throws Exception {
    org.springframework.core.io.Resource resource =
        costRequestService.downloadCostRequestFiles(uid, fileUids);
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
  @PostMapping("/{uid}/files/delete")
  public ResponseEntity<List<SWFileInfo>> deleteCostRequestFiles(
      @PathVariable UUID uid, @Valid @RequestBody List<UUID> fileUids) throws Exception {
    return new ResponseEntity<>(
        costRequestService.deleteCostRequestFiles(uid, fileUids), HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{uid}/messages")
  public ResponseEntity<List<SWMessage>> retrieveMessages(@PathVariable UUID uid) {
    List<SWMessage> messages = costRequestService.retrieveMessages(uid);
    return new ResponseEntity<>(messages, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/messages")
  public ResponseEntity<List<SWMessage>> createMessages(
      @PathVariable UUID uid, @Valid @RequestBody SWMessageCreate body) {
    List<SWMessage> messages = costRequestService.createMessage(uid, body);
    return new ResponseEntity<>(messages, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/{uid}/messages/{messageUid}")
  public ResponseEntity<SWMessage> updateMessages(
      @PathVariable UUID uid,
      @PathVariable UUID messageUid,
      @Valid @RequestBody SWMessageUpdate body) {
    SWMessage result = costRequestService.updateMessages(uid, messageUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/messages/{messageUid}/delete")
  public ResponseEntity<SWMessage> deleteMessages(
      @PathVariable UUID uid, @PathVariable UUID messageUid) {
    SWMessage result = costRequestService.deleteMessages(uid, messageUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/messages/{messageUid}/undo")
  public ResponseEntity<SWMessage> undoMessages(
      @PathVariable UUID uid, @PathVariable UUID messageUid) {
    SWMessage result = costRequestService.undoMessages(uid, messageUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.MANAGEMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("lines/pending-approval")
  public ResponseEntity<List<SWCustomCostRequestLine>> listOfAllCostRequestLinesPendingApproval() {
    return new ResponseEntity<>(
        costRequestService.listOfAllCostRequestLinesPendingApproval(), HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/generate-quotation-pdf")
  public ResponseEntity<byte[]> generateCostRequestPdf(
      @PathVariable UUID uid, @RequestBody SWGenerateQuotationPdfBody body) throws Exception {
    DownloadedFileOutput downloadedFileOutput =
        costRequestService.generateCostRequestPdf(uid, body);

    // Set up the headers
    String encodedFilename =
        UriUtils.encode(downloadedFileOutput.getFileName(), StandardCharsets.UTF_8);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .contentType(MediaType.parseMediaType("application/pdf"))
        .body(downloadedFileOutput.getFileContent());
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/download-quotation-pdf")
  public ResponseEntity<byte[]> downloadCostRequestPdf(@PathVariable UUID uid) throws Exception {
    DownloadedFileOutput downloadedFileOutput = costRequestService.downloadCostRequestPdf(uid);

    // Set up the headers
    String encodedFilename =
        UriUtils.encode(downloadedFileOutput.getFileName(), StandardCharsets.UTF_8);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .contentType(MediaType.parseMediaType("application/pdf"))
        .body(downloadedFileOutput.getFileContent());
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/approved-by-customer")
  public ResponseEntity<String> approvedByCustomer(@PathVariable UUID uid) {
    costRequestService.approvedByCustomer(uid);
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/rejected-by-customer")
  public ResponseEntity<String> rejectedByCustomer(
      @PathVariable UUID uid, @RequestBody SWRejectBody body) {
    costRequestService.rejectedByCustomer(uid, body);
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/new-revision-created")
  public ResponseEntity<String> createdNewRevisionOfCostRequest(@PathVariable UUID uid) {
    costRequestService.createdNewRevisionOfCostRequest(uid);
    return new ResponseEntity<String>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROJECT_MANAGER,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/extend-expiration")
  public ResponseEntity<SWCostRequest> extendCostRequestExpiration(
      @PathVariable UUID uid, @Valid @RequestBody SWCostRequestExtendExpiration body) {
    SWCostRequest result = costRequestService.extendCostRequestExpiration(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
