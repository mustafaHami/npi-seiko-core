package my.lokalix.planning.core.services.validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWProcessLineStatusUpdateBody;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessLineValidator {

  public void validateRemainingTimeUpdate(ProcessLineEntity line) {
    if (line.getStatus() != ProcessLineStatus.IN_PROGRESS) {
      throw new GenericWithMessageException(
          "Remaining time can only be updated when the step is In Progress",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateStatusUpdate(
      ProcessLineEntity line, SWProcessLineStatusUpdateBody body, NpiOrderEntity npiOrder) {

    ProcessLineStatus newStatus = ProcessLineStatus.fromValue(body.getStatus().getValue());

    if (newStatus == ProcessLineStatus.IN_PROGRESS) {
      if (line.getIsMaterialPurchase() && body.getMaterialLatestDeliveryDate() == null) {
        throw new GenericWithMessageException(
            "Latest delivery date is required to start this step", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (line.getIsCustomerApproval()) {
        if (body.getStartingCustomerApprovalDate() == null) {
          throw new GenericWithMessageException(
              "Starting customer approval date is required to start this step",
              SWCustomErrorCode.GENERIC_ERROR);
        }
        LocalDate shippingDate = findShippingDate(npiOrder);
        if (shippingDate != null && body.getStartingCustomerApprovalDate().isBefore(shippingDate)) {
          throw new GenericWithMessageException(
              "Starting customer approval date ("
                  + body.getStartingCustomerApprovalDate()
                  + ") cannot be before the shipping date ("
                  + shippingDate
                  + ")",
              SWCustomErrorCode.GENERIC_ERROR);
        }
      }
    } else if (newStatus == ProcessLineStatus.COMPLETED) {
      if ((line.getIsMaterialPurchase() || line.getIsCustomerApproval())
          && line.getStatus().equals(ProcessLineStatus.NOT_STARTED)) {
        throw new GenericWithMessageException(
            "Cannot complete a step that is not in progress", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (line.getIsShipment()) {
        LocalDate materialLatestDeliveryDate = findMaterialLatestDeliveryDate(npiOrder);
        if (materialLatestDeliveryDate != null
            && body.getShippingDate() != null
            && body.getShippingDate().isBefore(materialLatestDeliveryDate)) {
          throw new GenericWithMessageException(
              "Shipping date ("
                  + body.getShippingDate()
                  + ") cannot be before the latest material delivery date ("
                  + materialLatestDeliveryDate
                  + ")",
              SWCustomErrorCode.GENERIC_ERROR);
        }
      }
      if (line.getIsCustomerApproval()) {
        if (body.getApprovalCustomerDate() == null) {
          throw new GenericWithMessageException(
              "Approval customer date is required to complete this step",
              SWCustomErrorCode.GENERIC_ERROR);
        }
        LocalDate startingCustomerApproval = findStartingCustomerApproval(npiOrder);
        if (startingCustomerApproval != null
            && body.getApprovalCustomerDate().isBefore(startingCustomerApproval)) {
          throw new GenericWithMessageException(
              "Customer approval date ("
                  + body.getApprovalCustomerDate()
                  + ") cannot be before the starting approval date ("
                  + startingCustomerApproval
                  + ")",
              SWCustomErrorCode.GENERIC_ERROR);
        }
      }
      if (line.getIsTesting() && body.getFileUid() == null) {
        throw new GenericWithMessageException(
            "Document is required to complete this step", SWCustomErrorCode.GENERIC_ERROR);
      }
    }
  }

  private LocalDate findMaterialLatestDeliveryDate(NpiOrderEntity npiOrder) {
    List<ProcessLineEntity> lines = npiOrder.getProcessLines();
    if (CollectionUtils.isEmpty(lines)) return null;
    return lines.stream()
        .filter(ProcessLineEntity::getIsMaterialPurchase)
        .map(ProcessLineEntity::getMaterialLatestDeliveryDate)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private LocalDate findShippingDate(NpiOrderEntity npiOrder) {
    List<ProcessLineEntity> lines = npiOrder.getProcessLines();
    if (CollectionUtils.isEmpty(lines)) return null;
    return lines.stream()
        .filter(ProcessLineEntity::getIsShipment)
        .map(ProcessLineEntity::getShippingDate)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private LocalDate findStartingCustomerApproval(NpiOrderEntity npiOrder) {
    List<ProcessLineEntity> lines = npiOrder.getProcessLines();
    if (CollectionUtils.isEmpty(lines)) return null;
    return lines.stream()
        .filter(ProcessLineEntity::getIsCustomerApproval)
        .map(ProcessLineEntity::getStartingCustomerApprovalDate)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }
}
