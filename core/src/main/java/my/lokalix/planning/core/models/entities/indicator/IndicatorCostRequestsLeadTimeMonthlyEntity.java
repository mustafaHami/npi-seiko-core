package my.lokalix.planning.core.models.entities.indicator;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "indicator_cost_requests_lead_time_monthly")
@ToString(of = {"id"})
@EqualsAndHashCode(of = {"id"})
public class IndicatorCostRequestsLeadTimeMonthlyEntity {

  @EmbeddedId private IndicatorCostRequestsLeadTimeMonthlyId id;

  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal minimumLeadTime;

  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal firstQuartileLeadTime;

  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal medianLeadTime;

  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal thirdQuartileLeadTime;

  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal maximumLeadTime;

  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal meanLeadTime;

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime lastUpdate = TimeUtils.nowOffsetDateTimeUTC();
}
