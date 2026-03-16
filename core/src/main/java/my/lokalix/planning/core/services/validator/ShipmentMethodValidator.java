package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.ShipmentMethodEntity;
import my.lokalix.planning.core.repositories.admin.ShipmentMethodRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShipmentMethodValidator {

  private final ShipmentMethodRepository shipmentMethodRepository;

  public void validateNotInUse(ShipmentMethodEntity shipmentMethod) {
    if (shipmentMethodRepository.isUsedByNonArchivedSupplier(shipmentMethod)) {
      throw new GenericWithMessageException(
          "Cannot archive shipment method: it is used by one or more suppliers",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
