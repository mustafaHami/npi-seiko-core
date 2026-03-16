package my.lokalix.planning.core.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostRequestRepository extends JpaRepository<CostRequestEntity, UUID> {
  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE cr.archived = false
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findByArchivedFalse(Pageable pageable);

  Page<CostRequestEntity> findByArchivedTrue(Pageable pageable);

  List<CostRequestEntity> findByArchivedFalseAndStatusNotIn(
      List<CostRequestStatus> statuses, Sort sort);

  List<CostRequestEntity> findByArchivedTrue(Sort sort);

  List<CostRequestEntity> findByStatus(CostRequestStatus status, Sort sort);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND cr.archived = false
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findBySearchAndArchivedFalse(
      Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND cr.archived = true
      """)
  Page<CostRequestEntity> findBySearchAndArchivedTrue(
      Pageable pageable, @Param("searchText") String searchText);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findBySearch(Pageable pageable, @Param("searchText") String searchText);

  @Query(
      "SELECT COUNT(c) FROM CostRequestEntity c WHERE c.costRequestReferenceNumber = :costRequestReferenceNumber")
  long countByRfqReferenceNumber(String costRequestReferenceNumber);

  @Query(
      """
              SELECT c.costRequestReferenceNumber
              FROM CostRequestEntity c
              WHERE c.costRequestReferenceNumber LIKE CONCAT(:costRequestReferenceNumber,'%')
              AND c.archived = false
              ORDER BY c.costRequestReferenceNumber DESC
              LIMIT 1
        """)
  String findSameReferenceWithHighestCloneNumber(String costRequestReferenceNumber);

  // Engineering search: filters cost requests that have at least one matching line (via EXISTS)
  // :hasCrStatuses / :hasLineStatuses are booleans that short-circuit the IN clause when false,
  // avoiding Hibernate issues with empty collections (always pass a non-empty list as placeholder)
  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE cr.archived = true
      AND (:hasSearchText = false OR LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%')))
      AND (:hasCrStatuses = false OR cr.status IN :crStatuses)
      AND EXISTS (
          SELECT 1 FROM CostRequestLineEntity l WHERE l.costRequest = cr
          AND (:hasLineStatuses = false OR l.status IN :lineStatuses)
      )
      """)
  Page<CostRequestEntity> findForEngineeringArchivedOnly(
      Pageable pageable,
      @Param("hasSearchText") boolean hasSearchText,
      @Param("searchText") String searchText,
      @Param("hasCrStatuses") boolean hasCrStatuses,
      @Param("crStatuses") List<CostRequestStatus> crStatuses,
      @Param("hasLineStatuses") boolean hasLineStatuses,
      @Param("lineStatuses") List<CostRequestStatus> lineStatuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE (:archived IS NULL OR cr.archived = :archived)
      AND (:hasSearchText = false OR LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%')))
      AND (:hasCrStatuses = false OR cr.status IN :crStatuses)
      AND EXISTS (
          SELECT 1 FROM CostRequestLineEntity l WHERE l.costRequest = cr
          AND (:hasLineStatuses = false OR l.status IN :lineStatuses)
      )
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findForEngineeringNonArchivedOrAll(
      Pageable pageable,
      @Param("archived") Boolean archived,
      @Param("hasSearchText") boolean hasSearchText,
      @Param("searchText") String searchText,
      @Param("hasCrStatuses") boolean hasCrStatuses,
      @Param("crStatuses") List<CostRequestStatus> crStatuses,
      @Param("hasLineStatuses") boolean hasLineStatuses,
      @Param("lineStatuses") List<CostRequestStatus> lineStatuses);

  // Methods with status filtering
  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status IN :statuses
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findByArchivedFalseAndStatusIn(
      Pageable pageable, @Param("statuses") List<CostRequestStatus> statuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE cr.archived = true
      AND cr.status IN :statuses
      """)
  Page<CostRequestEntity> findByArchivedTrueAndStatusIn(
      Pageable pageable, @Param("statuses") List<CostRequestStatus> statuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE cr.status IN :statuses
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findByStatusIn(
      Pageable pageable, @Param("statuses") List<CostRequestStatus> statuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND cr.archived = false
      AND cr.status IN :statuses
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findBySearchAndArchivedFalseAndStatusIn(
      Pageable pageable,
      @Param("searchText") String searchText,
      @Param("statuses") List<CostRequestStatus> statuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND cr.archived = true
      AND cr.status IN :statuses
      """)
  Page<CostRequestEntity> findBySearchAndArchivedTrueAndStatusIn(
      Pageable pageable,
      @Param("searchText") String searchText,
      @Param("statuses") List<CostRequestStatus> statuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE LOWER(cr.searchableConcatenatedFields) LIKE LOWER(CONCAT('%', :searchText, '%'))
      AND cr.status IN :statuses
      ORDER BY
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN 0 ELSE 1 END ASC,
        CASE WHEN cr.expirationDate IS NOT NULL AND cr.expirationDate < CURRENT_DATE THEN cr.expirationDate ELSE NULL END ASC,
        cr.creationDate DESC
      """)
  Page<CostRequestEntity> findBySearchAndStatusIn(
      Pageable pageable,
      @Param("searchText") String searchText,
      @Param("statuses") List<CostRequestStatus> statuses);

  // --- Dashboard queries ---

  @Query(
      """
      SELECT COUNT(cr) FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status NOT IN :excludedStatuses
      """)
  long dashboardCountOpenCostRequests(
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  @Query(
      value =
          """
        SELECT AVG(EXTRACT(EPOCH FROM (NOW() - creation_date)) / 86400)
        FROM cost_request cr
        WHERE cr.status NOT IN (:excludedStatuses)
        AND cr.archived = false
        """,
      nativeQuery = true)
  Double dashboardFindAverageLeadTimeDaysOpen(
      @Param("excludedStatuses") List<String> excludedStatuses);

  @Query(
      """
      SELECT COUNT(cr) FROM CostRequestEntity cr
      WHERE cr.activeStatusDate is not null
      AND cr.archived = false
      AND cr.activeStatusDate BETWEEN :fromDate AND :toDate
      """)
  long dashboardCountByStatusWithActiveStatusDate(
      @Param("status") CostRequestStatus status,
      @Param("fromDate") OffsetDateTime fromDate,
      @Param("toDate") OffsetDateTime toDate);

  @Query(
      """
          SELECT COUNT(cr) FROM CostRequestEntity cr
          WHERE cr.status = :status
          AND cr.finalizationDate is not null
          AND cr.finalizationDate BETWEEN :fromDate AND :toDate
          """)
  long dashboardCountByStatusWithFinalizationDateBetween(
      @Param("status") CostRequestStatus status,
      @Param("fromDate") OffsetDateTime fromDate,
      @Param("toDate") OffsetDateTime toDate);

  @Query(
      value =
          """
          SELECT AVG(EXTRACT(EPOCH FROM (active_status_date - creation_date)) / 86400)
          FROM cost_request cr
          WHERE cr.active_status_date BETWEEN :fromDate AND :toDate
          AND cr.archived = false
          """,
      nativeQuery = true)
  Double dashboardFindAverageLeadTimeDaysActive(
      @Param("fromDate") OffsetDateTime fromDate, @Param("toDate") OffsetDateTime toDate);

  @Query(
      value =
          """
            SELECT AVG(EXTRACT(EPOCH FROM (finalization_date - creation_date)) / 86400)
            FROM cost_request
            WHERE finalization_date BETWEEN :fromDate AND :toDate
            AND status = 'WON'
          """,
      nativeQuery = true)
  Double dashboardFindAverageLeadTimeDaysWon(
      @Param("fromDate") OffsetDateTime fromDate, @Param("toDate") OffsetDateTime toDate);

  @Query(
      value =
          """
            SELECT AVG(EXTRACT(EPOCH FROM (finalization_date - creation_date)) / 86400)
            FROM cost_request
            WHERE finalization_date BETWEEN :fromDate AND :toDate
            AND status = 'LOST'
          """,
      nativeQuery = true)
  Double dashboardFindAverageLeadTimeDaysLost(
      @Param("fromDate") OffsetDateTime fromDate, @Param("toDate") OffsetDateTime toDate);

  @Query(
      value =
          """
            SELECT AVG(EXTRACT(EPOCH FROM (finalization_date - creation_date)) / 86400)
            FROM cost_request
            WHERE finalization_date BETWEEN :fromDate AND :toDate
            AND status = 'NEW_REVISION_CREATED'
          """,
      nativeQuery = true)
  Double dashboardFindAverageLeadTimeDaysNewRevision(
      @Param("fromDate") OffsetDateTime fromDate, @Param("toDate") OffsetDateTime toDate);

  @Query(
      """
            SELECT cr
            FROM CostRequestEntity cr
            WHERE cr.creationDate is not null
            AND cr.activeStatusDate is not null
            AND (cr.activeStatusDate >= :startDate AND cr.activeStatusDate <= :endDate)
            """)
  List<CostRequestEntity> findByDateBetweenAndActive(
      OffsetDateTime startDate, OffsetDateTime endDate, Sort sort);

  @Query(
      """
      SELECT COUNT(cr)
      FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status != :excludedStatus
      """)
  long dashboardCountNonAbortedCostRequests(
      @Param("excludedStatus") CostRequestStatus excludedStatus);

  @Query(
      """
      SELECT COALESCE(SUM(cr.totalLinesCostInSystemCurrency), 0)
      FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status NOT IN :excludedStatuses
      AND cr.totalLinesCostInSystemCurrency IS NOT NULL
      """)
  BigDecimal dashboardFindTotalPipelineValue(
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  @Query(
"""
      SELECT cr
      FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status NOT IN :excludedStatuses
      AND cr.purchaseOrderExpectedDate IS NOT NULL
      AND cr.purchaseOrderExpectedDate <= :toDate
      ORDER BY cr.purchaseOrderExpectedDate ASC
""")
  List<CostRequestEntity> dashboardCostRequestsAtRisk(
      @Param("toDate") LocalDate toDate,
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  @Query(
      """
      SELECT cr.status, COUNT(cr)
      FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status NOT IN :excludedStatuses
      GROUP BY cr.status
      ORDER BY CASE cr.status
        WHEN 'PENDING_INFORMATION' THEN 1
        WHEN 'READY_FOR_REVIEW' THEN 2
        WHEN 'READY_TO_ESTIMATE' THEN 3
        WHEN 'PENDING_REESTIMATION' THEN 4
        WHEN 'READY_FOR_MARKUP' THEN 5
        ELSE 6
      END ASC
      """)
  List<Object[]> dashboardFindCountByStatus(
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  @Query(
      """
      SELECT cr FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status NOT IN :excludedStatuses
      ORDER BY cr.creationDate ASC
      LIMIT 5
      """)
  List<CostRequestEntity> dashboardFindTop5WorstByLeadTime(
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  long countByStatusAndArchivedFalse(CostRequestStatus status);

  @Query(
      """
      SELECT cr.customer, COUNT(cr)
      FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.status NOT IN :excludedStatuses
      AND cr.customer IS NOT NULL
      GROUP BY cr.customer
      ORDER BY COUNT(cr) DESC
      LIMIT 5
      """)
  List<Object[]> dashboardFindTop5CustomersByOpenVolume(
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);

  @Query(
      """
      SELECT cr.customer.code, cr.customer.name, COUNT(cr)
      FROM CostRequestEntity cr
      WHERE cr.customer IS NOT NULL
      AND cr.activeStatusDate BETWEEN :fromDate AND :toDate
      AND cr.archived = false
      GROUP BY cr.customer.code, cr.customer.name
      ORDER BY COUNT(cr) DESC
      LIMIT 5
      """)
  List<Object[]> dashboardFindTop5CustomersByActiveVolume(
      @Param("fromDate") OffsetDateTime fromDate, @Param("toDate") OffsetDateTime toDate);

  @Query(
      """
      SELECT cr
      FROM CostRequestEntity cr
      WHERE cr.archived = false
      AND cr.expirationDate < :today
      AND cr.status IN :excludedStatuses
      """)
  List<CostRequestEntity> dashboardCountExpiredCostRequests(
      @Param("today") LocalDate today,
      @Param("excludedStatuses") List<CostRequestStatus> excludedStatuses);
}
