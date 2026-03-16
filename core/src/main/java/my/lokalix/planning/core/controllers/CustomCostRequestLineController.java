package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.CostRequestLineService;
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
@RequestMapping("cost-request-lines")
public class CustomCostRequestLineController {

  private final CostRequestLineService costRequestLineService;

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/to-be-estimated/search")
  public ResponseEntity<SWCustomCostRequestLinesPaginated> searchCostRequestLinesToBeEstimated(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @Valid @RequestBody final SWBasicSearch body) {
    SWCustomCostRequestLinesPaginated result =
        costRequestLineService.searchCostRequestLinesToBeEstimated(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/to-be-estimated/{uid}/estimate")
  public ResponseEntity<Void> estimateCostRequestLine(
      @PathVariable final UUID uid, @RequestBody SWCostRequestLineEstimate body) {
    costRequestLineService.estimateOutsourcedCostRequestLine(uid, body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/to-be-estimated/{uid}/reject")
  public ResponseEntity<Void> rejectCostRequestLine(
      @PathVariable final UUID uid, @Valid @RequestBody final SWCostRequestLineReject body)
      throws Exception {
    costRequestLineService.rejectOutsourcedCostRequestLine(uid, body);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @GetMapping("/to-be-estimated/{uid}/files")
  public ResponseEntity<List<SWFileInfo>> retrieveCostRequestLineFilesMetadata(
      @PathVariable UUID uid) {
    List<SWFileInfo> filesMetadata =
        costRequestLineService.retrieveCostRequestLineFilesMetadata(uid);
    return new ResponseEntity<>(filesMetadata, HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR,
  })
  @PostMapping("/to-be-estimated/{uid}/files/download")
  public ResponseEntity<org.springframework.core.io.Resource> downloadCostRequestLineFiles(
      @PathVariable UUID uid, @Valid @RequestBody List<UUID> fileUids) throws Exception {
    org.springframework.core.io.Resource resource =
        costRequestLineService.downloadCostRequestLineFiles(uid, fileUids);
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
    UserRole.SecurityConstants.PLANNING,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/planning/search")
  public ResponseEntity<SWCustomCostRequestLinesPaginated> searchCostRequestLinesForPlanning(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @Valid @RequestBody final SWBasicSearch body) {
    SWCustomCostRequestLinesPaginated result =
        costRequestLineService.searchCostRequestLinesForPlanning(offset, limit, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
