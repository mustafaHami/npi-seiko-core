package my.lokalix.planning.core.services.validator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.utils.NumberUtils;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierMoqLineCreate;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialSupplierValidator {

  public void validateMoqLines(List<SWMaterialSupplierMoqLineCreate> moqLines) {
    if (CollectionUtils.isEmpty(moqLines)) {
      throw new GenericWithMessageException(
          "At least one MOQ line must be provided", SWCustomErrorCode.GENERIC_ERROR);
    }
    for (SWMaterialSupplierMoqLineCreate moqLine : moqLines) {
      if (NumberUtils.isNullOrNotStrictlyPositive(moqLine.getMinimumOrderQuantity())) {
        throw new GenericWithMessageException(
            "Minimum order quantity must be greater than 0", SWCustomErrorCode.GENERIC_ERROR);
      }
      if (NumberUtils.isNullOrNotStrictlyPositive(
          moqLine.getUnitPurchasingPriceInPurchasingCurrency())) {
        throw new GenericWithMessageException(
            "Unit price must be greater than 0", SWCustomErrorCode.GENERIC_ERROR);
      }
    }
  }
}
