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
@Table(name = "process_line_per_cost_request_quantity")
@EqualsAndHashCode(of = "processLinePerCostRequestQuantityId")
public class ProcessLinePerCostRequestQuantityEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID processLinePerCostRequestQuantityId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "process_line_id", nullable = false)
  private ProcessLineEntity processLine;

  @Column(nullable = false)
  private int costRequestQuantity;

  @Column(nullable = false)
  private @NotNull BigDecimal quantity;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal unitCostInSystemCurrency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal totalCostInSystemCurrency;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  public void buildCalculatedFields() {
    BigDecimal costPerSecond =
        processLine
            .getProcess()
            .getCostPerMinute()
            .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
    BigDecimal cycleTimeInSeconds = processLine.getProcessCycleTimeInSeconds();
    unitCostInSystemCurrency = cycleTimeInSeconds.multiply(costPerSecond);
    if (processLine.getProcess().isSetupProcess()) {
      quantity = processLine.getQuantity();
      totalCostInSystemCurrency = unitCostInSystemCurrency.multiply(quantity);
    } else {
      quantity = BigDecimal.valueOf(costRequestQuantity).multiply(processLine.getQuantity());
      totalCostInSystemCurrency = unitCostInSystemCurrency.multiply(quantity);
    }
  }
}
