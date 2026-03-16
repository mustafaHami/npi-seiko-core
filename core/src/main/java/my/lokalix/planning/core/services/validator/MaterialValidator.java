package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.repositories.MaterialRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialValidator {

  private final MaterialRepository materialRepository;

  public void validateNotInUse(MaterialEntity material) {
    if (materialRepository.isUsedByNonArchivedMaterialLine(material)) {
      throw new GenericWithMessageException(
          "Cannot archive material: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void checkDuplicateMaterial(
      MaterialEntity materialLineEntity, MaterialEntity materialSubstitute) {
    if (materialLineEntity.getManufacturer() != null
        && materialLineEntity.getManufacturerPartNumber() != null) {
      // No need to verify if Manufacturer and P/N is not null
      // Because for substitute we get always estimated material
      if (materialSubstitute.getManufacturer().equals(materialLineEntity.getManufacturer())
          && materialSubstitute
              .getManufacturerPartNumber()
              .equals(materialLineEntity.getManufacturerPartNumber())) {
        throw new GenericWithMessageException(
            "The sames material is already added in material lines. Please choose another material",
            SWCustomErrorCode.GENERIC_ERROR);
      }
    }
  }
}
