package my.lokalix.planning.core.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Value
@Getter
@Setter
public class TotalCosts {
  TotalMaterialCosts totalMaterialCosts;
  TotalProcessCosts totalProcessCosts;
  TotalOtherCosts totalOtherCosts;
  TotalToolingCosts totalToolingCosts;

  BigDecimal totalCostWithoutToolingInTargetCurrency;
  BigDecimal totalCostWithoutToolingWithMarkupInTargetCurrency;

  BigDecimal totalCostWithToolingInTargetCurrency;
  BigDecimal totalCostWithToolingWithMarkupInTargetCurrency;

  BigDecimal totalToolingCostInTargetCurrency;
  BigDecimal totalToolingCostWithMarkupInTargetCurrency;
}
