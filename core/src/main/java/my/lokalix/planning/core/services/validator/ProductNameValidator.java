package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.models.entities.admin.ProductNameEntity;
import my.lokalix.planning.core.repositories.admin.ProductNameRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductNameValidator {

  private final ProductNameRepository productNameRepository;

  public void validateNotInUse(ProductNameEntity productName) {
    if (productNameRepository.isUsedByNonArchivedCostRequestLine(productName)) {
      throw new GenericWithMessageException(
          "Cannot archive product name: it is used by one or more request for quotation lines",
          SWCustomErrorCode.GENERIC_ERROR);
    }
  }
}
