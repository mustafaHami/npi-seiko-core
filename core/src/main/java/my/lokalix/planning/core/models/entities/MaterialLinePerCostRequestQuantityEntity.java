package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.enums.CostingMethodType;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@Entity
@Table(name = "material_line_per_cost_request_quantity")
@EqualsAndHashCode(of = "materialLinePerCostRequestQuantityId")
public class MaterialLinePerCostRequestQuantityEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID materialLinePerCostRequestQuantityId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_line_id", nullable = false)
  private MaterialLineEntity materialLine;

  @Column(nullable = false)
  private int costRequestQuantity;

  @Column(nullable = false)
  private @NotNull BigDecimal quantity;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "chosen_material_supplier_id")
  private MaterialSupplierEntity chosenMaterialSupplier;

  // *** Calculated
  @Column(precision = 25, scale = 6)
  private BigDecimal minimumOrderQuantity;

  @Column(precision = 25, scale = 6)
  private BigDecimal unitPurchasingPriceInPurchasingCurrency;

  @Column(length = 500)
  private String leadTime;

  @Column(precision = 25, scale = 6)
  private BigDecimal standardPackagingQuantity;

  @Column(precision = 25, scale = 6)
  private BigDecimal purchasingCurrencyExchangeRateToSystemCurrency;

  @Column(precision = 25, scale = 6)
  private BigDecimal unitPurchasingPriceInSystemCurrency;

  @Column(precision = 25, scale = 6)
  private BigDecimal totalPurchasingPriceInSystemCurrency;

  @Column(precision = 25, scale = 6)
  private BigDecimal shipmentPercentage;

  @Column(precision = 25, scale = 6)
  private BigDecimal totalShipmentCostInSystemCurrency;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  // ***

  public void buildCalculatedFields(
      CostingMethodType costingMethodType, String systemCurrencyCode) {
    quantity = materialLine.getQuantity().multiply(BigDecimal.valueOf(costRequestQuantity));
    MaterialSupplierMoqLineEntity chosenMoqLine =
        selectOptimalMoqLine(chosenMaterialSupplier.getMoqLines(), quantity, costingMethodType);
    if (chosenMoqLine == null) {
      return;
    }
    this.minimumOrderQuantity = chosenMoqLine.getMinimumOrderQuantity();
    this.unitPurchasingPriceInPurchasingCurrency =
        chosenMoqLine.getUnitPurchasingPriceInPurchasingCurrency();
    this.leadTime = chosenMoqLine.getLeadTime();
    this.standardPackagingQuantity = chosenMoqLine.getStandardPackagingQuantity();

    CurrencyEntity purchasingCurrency = chosenMoqLine.getMaterialSupplier().getPurchasingCurrency();
    BigDecimal exchangeRateToSystemCurrency =
        purchasingCurrency.findExchangeRate(systemCurrencyCode);
    setPurchasingCurrencyExchangeRateToSystemCurrency(exchangeRateToSystemCurrency);

    setUnitPurchasingPriceInSystemCurrency(
        unitPurchasingPriceInPurchasingCurrency.multiply(exchangeRateToSystemCurrency));
    BigDecimal totalPurchasePrice = quantity.multiply(unitPurchasingPriceInSystemCurrency);
    setTotalPurchasingPriceInSystemCurrency(totalPurchasePrice);
    shipmentPercentage =
        this.chosenMaterialSupplier.getSupplier().getShipmentMethod() != null
            ? this.chosenMaterialSupplier.getSupplier().getShipmentMethod().getPercentage()
            : BigDecimal.ZERO;
    totalShipmentCostInSystemCurrency =
        totalPurchasingPriceInSystemCurrency.multiply(
            shipmentPercentage.divide(new BigDecimal(100), 6, RoundingMode.HALF_UP));
  }

  private MaterialSupplierMoqLineEntity selectOptimalMoqLine(
      List<MaterialSupplierMoqLineEntity> moqLines,
      BigDecimal targetQuantity,
      CostingMethodType costingMethodType) {
    if (CollectionUtils.isEmpty(moqLines)) {
      return null;
    }
    // for NPI, always take the smallest MOQ
    if (costingMethodType == CostingMethodType.NPI) {
      return moqLines.stream()
          .min(Comparator.comparing(MaterialSupplierMoqLineEntity::getMinimumOrderQuantity))
          .orElse(null);
    }

    MaterialSupplierMoqLineEntity closest = null;
    BigDecimal minDiff = null;

    for (MaterialSupplierMoqLineEntity moqLine : moqLines) {
      BigDecimal moq = moqLine.getMinimumOrderQuantity();
      if (moq == null) {
        continue;
      }

      // Computes the absolute difference (moq - targetQuantity).abs() for each MOQ line
      BigDecimal diff = moq.subtract(targetQuantity).abs();
      if (minDiff == null
          || diff.compareTo(minDiff) < 0 // Picks the one with the smallest diff
          || (diff.compareTo(minDiff) == 0
              && moq.compareTo(closest.getMinimumOrderQuantity())
                  > 0)) // On equal diff, picks the one with the higher MOQ quantity
      {
        minDiff = diff;
        closest = moqLine;
      }
    }
    return closest;
  }
}
