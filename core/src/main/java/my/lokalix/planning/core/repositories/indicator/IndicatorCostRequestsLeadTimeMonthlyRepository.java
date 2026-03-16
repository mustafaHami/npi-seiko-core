package my.lokalix.planning.core.repositories.indicator;

import java.util.List;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsLeadTimeMonthlyEntity;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsLeadTimeMonthlyId;
import my.lokalix.planning.core.models.interfaces.WhiskerBarStatsProjection;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndicatorCostRequestsLeadTimeMonthlyRepository
    extends JpaRepository<
        IndicatorCostRequestsLeadTimeMonthlyEntity, IndicatorCostRequestsLeadTimeMonthlyId> {
  List<IndicatorCostRequestsLeadTimeMonthlyEntity> findById_CustomerNameIn(
      List<String> customerNames, Sort sort);

  @Query(
      """
    SELECT
      min(i.minimumLeadTime) as minimumLeadTime,
      avg(i.firstQuartileLeadTime) as firstQuartileLeadTime,
      avg(i.medianLeadTime) as medianLeadTime,
      avg(i.thirdQuartileLeadTime) as thirdQuartileLeadTime,
      max(i.maximumLeadTime) as maximumLeadTime,
      avg(i.meanLeadTime) as meanLeadTime
    FROM IndicatorCostRequestsLeadTimeMonthlyEntity i
    WHERE i.id.customerName in :customerNames
    GROUP BY i.id.firstDayOfMonth
    ORDER BY i.id.firstDayOfMonth
    """)
  List<WhiskerBarStatsProjection> summarizeForMonth(List<String> customerNames);
}
