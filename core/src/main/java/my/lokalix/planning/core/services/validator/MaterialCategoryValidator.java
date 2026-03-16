package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.lokalix.planning.core.repositories.admin.MaterialCategoryRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialCategoryValidator {

  private final MaterialCategoryRepository materialCategoryRepository;

  public void validateNotInUse(MaterialCategoryEntity category) {
    if (materialCategoryRepository.isUsedByNonArchivedMaterial(category)) {
      throw new GenericWithMessageException(
          "Cannot archive material category: it is used by one or more materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (materialCategoryRepository.isUsedByNonArchivedDraftMaterialLine(category)) {
      throw new GenericWithMessageException(
          "Cannot archive material category: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
