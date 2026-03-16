package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.mappers.CostRequestMapper;
import my.lokalix.planning.core.mappers.CustomerMapper;
import my.lokalix.planning.core.mappers.EnumMapper;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.OutsourcingStatus;
import my.lokalix.planning.core.repositories.CostRequestLineRepository;
import my.lokalix.planning.core.repositories.CostRequestRepository;
import my.lokalix.planning.core.repositories.MaterialLineRepository;
import my.lokalix.planning.core.repositories.ToolingCostLineRepository;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DashboardService {

  private static final List<CostRequestStatus> FINALIZED_STATUSES =
      List.of(
          CostRequestStatus.READY_TO_QUOTE,
          CostRequestStatus.ACTIVE,
          CostRequestStatus.WON,
          CostRequestStatus.LOST,
          CostRequestStatus.ABORTED);

  private static final List<CostRequestStatus> PIPELINE_EXCLUDED_STATUSES =
      List.of(
          CostRequestStatus.PENDING_INFORMATION,
          CostRequestStatus.READY_FOR_REVIEW,
          CostRequestStatus.READY_TO_QUOTE,
          CostRequestStatus.ABORTED);

  private static final int AT_RISK_DAYS_WINDOW = 14;

  private final CostRequestRepository costRequestRepository;
  private final CostRequestLineRepository costRequestLineRepository;
  private final ToolingCostLineRepository toolingCostLineRepository;
  private final MaterialLineRepository materialLineRepository;
  private final CustomerMapper customerMapper;
  private final EnumMapper enumMapper;
  private final AppConfigurationProperties appConfigurationProperties;
  private final CostRequestMapper costRequestMapper;

  @Transactional
  public SWDashboard retrieveDashboard() {
    SWDashboard dashboard = new SWDashboard();
    dashboard.setGlobalKpis(buildGlobalKpis());
    dashboard.setCostRequestCountByStatus(buildCountByStatus());
    dashboard.setTop5WorstCostRequestsByLeadTime(buildTop5WorstByLeadTime());
    dashboard.setEngineerBacklog(buildEngineerBacklog());
    dashboard.setProcurementBacklog(buildProcurementBacklog());
    dashboard.setManagementBacklog(buildManagementBacklog());
    dashboard.setTop5ClientsByOpenVolume(buildTop5CustomersByOpenVolume());
    dashboard.setTop5ClientsByActiveVolume(buildTop5CustomersByActiveVolume());
    return dashboard;
  }

  private SWDashboardGlobalKpis buildGlobalKpis() {
    SWDashboardGlobalKpis kpis = new SWDashboardGlobalKpis();
    LocalDate now = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());
    OffsetDateTime startOfCurrentMonthInUTC =
        TimeUtils.convertZonedFirstTimeOfMonthToUTC(
            now, appConfigurationProperties.getAppTimezone());
    OffsetDateTime endOfCurrentMonthInUTC =
        TimeUtils.convertZonedLastTimeOfMonthToUTC(
            now, appConfigurationProperties.getAppTimezone());
    long openCount = costRequestRepository.dashboardCountOpenCostRequests(FINALIZED_STATUSES);
    long activeCount =
        costRequestRepository.dashboardCountByStatusWithActiveStatusDate(
            CostRequestStatus.ACTIVE, startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    long wonCount =
        costRequestRepository.dashboardCountByStatusWithFinalizationDateBetween(
            CostRequestStatus.WON, startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    long lostCount =
        costRequestRepository.dashboardCountByStatusWithFinalizationDateBetween(
            CostRequestStatus.LOST, startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    long newRevisionCount =
        costRequestRepository.dashboardCountByStatusWithFinalizationDateBetween(
            CostRequestStatus.NEW_REVISION_CREATED,
            startOfCurrentMonthInUTC,
            endOfCurrentMonthInUTC);
    kpis.setTotalOpenCostRequests((int) openCount);
    kpis.setTotalActiveCostRequests((int) activeCount);
    kpis.setTotalWonCostRequests((int) wonCount);
    kpis.setTotalLostCostRequests((int) lostCount);
    kpis.setTotalNewRevisionCostRequests((int) newRevisionCount);
    List<String> excluded = FINALIZED_STATUSES.stream().map(CostRequestStatus::getValue).toList();
    Double avgLeadTimeOpen = costRequestRepository.dashboardFindAverageLeadTimeDaysOpen(excluded);
    Double avgLeadTimeActive =
        costRequestRepository.dashboardFindAverageLeadTimeDaysActive(
            startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    Double avgLeadTimeWon =
        costRequestRepository.dashboardFindAverageLeadTimeDaysWon(
            startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    Double avgLeadTimeLost =
        costRequestRepository.dashboardFindAverageLeadTimeDaysLost(
            startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    Double avgLeadTimeNewRevision =
        costRequestRepository.dashboardFindAverageLeadTimeDaysNewRevision(
            startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);
    kpis.setAverageLeadTimeDaysOpenCostRequests(
        avgLeadTimeOpen != null ? BigDecimal.valueOf(avgLeadTimeOpen) : null);
    kpis.setAverageLeadTimeDaysActiveCostRequests(
        avgLeadTimeActive != null ? BigDecimal.valueOf(avgLeadTimeActive) : null);
    kpis.setAverageLeadTimeDaysWonCostRequests(
        avgLeadTimeWon != null ? BigDecimal.valueOf(avgLeadTimeWon) : null);
    kpis.setAverageLeadTimeDaysLostCostRequests(
        avgLeadTimeLost != null ? BigDecimal.valueOf(avgLeadTimeLost) : null);
    kpis.setAverageLeadTimeDaysNewRevisionCostRequests(
        avgLeadTimeNewRevision != null ? BigDecimal.valueOf(avgLeadTimeNewRevision) : null);
    kpis.setPipelineValueInTargetCurrency(
        costRequestRepository.dashboardFindTotalPipelineValue(PIPELINE_EXCLUDED_STATUSES));

    LocalDate today = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());
    kpis.setExpiredCostRequest(
        costRequestMapper.toListSWCostRequest(
            costRequestRepository.dashboardCountExpiredCostRequests(
                today, List.of(CostRequestStatus.READY_TO_QUOTE, CostRequestStatus.ACTIVE))));
    kpis.setCostRequestsAtRisk(
        costRequestMapper.toListSWCostRequest(
            costRequestRepository.dashboardCostRequestsAtRisk(
                today.plusDays(AT_RISK_DAYS_WINDOW), FINALIZED_STATUSES)));

    return kpis;
  }

  private List<SWDashboardCostRequestCountByStatus> buildCountByStatus() {
    List<Object[]> rows =
        costRequestRepository.dashboardFindCountByStatus(
            List.of(
                CostRequestStatus.WON,
                CostRequestStatus.LOST,
                CostRequestStatus.NEW_REVISION_CREATED,
                CostRequestStatus.ACTIVE,
                CostRequestStatus.ABORTED));
    List<SWDashboardCostRequestCountByStatus> result = new ArrayList<>();
    for (Object[] row : rows) {
      SWDashboardCostRequestCountByStatus entry = new SWDashboardCostRequestCountByStatus();
      entry.setStatus(enumMapper.asSwCostRequestStatus((CostRequestStatus) row[0]));
      entry.setCount(((Long) row[1]).intValue());
      result.add(entry);
    }
    return result;
  }

  private List<SWDashboardCostRequest> buildTop5WorstByLeadTime() {
    List<CostRequestEntity> entities =
        costRequestRepository.dashboardFindTop5WorstByLeadTime(FINALIZED_STATUSES);
    List<SWDashboardCostRequest> result = new ArrayList<>();
    for (CostRequestEntity cr : entities) {
      SWDashboardCostRequest entry = new SWDashboardCostRequest();
      entry.setCostRequestReferenceNumber(cr.getCostRequestReferenceNumber());
      entry.setCostRequestRevision(cr.getCostRequestRevision());
      if (cr.getCustomer() != null) {
        entry.setCustomer(customerMapper.toSWCustomer(cr.getCustomer()));
      }
      entry.setStatus(enumMapper.asSwCostRequestStatus(cr.getStatus()));
      entry.setCreationDate(cr.getCreationDate());
      entry.setAverageLeadTime(
          BigDecimal.valueOf(ChronoUnit.DAYS.between(cr.getCreationDate(), OffsetDateTime.now())));
      result.add(entry);
    }
    return result;
  }

  private SWDashboardEngineerBacklog buildEngineerBacklog() {
    SWDashboardEngineerBacklog backlog = new SWDashboardEngineerBacklog();
    backlog.setReadyToEstimateCount(
        (int)
            costRequestRepository.countByStatusAndArchivedFalse(
                CostRequestStatus.READY_TO_ESTIMATE));
    backlog.setReadyForReviewCount(
        (int)
            costRequestRepository.countByStatusAndArchivedFalse(
                CostRequestStatus.READY_FOR_REVIEW));
    backlog.setPendingReestimationCount(
        (int)
            costRequestRepository.countByStatusAndArchivedFalse(
                CostRequestStatus.PENDING_REESTIMATION));
    return backlog;
  }

  private SWDashboardProcurementBacklog buildProcurementBacklog() {
    SWDashboardProcurementBacklog backlog = new SWDashboardProcurementBacklog();
    backlog.setMaterialsToEstimateCount(
        (int)
            materialLineRepository.dashboardCountByMaterialStatusAndActiveCostRequest(
                MaterialStatus.TO_BE_ESTIMATED, FINALIZED_STATUSES));
    backlog.setToolingToOutsourceCount(
        (int)
            toolingCostLineRepository.dashboardCountByOutsourcingStatusAndActiveCostRequest(
                OutsourcingStatus.TO_BE_ESTIMATED, FINALIZED_STATUSES));
    backlog.setCostLinesToOutsourceCount(
        (int)
            costRequestLineRepository.dashboardCountOutsourcedLinesToEstimate(
                OutsourcingStatus.TO_BE_ESTIMATED, FINALIZED_STATUSES));
    return backlog;
  }

  private SWDashboardManagementBacklog buildManagementBacklog() {
    SWDashboardManagementBacklog backlog = new SWDashboardManagementBacklog();
    backlog.setPendingApprovalCount(
        (int)
            costRequestLineRepository.dashboardCountByStatusAndCostRequestArchivedFalse(
                CostRequestStatus.PENDING_APPROVAL));
    backlog.setOldestPendingApprovalDate(
        costRequestLineRepository.dashboardFindOldestUpdatedAtByStatus(
            CostRequestStatus.PENDING_APPROVAL));
    return backlog;
  }

  private List<SWDashboardClientOpenVolume> buildTop5CustomersByOpenVolume() {
    List<Object[]> rows =
        costRequestRepository.dashboardFindTop5CustomersByOpenVolume(FINALIZED_STATUSES);
    List<SWDashboardClientOpenVolume> result = new ArrayList<>();
    for (Object[] row : rows) {
      SWDashboardClientOpenVolume entry = new SWDashboardClientOpenVolume();
      entry.setCustomer(customerMapper.toSWCustomer((CustomerEntity) row[0]));
      entry.setOpenCostRequestsCount(((Long) row[1]).intValue());
      result.add(entry);
    }
    return result;
  }

  private List<SWDashboardClientActiveVolume> buildTop5CustomersByActiveVolume() {
    LocalDate now = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());
    OffsetDateTime startOfCurrentMonthInUTC =
        TimeUtils.convertZonedFirstTimeOfMonthToUTC(
            now, appConfigurationProperties.getAppTimezone());
    OffsetDateTime endOfCurrentMonthInUTC =
        TimeUtils.convertZonedLastTimeOfMonthToUTC(
            now, appConfigurationProperties.getAppTimezone());
    List<Object[]> rows =
        costRequestRepository.dashboardFindTop5CustomersByActiveVolume(
            startOfCurrentMonthInUTC, endOfCurrentMonthInUTC);

    List<SWDashboardClientActiveVolume> result = new ArrayList<>();
    for (Object[] row : rows) {
      SWCustomer customer = new SWCustomer();
      customer.setCode((String) row[0]);
      customer.setName((String) row[1]);
      SWDashboardClientActiveVolume entry = new SWDashboardClientActiveVolume();
      entry.setCustomer(customer);
      entry.setActiveCostRequestsCount(((Long) row[2]).intValue());
      result.add(entry);
    }
    return result;
  }
}
