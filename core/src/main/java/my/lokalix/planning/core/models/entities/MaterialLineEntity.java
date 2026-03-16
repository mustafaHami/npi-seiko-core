package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.enums.CostingMethodType;
import my.lokalix.planning.core.utils.TimeUtils;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@Entity
@Table(name = "material_line")
@EqualsAndHashCode(of = "materialLineId")
public class MaterialLineEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID materialLineId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_line_id", nullable = false)
  private CostRequestLineEntity costRequestLine;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_id", nullable = false)
  private MaterialEntity material;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal quantity;

  @OneToMany(mappedBy = "materialLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<MaterialLinePerCostRequestQuantityEntity> materialLineForCostRequestQuantities =
      new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "chosen_material_supplier_id")
  private MaterialSupplierEntity chosenMaterialSupplier;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  // *** Calculated
  @Column(precision = 25, scale = 6)
  private BigDecimal minimumOrderQuantity;

  @Column(precision = 25, scale = 6)
  private BigDecimal unitPurchasingPriceInPurchasingCurrency;

  @Column(length = 500)
  private String leadTime;

  @Column(precision = 25, scale = 6)
  private BigDecimal standardPackagingQuantity;

  // The all values based on materialSubstitute is hasSubstituteMaterial = true
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

  @Column(nullable = false, name = "is_substitute_material")
  private boolean isSubstituteMaterial = false;

  @Column(nullable = false, name = "has_material_substitute")
  private boolean hasMaterialSubstitute = false;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "material_substitute_id")
  private MaterialLineEntity materialSubstitute;

  @Column(nullable = false)
  private boolean markedNotUsedForQuote = false;

  public void addMaterialLinePerCostRequestQuantity(
      MaterialLinePerCostRequestQuantityEntity entity) {
    entity.setIndexId(materialLineForCostRequestQuantities.size());
    entity.setMaterialLine(this);
    materialLineForCostRequestQuantities.add(entity);
  }

  public void clearMaterialLinePerCostRequestQuantities() {
    this.materialLineForCostRequestQuantities.clear();
  }

  public MaterialLinePerCostRequestQuantityEntity
      retrieveMatchingMaterialLinePerCostRequestQuantities(int qty) {
    if (materialLineForCostRequestQuantities.isEmpty()) {
      return null;
    }
    return materialLineForCostRequestQuantities.stream()
        .filter(mlpcq -> mlpcq.getCostRequestQuantity() == qty)
        .findFirst()
        .orElse(null);
  }

  // ***

  public void buildCalculatedFields(
      CostingMethodType costingMethodType, String systemCurrencyCode) {
    buildCalculatedFieldByMaterialLine(this, costingMethodType, systemCurrencyCode);
    if (hasMaterialSubstitute) {
      buildCalculatedFieldByMaterialLine(materialSubstitute, costingMethodType, systemCurrencyCode);
    }
  }

  private void buildCalculatedFieldByMaterialLine(
      MaterialLineEntity materialLine,
      CostingMethodType costingMethodType,
      String systemCurrencyCode) {
    if (materialLine == null) {
      return;
    }
    if (materialLine.getChosenMaterialSupplier() == null) {
      if (materialLine.getMaterial().getSuppliers().isEmpty()) {
        return;
      }
      Optional<MaterialSupplierEntity> optMaterialSupplier =
          materialLine.getMaterial().getSuppliers().stream()
              .filter(MaterialSupplierEntity::isDefaultSupplier)
              .findFirst();
      if (optMaterialSupplier.isEmpty()) {
        return;
      }
      materialLine.setChosenMaterialSupplier(optMaterialSupplier.get());
    }
    MaterialSupplierMoqLineEntity chosenMoqLine =
        selectOptimalMoqLine(
            materialLine.getChosenMaterialSupplier().getMoqLines(),
            materialLine.getQuantity(),
            costingMethodType);
    if (chosenMoqLine == null) {
      return;
    }
    materialLine.setMinimumOrderQuantity(chosenMoqLine.getMinimumOrderQuantity());
    materialLine.setUnitPurchasingPriceInPurchasingCurrency(
        chosenMoqLine.getUnitPurchasingPriceInPurchasingCurrency());
    materialLine.setLeadTime(chosenMoqLine.getLeadTime());
    materialLine.setStandardPackagingQuantity(chosenMoqLine.getStandardPackagingQuantity());

    CurrencyEntity purchasingCurrency = chosenMoqLine.getMaterialSupplier().getPurchasingCurrency();
    materialLine.setPurchasingCurrencyExchangeRateToSystemCurrency(
        purchasingCurrency.findExchangeRate(systemCurrencyCode));

    materialLine.setUnitPurchasingPriceInSystemCurrency(
        materialLine
            .getUnitPurchasingPriceInPurchasingCurrency()
            .multiply(materialLine.getPurchasingCurrencyExchangeRateToSystemCurrency()));
    BigDecimal totalPurchasePrice =
        materialLine.getQuantity().multiply(materialLine.getUnitPurchasingPriceInSystemCurrency());
    materialLine.setTotalPurchasingPriceInSystemCurrency(totalPurchasePrice);
    materialLine.setShipmentPercentage(
        materialLine.getChosenMaterialSupplier().getSupplier().getShipmentMethod() != null
            ? materialLine
                .getChosenMaterialSupplier()
                .getSupplier()
                .getShipmentMethod()
                .getPercentage()
            : BigDecimal.ZERO);
    materialLine.setTotalShipmentCostInSystemCurrency(
        materialLine
            .getTotalPurchasingPriceInSystemCurrency()
            .multiply(
                materialLine
                    .getShipmentPercentage()
                    .divide(new BigDecimal(100), 6, RoundingMode.HALF_UP)));
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
