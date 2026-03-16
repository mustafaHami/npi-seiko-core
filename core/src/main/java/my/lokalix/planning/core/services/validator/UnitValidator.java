package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import my.lokalix.planning.core.repositories.admin.UnitRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnitValidator {

  private final UnitRepository unitRepository;

  public void validateNotInUse(UnitEntity unit) {
    if (unitRepository.isUsedByNonArchivedMaterial(unit)) {
      throw new GenericWithMessageException(
          "Cannot archive unit: it is used by one or more materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (unitRepository.isUsedByNonArchivedDraftMaterialLine(unit)) {
      throw new GenericWithMessageException(
          "Cannot archive unit: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
