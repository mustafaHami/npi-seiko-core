package my.lokalix.planning.core.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.entities.CostRequestLineEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.OutsourcingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostRequestLineRepository extends JpaRepository<CostRequestLineEntity, UUID> {
  Optional<CostRequestLineEntity> findByCostRequestAndCostRequestLineId(
      CostRequestEntity costRequest, UUID lineUid);

  List<CostRequestLineEntity> findByStatus(CostRequestStatus status);

  Page<CostRequestLineEntity> findByStatusInAndCostRequest_ArchivedFalse(
      Pageable pageable, List<CostRequestStatus> statuses);

  @Query(
      """
      SELECT crl FROM CostRequestLineEntity crl
      WHERE crl.costRequest.costRequestId = :costRequestId
      AND LOWER(crl.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      """)
  Page<CostRequestLineEntity> findBySearch(
      Pageable pageable,
      @Param("costRequestId") UUID costRequestId,
      @Param("searchText") String searchText);

  @Query(
      """
              SELECT crl FROM CostRequestLineEntity crl
              WHERE crl.outsourcingStatus = :status
              AND LOWER(crl.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
              """)
  Page<CostRequestLineEntity> findByOutsourcingStatusAndSearch(
      Pageable pageable,
      @Param("status") OutsourcingStatus status,
      @Param("searchText") String searchText);

  Page<CostRequestLineEntity> findByOutsourcingStatus(
      Pageable pageable, OutsourcingStatus outsourcingStatus);

  @Query(
      """
                  SELECT crl FROM CostRequestLineEntity crl
                  WHERE crl.status IN :statuses
                  AND crl.costRequest.archived = false
                  AND LOWER(crl.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
                  """)
  Page<CostRequestLineEntity> findByStatusInAndSearchAndParentArchivedFalse(
      Pageable pageable,
      @Param("statuses") List<CostRequestStatus> statuses,
      @Param("searchText") String searchText);

  // --- Dashboard queries ---

  @Query(
      """
      SELECT COUNT(crl) FROM CostRequestLineEntity crl
      WHERE crl.status = :status
      AND crl.costRequest.archived = false
      """)
  long dashboardCountByStatusAndCostRequestArchivedFalse(@Param("status") CostRequestStatus status);

  @Query(
      """
      SELECT MIN(crl.updatedAt) FROM CostRequestLineEntity crl
      WHERE crl.status = :status
      AND crl.costRequest.archived = false
      """)
  OffsetDateTime dashboardFindOldestUpdatedAtByStatus(@Param("status") CostRequestStatus status);

  @Query(
      """
      SELECT COUNT(crl) FROM CostRequestLineEntity crl
      WHERE crl.outsourced = true
      AND crl.outsourcingStatus = :outsourcingStatus
      AND crl.costRequest.archived = false
      AND crl.costRequest.status NOT IN :excludedStatuses
      """)
  long dashboardCountOutsourcedLinesToEstimate(
      @Param("outsourcingStatus") OutsourcingStatus outsourcingStatus,
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  @Query(
      """
    SELECT crl FROM CostRequestLineEntity crl
    WHERE crl.status NOT IN :frozenStatuses
    AND crl.costRequest.status NOT IN :frozenStatuses
    AND crl.costRequest.archived = false
    AND crl.otherCostLines IS NOT EMPTY
    """)
  List<CostRequestLineEntity> findAllNotFreezedAndHavingOtherCosts(
      List<CostRequestStatus> frozenStatuses);
}
