package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.repositories.ProcessLineRepository;
import my.zkonsulting.planning.generated.model.*;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DashboardService {

  private static final List<NpiOrderStatus> FINAL_STATUSES =
      List.of(NpiOrderStatus.COMPLETED, NpiOrderStatus.ABORTED);

  private static final List<ProcessLineStatus> ACTIVE_PROCESS_LINE_STATUSES =
      List.of(ProcessLineStatus.NOT_STARTED, ProcessLineStatus.IN_PROGRESS);

  private final NpiOrderRepository npiOrderRepository;
  private final ProcessLineRepository processLineRepository;

  @Transactional
  public SWDashboard retrieveDashboard() {
    SWDashboard dashboard = new SWDashboard();
    dashboard.setGlobalKpis(buildGlobalKpis());
    dashboard.setWorstNpiOrders(buildWorstNpiOrders());
    dashboard.setNpiCountByStatus(buildNpiCountByStatus());
    dashboard.setProcessStageKpis(buildProcessStageKpis());
    return dashboard;
  }

  private SWDashboardGlobalKpis buildGlobalKpis() {
    long openCount = npiOrderRepository.countOpenNpiOrders(FINAL_STATUSES);
    long completedCount = npiOrderRepository.countCompletedNpiOrders();
    long totalOpenQty = npiOrderRepository.sumOpenNpiOrdersQuantity(FINAL_STATUSES);
    long customerApprovalPending =
        processLineRepository.countCustomerApprovalByStatus(
            ProcessLineStatus.IN_PROGRESS, FINAL_STATUSES);

    Double avgLeadTimeOpen = npiOrderRepository.dashboardAvgLeadTimeDaysOpenNpiOrders();
    Double avgLeadTimeCompleted = npiOrderRepository.dashboardAvgLeadTimeDaysCompletedNpiOrders();

    SWDashboardGlobalKpis kpis = new SWDashboardGlobalKpis();
    kpis.setOpenNpiCount((int) openCount);
    kpis.setCompletedNpiCount((int) completedCount);
    kpis.setAverageLeadTimeDaysOpenNpiOrders(
        avgLeadTimeOpen != null ? BigDecimal.valueOf(avgLeadTimeOpen) : BigDecimal.ZERO);
    kpis.setAverageLeadTimeDaysCompletedNpiOrders(
        avgLeadTimeCompleted != null ? BigDecimal.valueOf(avgLeadTimeCompleted) : BigDecimal.ZERO);
    kpis.setTotalOpenQuantity((int) totalOpenQty);
    kpis.setCustomerApprovalPendingCount((int) customerApprovalPending);
    return kpis;
  }

  private List<SWDashboardWorstNpiOrder> buildWorstNpiOrders() {
    List<NpiOrderEntity> openOrders = npiOrderRepository.findOpenNpiOrders(FINAL_STATUSES);
    return openOrders.stream()
        .filter(n -> n.getPlannedDeliveryDate() != null && n.getTargetDeliveryDate() != null)
        .sorted(
            Comparator.comparingLong(
                    (NpiOrderEntity n) ->
                        ChronoUnit.DAYS.between(
                            n.getTargetDeliveryDate(), n.getPlannedDeliveryDate()))
                .reversed())
        .limit(5)
        .map(
            n -> {
              long delayInDays =
                  ChronoUnit.DAYS.between(n.getTargetDeliveryDate(), n.getPlannedDeliveryDate());
              SWDashboardWorstNpiOrder worst = new SWDashboardWorstNpiOrder();
              worst.setUid(n.getNpiOrderId());
              worst.setPurchaseOrderNumber(n.getPurchaseOrderNumber());
              worst.setPartNumber(n.getPartNumber());
              worst.setTargetDeliveryDate(n.getTargetDeliveryDate());
              worst.setPlannedDeliveryDate(n.getPlannedDeliveryDate());
              worst.setDelayInDays((int) delayInDays);
              return worst;
            })
        .toList();
  }

  private List<SWDashboardNpiCountByStatus> buildNpiCountByStatus() {
    return npiOrderRepository.dashboardCountOpenNpiOrdersGroupByStatus(FINAL_STATUSES).stream()
        .map(
            row -> {
              SWDashboardNpiCountByStatus item = new SWDashboardNpiCountByStatus();
              item.setStatus(SWNpiOrderStatus.fromValue(((NpiOrderStatus) row[0]).getValue()));
              item.setCount(((Number) row[1]).intValue());
              return item;
            })
        .toList();
  }

  private List<SWDashboardProcessStageKpi> buildProcessStageKpis() {
    List<Object[]> rows =
        processLineRepository.dashboardCountProcessLinesByProcessNameAndStatus(
            FINAL_STATUSES, ACTIVE_PROCESS_LINE_STATUSES);
    Map<String, SWDashboardProcessStageKpi> byProcessName = new java.util.LinkedHashMap<>();
    for (Object[] row : rows) {
      String processName = (String) row[0];
      int count = ((Number) row[2]).intValue();
      SWDashboardProcessStageKpi kpi =
          byProcessName.computeIfAbsent(
              processName,
              name -> {
                SWDashboardProcessStageKpi k = new SWDashboardProcessStageKpi();
                k.setProcessName(name);
                k.setNotStartedCount(0);
                k.setInProgressCount(0);
                return k;
              });
      ProcessLineStatus lineStatus = (ProcessLineStatus) row[1];
      if (lineStatus == ProcessLineStatus.NOT_STARTED) {
        kpi.setNotStartedCount(count);
      } else if (lineStatus == ProcessLineStatus.IN_PROGRESS) {
        kpi.setInProgressCount(count);
      }
    }
    return byProcessName.values().stream().toList();
  }
}
