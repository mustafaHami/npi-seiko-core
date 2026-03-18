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
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.NpiOrderService;
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
@RequestMapping("npi-orders")
public class NpiOrderController {

  private final NpiOrderService npiOrderService;
  private final AppConfigurationProperties appConfigurationProperties;

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
    SWNpiOrdersPaginated result =
        npiOrderService.searchNpiOrders(offset, limit, archivedFilter, body);
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

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/open/export")
  public ResponseEntity<byte[]> exportOpenNpiOrder() throws IOException {
    byte[] fileBytes = npiOrderService.exportOpenNpiOrder();

    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());

    // Set up the headers
    String filename =
        "open-npi-orders-"
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
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/archived/export")
  public ResponseEntity<byte[]> exportArchivedNpiReport() throws IOException {
    byte[] fileBytes = npiOrderService.exportArchivedNpiOrder();

    LocalDateTime datetime =
        TimeUtils.nowLocalDateTime(appConfigurationProperties.getAppTimezone());

    // Set up the headers
    String filename =
        "archive-npi-orders"
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
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/{uid}/process/lines/{lineUid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveProcessLineFilesMetadata(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    List<SWFileInfo> result = npiOrderService.retrieveProcessLineFilesMetadata(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/process/lines/{lineUid}/upload")
  public ResponseEntity<List<SWFileInfo>> uploadProcessLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @RequestParam("files") MultipartFile[] files)
      throws Exception {
    List<SWFileInfo> result = npiOrderService.uploadProcessLineFiles(uid, lineUid, files);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/process/lines/{lineUid}/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadProcessLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    org.springframework.core.io.Resource resource =
        npiOrderService.downloadProcessLineFiles(uid, lineUid, fileUids);
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
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/{uid}/process/lines/{lineUid}/delete")
  public ResponseEntity<List<SWFileInfo>> deleteProcessLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    List<SWFileInfo> result = npiOrderService.deleteProcessLineFiles(uid, lineUid, fileUids);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
