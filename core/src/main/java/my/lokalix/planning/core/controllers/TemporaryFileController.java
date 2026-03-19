package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.services.TemporaryFileService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("temporary-files")
public class TemporaryFileController {

  private final TemporaryFileService temporaryFileService;

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/upload")
  public ResponseEntity<List<SWFileInfo>> uploadTemporaryFiles(
      @RequestParam("files") MultipartFile[] files) throws Exception {
    return new ResponseEntity<>(temporaryFileService.uploadTemporaryFile(files), HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/delete")
  public ResponseEntity<String> deleteTemporaryFiles(@Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    temporaryFileService.deleteTemporaryFile(fileUids);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Secured({
    UserRole.SecurityConstants.ENGINEERING,
    UserRole.SecurityConstants.PROCUREMENT,
    UserRole.SecurityConstants.ADMINISTRATOR,
    UserRole.SecurityConstants.SUPER_ADMINISTRATOR
  })
  @PostMapping("/download")
  public ResponseEntity<Resource> downloadTemporaryFiles(@Valid @RequestBody List<UUID> fileUids)
      throws Exception {
    Resource resource = temporaryFileService.downloadTemporaryFiles(fileUids);
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
