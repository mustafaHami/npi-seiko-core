package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWProcessLineStatusUpdateBody;
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

  public void validateStatusUpdate(ProcessLineEntity line, SWProcessLineStatusUpdateBody body) {

    ProcessLineStatus newStatus = ProcessLineStatus.fromValue(body.getStatus().getValue());

    if (newStatus == ProcessLineStatus.IN_PROGRESS) {
      if (line.getIsMaterialPurchase() && body.getMaterialLatestDeliveryDate() == null) {
        throw new GenericWithMessageException(
            "Latest delivery date is required to start this step", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (line.getIsCustomerApproval() && body.getStartingCustomerApprovalDate() == null) {
        throw new GenericWithMessageException(
            "Starting customer approval date is required to start this step",
            SWCustomErrorCode.GENERIC_ERROR);
      }
    }
    if (newStatus == ProcessLineStatus.COMPLETED
        && line.getStatus().equals(ProcessLineStatus.NOT_STARTED)
        && line.getIsCustomerApproval()) {
      throw new GenericWithMessageException(
          "Customer approval cannot be completed before it is started",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (newStatus == ProcessLineStatus.COMPLETED
        && line.getIsCustomerApproval()
        && body.getApprovalCustomerDate() == null) {
      throw new GenericWithMessageException(
          "Approval customer date is required to complete this step",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if ((line.getIsMaterialPurchase() || line.getIsCustomerApproval())
        && newStatus == ProcessLineStatus.COMPLETED
        && line.getStatus().equals(ProcessLineStatus.NOT_STARTED)) {
      throw new GenericWithMessageException(
          "Cannot complete a step that is not in progress", SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
