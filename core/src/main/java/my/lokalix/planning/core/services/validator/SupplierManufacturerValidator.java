package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.repositories.admin.SupplierAndManufacturerRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplierManufacturerValidator {

  private final SupplierAndManufacturerRepository supplierAndManufacturerRepository;

  public void validateNotInUse(SupplierManufacturerEntity supplierManufacturer) {
    if (supplierAndManufacturerRepository.isUsedByNonArchivedMaterial(supplierManufacturer)) {
      throw new GenericWithMessageException(
          "Cannot archive supplier/manufacturer: it is used by one or more materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (supplierAndManufacturerRepository.isSupplierUsedByNonArchivedMaterial(
        supplierManufacturer)) {
      throw new GenericWithMessageException(
          "Cannot archive supplier/manufacturer: it is used by one or more materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (supplierAndManufacturerRepository.isUsedByNonArchivedDraftMaterialLine(
        supplierManufacturer)) {
      throw new GenericWithMessageException(
          "Cannot archive supplier/manufacturer: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateSupplierNotInUse(SupplierManufacturerEntity supplierManufacturer) {
    if (supplierAndManufacturerRepository.isSupplierUsedByNonArchivedMaterial(
        supplierManufacturer)) {
      throw new GenericWithMessageException(
          "Cannot remove the SUPPLIER type: it is used by one or more materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateManufacturerNotInUse(SupplierManufacturerEntity supplierManufacturer) {
    if (supplierAndManufacturerRepository.isUsedByNonArchivedMaterial(supplierManufacturer)) {
      throw new GenericWithMessageException(
          "Cannot remove the MANUFACTURER type: it is used by one or more materials",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (supplierAndManufacturerRepository.isUsedByNonArchivedDraftMaterialLine(
        supplierManufacturer)) {
      throw new GenericWithMessageException(
          "Cannot remove the MANUFACTURER type: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
