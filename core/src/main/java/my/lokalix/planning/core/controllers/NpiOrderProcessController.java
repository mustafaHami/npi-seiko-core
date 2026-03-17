package my.lokalix.planning.core.controllers;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.services.NpiOrderService;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("npi-orders/{uid}/process")
public class NpiOrderProcessController {

  private final NpiOrderService npiOrderService;

  @GetMapping
  public ResponseEntity<SWProcess> retrieveNpiOrderProcess(@PathVariable final UUID uid) {
    SWProcess result = npiOrderService.retrieveNpiOrderProcess(uid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/lines/{lineUid}/status")
  public ResponseEntity<List<SWProcessLine>> updateNpiOrderProcessLineStatus(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWProcessLineStatusUpdateBody body) {
    List<SWProcessLine> result =
        npiOrderService.updateNpiOrderProcessLineStatus(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @GetMapping("/lines/{lineUid}/statuses")
  public ResponseEntity<List<SWProcessLineStatusHistory>> retrieveNpiOrderProcessLineStatusesHistory(
      @PathVariable final UUID uid, @PathVariable final UUID lineUid) {
    List<SWProcessLineStatusHistory> result =
        npiOrderService.retrieveNpiOrderProcessLineStatusesHistory(uid, lineUid);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PutMapping("/lines/{lineUid}/remaining-time")
  public ResponseEntity<SWProcessLine> updateProcessLineRemainingTime(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWProcessLineRemainingTimeUpdate body) {
    SWProcessLine result = npiOrderService.updateProcessLineRemainingTime(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/lines/{lineUid}/material-delivery-date/import")
  public ResponseEntity<LocalDate> importMaterialLatestDeliveryDate(
      @PathVariable final UUID uid,
      @PathVariable final UUID lineUid,
      @Valid @RequestBody SWProcessLineMaterialDeliveryDateImport body) {
    LocalDate result = npiOrderService.importMaterialLatestDeliveryDate(uid, lineUid, body);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
