package my.lokalix.planning.core.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Getter
@Setter
public class TotalOtherCosts {
  BigDecimal totalOtherCostInSystemCurrency;
  BigDecimal totalOtherCostInTargetCurrency;
}
