package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.ToolingCostLineService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("toolings")
public class ToolingCostLineOutsourcedController {

  private final ToolingCostLineService toolingCostLineService;

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/to-be-estimated/search")
  public ResponseEntity<SWToolingCostLinesPaginated> searchToolingCostLinesToBeEstimated(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @Valid @RequestBody final SWBasicSearch body) {
    SWToolingCostLinesPaginated result =
        toolingCostLineService.searchToolingCostLines(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/to-be-estimated/{uid}/estimate")
  public ResponseEntity<SWToolingCostLine> estimateToolingCostLine(
      @PathVariable final UUID uid, @RequestBody SWToolingCostLineEstimate body) {
    SWToolingCostLine result = toolingCostLineService.estimateToolingCostLine(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/to-be-estimated/{uid}/reject")
  public ResponseEntity<SWToolingCostLine> rejectToolingCostLine(
      @PathVariable final UUID uid, @Valid @RequestBody final SWToolingCostLineReject body)
      throws Exception {
    SWToolingCostLine result = toolingCostLineService.rejectToolingCostLine(uid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/to-be-estimated/{uid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveToolingCostLineFilesMetadata(
      @PathVariable UUID uid) {
    List<SWFileInfo> filesMetadata =
        toolingCostLineService.retrieveToolingCostLineFilesMetadata(uid);
    return new ResponseEntity<>(filesMetadata, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/to-be-estimated/{uid}/messages")
  public ResponseEntity<List<SWMessage>> retrieveToolingMessages(@PathVariable UUID uid) {
    List<SWMessage> messages = toolingCostLineService.retrieveMessages(uid);
    return new ResponseEntity<>(messages, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/to-be-estimated/{uid}/messages")
  public ResponseEntity<List<SWMessage>> createToolingMessage(
      @PathVariable UUID uid, @Valid @RequestBody SWMessageCreate body) {
    List<SWMessage> messages = toolingCostLineService.createMessage(uid, body);
    return new ResponseEntity<>(messages, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PutMapping("/to-be-estimated/{uid}/messages/{messageUid}")
  public ResponseEntity<SWMessage> updateToolingMessage(
      @PathVariable UUID uid,
      @PathVariable UUID messageUid,
      @Valid @RequestBody SWMessageUpdate body) {
    SWMessage result = toolingCostLineService.updateMessage(uid, messageUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/to-be-estimated/{uid}/messages/{messageUid}/delete")
  public ResponseEntity<SWMessage> deleteToolingMessage(
      @PathVariable UUID uid, @PathVariable UUID messageUid) {
    SWMessage result = toolingCostLineService.deleteMessage(uid, messageUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/to-be-estimated/{uid}/messages/{messageUid}/undo")
  public ResponseEntity<SWMessage> undoToolingMessage(
      @PathVariable UUID uid, @PathVariable UUID messageUid) {
    SWMessage result = toolingCostLineService.undoMessage(uid, messageUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/to-be-estimated/{uid}/files/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadToolingCostLineFiles(
      @PathVariable UUID uid, @Valid @RequestBody List<UUID> fileUids) throws Exception {
    org.springframework.core.io.Resource resource =
        toolingCostLineService.downloadToolingCostLineFiles(uid, fileUids);
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
}
