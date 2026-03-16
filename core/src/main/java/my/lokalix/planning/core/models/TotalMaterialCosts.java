package my.lokalix.planning.core.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Getter
@Setter
public class TotalMaterialCosts {
  BigDecimal materialCostWithoutShippingCostInSystemCurrency;
  BigDecimal materialCostWithoutShippingCostWithYieldInSystemCurrency;
  BigDecimal materialShippingCostInSystemCurrency;

  BigDecimal materialCostWithoutShippingCostInTargetCurrency;
  BigDecimal materialCostWithoutShippingCostWithYieldInTargetCurrency;
  BigDecimal materialShippingCostInTargetCurrency;
}
