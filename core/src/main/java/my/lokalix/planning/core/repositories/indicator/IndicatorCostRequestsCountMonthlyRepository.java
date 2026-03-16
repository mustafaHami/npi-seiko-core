package my.lokalix.planning.core.repositories.indicator;

import java.util.List;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsCountMonthlyEntity;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsCountMonthlyId;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndicatorCostRequestsCountMonthlyRepository
    extends JpaRepository<
        IndicatorCostRequestsCountMonthlyEntity, IndicatorCostRequestsCountMonthlyId> {
  List<IndicatorCostRequestsCountMonthlyEntity> findById_CustomerNameIn(
      List<String> customerNames, Sort sort);

  @Query(
      """
              SELECT sum(i.quantity)
              FROM IndicatorCostRequestsCountMonthlyEntity i
              WHERE i.id.customerName in :customerNames
              GROUP BY i.id.firstDayOfMonth
              ORDER BY i.id.firstDayOfMonth
            """)
  List<Long> sumQuantityByMonth(List<String> customerNames);
}
