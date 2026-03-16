package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.ProcessEntity;
import my.lokalix.planning.core.repositories.admin.ProcessRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessValidator {

  private final ProcessRepository processRepository;

  public void validateNotInUse(ProcessEntity process) {
    if (processRepository.isUsedByNonArchivedProcessLine(process)) {
      throw new GenericWithMessageException(
          "Cannot archive process: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
