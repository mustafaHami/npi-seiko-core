package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tooling_cost_line_per_cost_request_quantity")
@EqualsAndHashCode(of = "toolingCostLinePerCostRequestQuantityId")
public class ToolingCostLinePerCostRequestQuantityEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID toolingCostLinePerCostRequestQuantityId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tooling_cost_line_id", nullable = false)
  private ToolingCostLineEntity toolingCostLine;

  @Column(nullable = false)
  private int costRequestQuantity;

  @Column(nullable = false)
  private @NotNull BigDecimal quantity;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal unitCostInCurrency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal totalCostInSystemCurrency;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  public void buildCalculatedFields(String systemCurrencyCode) {
    // Tooling cost is agnostic of the CR line quantity
    BigDecimal exchangeRateToSystemCurrency =
        toolingCostLine.getCurrency().findExchangeRate(systemCurrencyCode);
    quantity = toolingCostLine.getQuantity();
    unitCostInCurrency = toolingCostLine.getUnitCostInCurrency();
    totalCostInSystemCurrency =
        unitCostInCurrency.multiply(quantity).multiply(exchangeRateToSystemCurrency);
  }
}
