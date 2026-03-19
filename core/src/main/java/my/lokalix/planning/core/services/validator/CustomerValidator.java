package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.CustomerEntity;
import my.lokalix.planning.core.repositories.CustomerRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerValidator {

  private final CustomerRepository customerRepository;

  public void validateNotInUse(CustomerEntity customer) {
    if (customerRepository.isUsedByNonArchivedCostRequest(customer)) {
      throw new GenericWithMessageException(
          "Cannot archive customer: it is used by one or more npi orders",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
