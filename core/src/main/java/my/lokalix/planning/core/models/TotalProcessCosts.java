package my.lokalix.planning.core.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Getter
@Setter
public class TotalProcessCosts {
  BigDecimal totalNonSetupProcessCostInSystemCurrency;
  BigDecimal totalSetupProcessCostInSystemCurrency;
  BigDecimal totalProcessCostInSystemCurrency;
  BigDecimal totalProcessCostWithAdditionalInSystemCurrency;

  BigDecimal totalNonSetupProcessCostInTargetCurrency;
  BigDecimal totalSetupProcessCostInTargetCurrency;
  BigDecimal totalProcessCostInTargetCurrency;
  BigDecimal totalProcessCostWithAdditionalInTargetCurrency;
}
