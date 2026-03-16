package my.lokalix.planning.core.services.validator;

import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerShipmentLocationEntity;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.lokalix.planning.core.repositories.admin.CustomerRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerValidator {

  private final CustomerRepository customerRepository;

  public void validateCurrencyAcceptedByShipmentLocation(
      CurrencyEntity currency, ShipmentLocationEntity shipmentLocation) {
    if (!shipmentLocation.getAcceptedCurrencies().contains(currency)) {
      String acceptedCurrencyCodes =
          shipmentLocation.getAcceptedCurrencies().stream()
              .map(CurrencyEntity::getCode)
              .collect(Collectors.joining(", "));
      throw new GenericWithMessageException(
          "Currency not accepted by shipment location '"
              + shipmentLocation.getName()
              + "'. Accepted currencies: "
              + acceptedCurrencyCodes,
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateMaxShipmentLocations(CustomerEntity customer) {
    if (customer.getShipmentLocations().size() >= 3) {
      throw new GenericWithMessageException(
          "A customer cannot have more than 3 shipment locations",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNoDuplicateShipmentLocation(
      CustomerEntity customer,
      ShipmentLocationEntity shipmentLocation,
      CurrencyEntity currency,
      UUID excludedId) {
    boolean duplicate =
        customer.getShipmentLocations().stream()
            .filter(
                csl ->
                    excludedId == null
                        || !csl.getCustomerShipmentLocationId().equals(excludedId))
            .anyMatch(
                csl ->
                    csl.getShipmentLocation().equals(shipmentLocation)
                        && csl.getCurrency().equals(currency));
    if (duplicate) {
      throw new GenericWithMessageException(
          "A shipment location '"
              + shipmentLocation.getName()
              + "' with currency '"
              + currency.getCode()
              + "' already exists for this customer",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }

  public void validateNotInUse(CustomerEntity customer) {
    if (customerRepository.isUsedByNonArchivedCostRequest(customer)) {
      throw new GenericWithMessageException(
          "Cannot archive customer: it is used by one or more request for quotations",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
