package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.repositories.CurrencyRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyValidator {

  private final CurrencyRepository currencyRepository;

  public void validateNotInUse(CurrencyEntity currency) {
    if (currencyRepository.isUsedByNonArchivedCostRequest(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more request for quotations",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (currencyRepository.isUsedByNonArchivedProcess(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more processes",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (currencyRepository.isUsedByNonArchivedToolingCostLine(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more tooling cost lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (currencyRepository.isUsedByNonArchivedOtherCostLine(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more other cost lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (currencyRepository.isUsedByNonArchivedMaterialSupplier(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more material suppliers",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (currencyRepository.isUsedByNonArchivedShipmentLocation(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more shipment locations",
          SWCustomErrorCode.GENERIC_ERROR);
    }
    if (currencyRepository.isUsedByNonArchivedCustomerShipmentLocation(currency)) {
      throw new GenericWithMessageException(
          "Cannot archive currency: it is used by one or more customers",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
