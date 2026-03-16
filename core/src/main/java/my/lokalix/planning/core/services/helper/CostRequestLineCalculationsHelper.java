package my.lokalix.planning.core.services.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.*;
import my.lokalix.planning.core.models.entities.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostRequestLineCalculationsHelper {
  public TotalMaterialCosts calculateMaterialCosts(
      List<MaterialLineEntity> onlyMaterialLinesUsedForQuotation,
      BigDecimal materialYieldPercentage,
      BigDecimal targetExchangeRate) {
    BigDecimal totalMaterialCostWithoutShippingCostInSystemCurrency = BigDecimal.ZERO;
    BigDecimal totalMaterialShippingCostInSystemCurrency = BigDecimal.ZERO;
    for (MaterialLineEntity m : onlyMaterialLinesUsedForQuotation) {
      boolean hasSubstitute = m.isHasMaterialSubstitute();
      BigDecimal purchasingPrice =
          hasSubstitute
              ? m.getMaterialSubstitute().getTotalPurchasingPriceInSystemCurrency()
              : m.getTotalPurchasingPriceInSystemCurrency();
      BigDecimal shipmentCost =
          hasSubstitute
              ? m.getMaterialSubstitute().getTotalShipmentCostInSystemCurrency()
              : m.getTotalShipmentCostInSystemCurrency();
      if (purchasingPrice != null) {
        totalMaterialCostWithoutShippingCostInSystemCurrency =
            totalMaterialCostWithoutShippingCostInSystemCurrency.add(purchasingPrice);
      }
      if (shipmentCost != null) {
        totalMaterialShippingCostInSystemCurrency =
            totalMaterialShippingCostInSystemCurrency.add(shipmentCost);
      }
    }

    BigDecimal yieldMultiplier =
        BigDecimal.ONE.add(
            materialYieldPercentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    BigDecimal totalMaterialCostWithoutShippingCostWithYieldInSystemCurrency =
        totalMaterialCostWithoutShippingCostInSystemCurrency.multiply(yieldMultiplier);
    return new TotalMaterialCosts(
        totalMaterialCostWithoutShippingCostInSystemCurrency,
        totalMaterialCostWithoutShippingCostWithYieldInSystemCurrency,
        totalMaterialShippingCostInSystemCurrency,
        totalMaterialCostWithoutShippingCostInSystemCurrency.multiply(targetExchangeRate),
        totalMaterialCostWithoutShippingCostWithYieldInSystemCurrency.multiply(targetExchangeRate),
        totalMaterialShippingCostInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalMaterialCosts calculateMaterialCostsPerQuantity(
      List<MaterialLinePerCostRequestQuantityEntity> perQuantity,
      BigDecimal materialYieldPercentage,
      BigDecimal targetExchangeRate) {
    BigDecimal totalMaterialCostWithoutShippingCostInSystemCurrency = BigDecimal.ZERO;
    BigDecimal totalMaterialShippingCostInSystemCurrency = BigDecimal.ZERO;
    for (MaterialLinePerCostRequestQuantityEntity m : perQuantity) {
      BigDecimal purchasingPrice = m.getTotalPurchasingPriceInSystemCurrency();
      BigDecimal shipmentCost = m.getTotalShipmentCostInSystemCurrency();
      if (purchasingPrice != null) {
        totalMaterialCostWithoutShippingCostInSystemCurrency =
            totalMaterialCostWithoutShippingCostInSystemCurrency.add(purchasingPrice);
      }
      if (shipmentCost != null) {
        totalMaterialShippingCostInSystemCurrency =
            totalMaterialShippingCostInSystemCurrency.add(shipmentCost);
      }
    }

    BigDecimal yieldMultiplier =
        BigDecimal.ONE.add(
            materialYieldPercentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    BigDecimal totalMaterialCostWithoutShippingCostWithYieldInSystemCurrency =
        totalMaterialCostWithoutShippingCostInSystemCurrency.multiply(yieldMultiplier);
    return new TotalMaterialCosts(
        totalMaterialCostWithoutShippingCostInSystemCurrency,
        totalMaterialCostWithoutShippingCostWithYieldInSystemCurrency,
        totalMaterialShippingCostInSystemCurrency,
        totalMaterialCostWithoutShippingCostInSystemCurrency.multiply(targetExchangeRate),
        totalMaterialCostWithoutShippingCostWithYieldInSystemCurrency.multiply(targetExchangeRate),
        totalMaterialShippingCostInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalProcessCosts calculateProcessCosts(
      List<ProcessLineEntity> processLines,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal targetExchangeRate) {
    BigDecimal totalNonSetupProcessCostInSystemCurrency =
        processLines.stream()
            .filter(pl -> !pl.getProcess().isSetupProcess())
            .map(ProcessLineEntity::getTotalCostInSystemCurrency)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSetupProcessCostInSystemCurrency =
        processLines.stream()
            .filter(pl -> pl.getProcess().isSetupProcess())
            .map(ProcessLineEntity::getTotalCostInSystemCurrency)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalProcessCostInSystemCurrency =
        totalNonSetupProcessCostInSystemCurrency.add(totalSetupProcessCostInSystemCurrency);
    BigDecimal additionalProcessRatePerMethodTypeMultiplier =
        BigDecimal.ONE.add(
            additionalProcessRatePerMethodType.divide(
                BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    BigDecimal totalProcessCostWithAdditionalInSystemCurrency =
        totalProcessCostInSystemCurrency.multiply(additionalProcessRatePerMethodTypeMultiplier);

    return new TotalProcessCosts(
        totalNonSetupProcessCostInSystemCurrency,
        totalSetupProcessCostInSystemCurrency,
        totalProcessCostInSystemCurrency,
        totalProcessCostWithAdditionalInSystemCurrency,
        totalNonSetupProcessCostInSystemCurrency.multiply(targetExchangeRate),
        totalSetupProcessCostInSystemCurrency.multiply(targetExchangeRate),
        totalProcessCostInSystemCurrency.multiply(targetExchangeRate),
        totalProcessCostWithAdditionalInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalProcessCosts calculateProcessCostsPerQuantity(
      List<ProcessLinePerCostRequestQuantityEntity> perQuantity,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal targetExchangeRate) {
    BigDecimal totalNonSetupProcessCostInSystemCurrency =
        perQuantity.stream()
            .filter(pl -> !pl.getProcessLine().getProcess().isSetupProcess())
            .map(ProcessLinePerCostRequestQuantityEntity::getTotalCostInSystemCurrency)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSetupProcessCostInSystemCurrency =
        perQuantity.stream()
            .filter(pl -> pl.getProcessLine().getProcess().isSetupProcess())
            .map(ProcessLinePerCostRequestQuantityEntity::getTotalCostInSystemCurrency)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalProcessCostInSystemCurrency =
        totalNonSetupProcessCostInSystemCurrency.add(totalSetupProcessCostInSystemCurrency);
    BigDecimal additionalProcessRatePerMethodTypeMultiplier =
        BigDecimal.ONE.add(
            additionalProcessRatePerMethodType.divide(
                BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    BigDecimal totalProcessCostWithAdditionalInSystemCurrency =
        totalProcessCostInSystemCurrency.multiply(additionalProcessRatePerMethodTypeMultiplier);

    return new TotalProcessCosts(
        totalNonSetupProcessCostInSystemCurrency,
        totalSetupProcessCostInSystemCurrency,
        totalProcessCostInSystemCurrency,
        totalProcessCostWithAdditionalInSystemCurrency,
        totalNonSetupProcessCostInSystemCurrency.multiply(targetExchangeRate),
        totalSetupProcessCostInSystemCurrency.multiply(targetExchangeRate),
        totalProcessCostInSystemCurrency.multiply(targetExchangeRate),
        totalProcessCostWithAdditionalInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalOtherCosts calculateOtherCosts(
      List<OtherCostLineEntity> otherCostLines,
      UUID shipmentLocationOtherCostLineId,
      BigDecimal targetExchangeRate) {
    BigDecimal totalOtherCostInSystemCurrency;
    if (shipmentLocationOtherCostLineId != null) {
      totalOtherCostInSystemCurrency =
          otherCostLines.stream()
              .filter(
                  ocl ->
                      !ocl.isShipmentToCustomerLine()
                          || ocl.getOtherCostLineId().equals(shipmentLocationOtherCostLineId))
              .map(OtherCostLineEntity::getTotalCostInSystemCurrency)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    } else {
      totalOtherCostInSystemCurrency =
          otherCostLines.stream()
              .map(OtherCostLineEntity::getTotalCostInSystemCurrency)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    return new TotalOtherCosts(
        totalOtherCostInSystemCurrency,
        totalOtherCostInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalOtherCosts calculateOtherCostsPerQuantity(
      List<OtherCostLinePerCostRequestQuantityEntity> perQuantity,
      UUID shipmentLocationOtherCostLineId,
      BigDecimal targetExchangeRate) {
    BigDecimal totalOtherCostInSystemCurrency;
    if (shipmentLocationOtherCostLineId != null) {
      totalOtherCostInSystemCurrency =
          perQuantity.stream()
              .filter(
                  ocl ->
                      !ocl.getOtherCostLine().isShipmentToCustomerLine()
                          || ocl.getOtherCostLine()
                              .getOtherCostLineId()
                              .equals(shipmentLocationOtherCostLineId))
              .map(OtherCostLinePerCostRequestQuantityEntity::getTotalCostInSystemCurrency)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    } else {
      totalOtherCostInSystemCurrency =
          perQuantity.stream()
              .map(OtherCostLinePerCostRequestQuantityEntity::getTotalCostInSystemCurrency)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    return new TotalOtherCosts(
        totalOtherCostInSystemCurrency,
        totalOtherCostInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalToolingCosts calculateToolingCosts(
      List<ToolingCostLineEntity> toolingCostLines, BigDecimal targetExchangeRate) {
    BigDecimal totalToolingCostInSystemCurrency =
        toolingCostLines.stream()
            .map(ToolingCostLineEntity::getTotalCostInSystemCurrency)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new TotalToolingCosts(
        totalToolingCostInSystemCurrency,
        totalToolingCostInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalToolingCosts calculateToolingCostsPerQuantity(
      List<ToolingCostLinePerCostRequestQuantityEntity> perQuantity,
      BigDecimal targetExchangeRate) {
    BigDecimal totalToolingCostInSystemCurrency =
        perQuantity.stream()
            .map(ToolingCostLinePerCostRequestQuantityEntity::getTotalCostInSystemCurrency)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new TotalToolingCosts(
        totalToolingCostInSystemCurrency,
        totalToolingCostInSystemCurrency.multiply(targetExchangeRate));
  }

  public TotalCosts calculateTotalCosts(
      List<MaterialLineEntity> materials,
      List<ProcessLineEntity> processes,
      List<OtherCostLineEntity> otherCosts,
      List<ToolingCostLineEntity> toolingCosts,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal materialYieldPercentage,
      UUID shipmentLocationOtherCostLineId,
      BigDecimal markupMultiplier,
      BigDecimal toolingMarkupMultiplier,
      BigDecimal targetExchangeRate) {

    TotalMaterialCosts totalMaterialCosts =
        calculateMaterialCosts(materials, materialYieldPercentage, targetExchangeRate);
    TotalProcessCosts totalProcessCosts =
        calculateProcessCosts(processes, additionalProcessRatePerMethodType, targetExchangeRate);
    TotalOtherCosts totalOtherCosts =
        calculateOtherCosts(otherCosts, shipmentLocationOtherCostLineId, targetExchangeRate);
    TotalToolingCosts totalToolingCosts = calculateToolingCosts(toolingCosts, targetExchangeRate);

    BigDecimal totalCostWithoutToolingInTargetCurrency =
        BigDecimal.ZERO
            .add(totalMaterialCosts.getMaterialCostWithoutShippingCostWithYieldInTargetCurrency())
            .add(totalMaterialCosts.getMaterialShippingCostInTargetCurrency())
            .add(totalProcessCosts.getTotalProcessCostWithAdditionalInTargetCurrency())
            .add(totalOtherCosts.getTotalOtherCostInTargetCurrency());
    BigDecimal totalCostWithToolingInTargetCurrency =
        totalCostWithoutToolingInTargetCurrency.add(
            totalToolingCosts.getTotalToolingCostInTargetCurrency());

    BigDecimal totalCostWithoutToolingWithMarkupInTargetCurrency = null;
    BigDecimal totalCostWithToolingWithMarkupInTargetCurrency = null;
    if (markupMultiplier != null) {
      totalCostWithoutToolingWithMarkupInTargetCurrency =
          totalCostWithoutToolingInTargetCurrency.multiply(markupMultiplier);
      totalCostWithToolingWithMarkupInTargetCurrency =
          totalCostWithToolingInTargetCurrency.multiply(markupMultiplier);
    }

    BigDecimal totalToolingCostWithMarkupInTargetCurrency = null;
    if (toolingMarkupMultiplier != null) {
      totalToolingCostWithMarkupInTargetCurrency =
          totalToolingCosts.getTotalToolingCostInTargetCurrency().multiply(toolingMarkupMultiplier);
    }

    return new TotalCosts(
        totalMaterialCosts,
        totalProcessCosts,
        totalOtherCosts,
        totalToolingCosts,
        totalCostWithoutToolingInTargetCurrency,
        totalCostWithoutToolingWithMarkupInTargetCurrency,
        totalCostWithToolingInTargetCurrency,
        totalCostWithToolingWithMarkupInTargetCurrency,
        totalToolingCosts.getTotalToolingCostInTargetCurrency(),
        totalToolingCostWithMarkupInTargetCurrency);
  }

  public TotalCosts calculateTotalCostsPerQuantity(
      List<MaterialLinePerCostRequestQuantityEntity> materialsPerQuantity,
      List<ProcessLinePerCostRequestQuantityEntity> processesPerQuantity,
      List<OtherCostLinePerCostRequestQuantityEntity> otherCostsPerQuantity,
      List<ToolingCostLinePerCostRequestQuantityEntity> toolingCostsPerQuantity,
      BigDecimal additionalProcessRatePerMethodType,
      BigDecimal materialYieldPercentage,
      UUID shipmentLocationOtherCostLineId,
      BigDecimal markupMultiplier,
      BigDecimal toolingMarkupMultiplier,
      BigDecimal targetExchangeRate) {

    TotalMaterialCosts totalMaterialCosts =
        calculateMaterialCostsPerQuantity(
            materialsPerQuantity, materialYieldPercentage, targetExchangeRate);
    TotalProcessCosts totalProcessCosts =
        calculateProcessCostsPerQuantity(
            processesPerQuantity, additionalProcessRatePerMethodType, targetExchangeRate);
    TotalOtherCosts totalOtherCosts =
        calculateOtherCostsPerQuantity(
            otherCostsPerQuantity, shipmentLocationOtherCostLineId, targetExchangeRate);
    TotalToolingCosts totalToolingCosts =
        calculateToolingCostsPerQuantity(toolingCostsPerQuantity, targetExchangeRate);

    BigDecimal totalCostWithoutToolingInTargetCurrency =
        BigDecimal.ZERO
            .add(totalMaterialCosts.getMaterialCostWithoutShippingCostWithYieldInTargetCurrency())
            .add(totalMaterialCosts.getMaterialShippingCostInTargetCurrency())
            .add(totalProcessCosts.getTotalProcessCostWithAdditionalInTargetCurrency())
            .add(totalOtherCosts.getTotalOtherCostInTargetCurrency());
    BigDecimal totalCostWithToolingInTargetCurrency =
        totalCostWithoutToolingInTargetCurrency.add(
            totalToolingCosts.getTotalToolingCostInTargetCurrency());

    BigDecimal totalCostWithoutToolingWithMarkupInTargetCurrency = null;
    BigDecimal totalCostWithToolingWithMarkupInTargetCurrency = null;
    if (markupMultiplier != null) {
      totalCostWithoutToolingWithMarkupInTargetCurrency =
          totalCostWithoutToolingInTargetCurrency.multiply(markupMultiplier);
      totalCostWithToolingWithMarkupInTargetCurrency =
          totalCostWithToolingInTargetCurrency.multiply(markupMultiplier);
    }

    BigDecimal totalToolingCostWithMarkupInTargetCurrency = null;
    if (toolingMarkupMultiplier != null) {
      totalToolingCostWithMarkupInTargetCurrency =
          totalToolingCosts.getTotalToolingCostInTargetCurrency().multiply(toolingMarkupMultiplier);
    }

    return new TotalCosts(
        totalMaterialCosts,
        totalProcessCosts,
        totalOtherCosts,
        totalToolingCosts,
        totalCostWithoutToolingInTargetCurrency,
        totalCostWithoutToolingWithMarkupInTargetCurrency,
        totalCostWithToolingInTargetCurrency,
        totalCostWithToolingWithMarkupInTargetCurrency,
        totalToolingCosts.getTotalToolingCostInTargetCurrency(),
        totalToolingCostWithMarkupInTargetCurrency);
  }
}
