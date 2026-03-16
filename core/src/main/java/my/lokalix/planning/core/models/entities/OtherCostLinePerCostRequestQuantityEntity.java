package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "other_cost_line_per_cost_request_quantity")
@EqualsAndHashCode(of = "otherCostLinePerCostRequestQuantityId")
public class OtherCostLinePerCostRequestQuantityEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID otherCostLinePerCostRequestQuantityId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "other_cost_line_id", nullable = false)
  private OtherCostLineEntity otherCostLine;

  @Column(nullable = false)
  private int costRequestQuantity;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal unitCostInCurrency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal totalCostInSystemCurrency;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  public void buildCalculatedFields(String systemCurrencyCode) {
    BigDecimal exchangeRate = otherCostLine.getCurrency().findExchangeRate(systemCurrencyCode);
    unitCostInCurrency = otherCostLine.getUnitCostInCurrency();
    switch (otherCostLine.getCalculationStrategy()) {
      case AS_IS -> {
        totalCostInSystemCurrency = unitCostInCurrency.multiply(exchangeRate);
      }
      case MULTIPLIED_BY_QUANTITY -> {
        totalCostInSystemCurrency =
            unitCostInCurrency
                .multiply(BigDecimal.valueOf(costRequestQuantity))
                .multiply(exchangeRate);
      }
      case DIVIDED_BY_QUANTITY -> {
        totalCostInSystemCurrency =
            unitCostInCurrency
                .divide(BigDecimal.valueOf(costRequestQuantity), 6, RoundingMode.HALF_UP)
                .multiply(exchangeRate);
      }
      default ->
          throw new IllegalStateException(
              "Unexpected value: " + otherCostLine.getCalculationStrategy());
    }
  }
}
