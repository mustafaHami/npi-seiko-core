package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.NpiOrderService;
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
@RequestMapping("npi-orders/{uid}/process")
public class NpiOrderProcessController {

  private final NpiOrderService npiOrderService;

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping
  public ResponseEntity<SWProcess> retrieveNpiOrderProcess(@PathVariable final UUID uid) {
    SWProcess result = npiOrderService.retrieveNpiOrderProcess(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/lines/{lineUid}/status")
  public ResponseEntity<List<SWProcessLine>> updateNpiOrderProcessLineStatus(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWProcessLineStatusUpdateBody body)
      throws Exception {
    List<SWProcessLine> result =
        npiOrderService.updateNpiOrderProcessLineStatus(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/lines/{lineUid}/statuses")
  public ResponseEntity<List<SWProcessLineStatusHistory>>
      retrieveNpiOrderProcessLineStatusesHistory(
          @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    List<SWProcessLineStatusHistory> result =
        npiOrderService.retrieveNpiOrderProcessLineStatusesHistory(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PutMapping("/lines/{lineUid}/remaining-time")
  public ResponseEntity<SWProcessLine> updateProcessLineRemainingTime(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWProcessLineRemainingTimeUpdate body) {
    SWProcessLine result = npiOrderService.updateProcessLineRemainingTime(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/lines/{lineUid}/material-delivery-date/import")
  public ResponseEntity<LocalDate> importMaterialLatestDeliveryDate(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWProcessLineMaterialDeliveryDateImport body) {
    LocalDate result = npiOrderService.importMaterialLatestDeliveryDate(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @GetMapping("/lines/{lineUid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveProcessLineFilesMetadata(
      @PathVariable UUID uid, @PathVariable UUID lineUid) {
    List<SWFileInfo> result = npiOrderService.retrieveProcessLineFilesMetadata(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/lines/{lineUid}/upload")
  public ResponseEntity<List<SWFileInfo>> uploadProcessLineFiles(
      @PathVariable UUID uid,
      @PathVariable UUID lineUid,
      @RequestParam("files") MultipartFile[] files)
      throws Exception {
    List<SWFileInfo> result = npiOrderService.uploadProcessLineFiles(uid, lineUid, files);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/lines/{lineUid}/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadProcessLineFiles(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @Valid @RequestBody List<UUID> fileUids)
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
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/lines/{lineUid}/delete")
  public ResponseEntity<List<SWFileInfo>> deleteProcessLineFiles(
      @PathVariable UUID uid, @PathVariable UUID lineUid, @Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    List<SWFileInfo> result = npiOrderService.deleteProcessLineFiles(uid, lineUid, fileUids);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
