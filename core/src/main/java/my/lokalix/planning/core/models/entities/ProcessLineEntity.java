package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.ProcessEntity;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "process_line")
@EqualsAndHashCode(of = "processLineId")
public class ProcessLineEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID processLineId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_line_id", nullable = false)
  private CostRequestLineEntity costRequestLine;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "process_id", nullable = false)
  private ProcessEntity process;

  /// VALUE FOR 1 unit ///
  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal quantity;

  // Can override the process cycle time
  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal processCycleTimeInSeconds;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal unitCostInSystemCurrency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal totalCostInSystemCurrency;

  /// ///////////////////////////////////////
  @OneToMany(mappedBy = "processLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<ProcessLinePerCostRequestQuantityEntity> processLineForCostRequestQuantities =
      new ArrayList<>();

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @Column(nullable = false, name = "index_id")
  private int indexId;

  public void addProcessLinePerCostRequestQuantity(ProcessLinePerCostRequestQuantityEntity entity) {
    entity.setIndexId(processLineForCostRequestQuantities.size());
    entity.setProcessLine(this);
    processLineForCostRequestQuantities.add(entity);
  }

  public void clearProcessLinePerCostRequestQuantities() {
    this.processLineForCostRequestQuantities.clear();
  }

  public void buildCalculatedFields() {
    BigDecimal costPerSecond =
        process.getCostPerMinute().divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
    BigDecimal cycleTimeInSeconds = processCycleTimeInSeconds;
    unitCostInSystemCurrency = cycleTimeInSeconds.multiply(costPerSecond);
    totalCostInSystemCurrency = unitCostInSystemCurrency.multiply(quantity);
  }
}
