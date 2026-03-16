package my.lokalix.planning.core.models.entities.indicator;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "indicator_cost_requests_count_monthly")
@ToString(of = {"id"})
@EqualsAndHashCode(of = {"id"})
public class IndicatorCostRequestsCountMonthlyEntity {

  @EmbeddedId private IndicatorCostRequestsCountMonthlyId id;

  @Column(nullable = false)
  private int quantity;

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime lastUpdate = TimeUtils.nowOffsetDateTimeUTC();
}
