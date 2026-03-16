package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.lokalix.planning.core.repositories.admin.ShipmentLocationRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShipmentLocationValidator {

  private final ShipmentLocationRepository shipmentLocationRepository;

  public void validateNotInUse(ShipmentLocationEntity entity) {
    if (shipmentLocationRepository.isUsedByNonArchivedOtherCostLine(entity)) {
      throw new GenericWithMessageException(
          "Cannot archive location: it is used in one or more customer request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (shipmentLocationRepository.isUsedByNonArchivedCustomer(entity)) {
      throw new GenericWithMessageException(
          "Cannot archive location: it is used by one or more customers",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
