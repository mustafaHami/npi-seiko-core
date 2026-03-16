package my.lokalix.planning.core.repositories;

import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.ToolingCostLineEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.OutsourcingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolingCostLineRepository extends JpaRepository<ToolingCostLineEntity, UUID> {

  @Query(
      """
          SELECT t FROM ToolingCostLineEntity t
          WHERE t.outsourcingStatus = :status
          """)
  Page<ToolingCostLineEntity> findByOutsourcingStatus(
      Pageable pageable, @Param("status") OutsourcingStatus status);

  @Query(
      """
          SELECT t FROM ToolingCostLineEntity t
          WHERE t.outsourcingStatus = :status
          AND LOWER(t.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
          """)
  Page<ToolingCostLineEntity> findByOutsourcingStatusAndSearch(
      Pageable pageable,
      @Param("status") OutsourcingStatus status,
      @Param("searchText") String searchText);

  // --- Dashboard queries ---

  @Query(
      """
      SELECT COUNT(t) FROM ToolingCostLineEntity t
      WHERE t.outsourcingStatus = :outsourcingStatus
      AND t.costRequestLine.costRequest.archived = false
      AND t.costRequestLine.costRequest.status NOT IN :excludedStatuses
      """)
  long dashboardCountByOutsourcingStatusAndActiveCostRequest(
      @Param("outsourcingStatus") OutsourcingStatus outsourcingStatus,
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);
}
