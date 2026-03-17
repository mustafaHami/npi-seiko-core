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
    }
  }
}
