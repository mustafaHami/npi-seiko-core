package my.lokalix.planning.core.services.indicator;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsCountMonthlyEntity;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsCountMonthlyId;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsLeadTimeMonthlyEntity;
import my.lokalix.planning.core.models.entities.indicator.IndicatorCostRequestsLeadTimeMonthlyId;
import my.lokalix.planning.core.models.indicator.WhiskerBarStats;
import my.lokalix.planning.core.models.interfaces.WhiskerBarStatsProjection;
import my.lokalix.planning.core.repositories.CostRequestRepository;
import my.lokalix.planning.core.repositories.admin.CustomerRepository;
import my.lokalix.planning.core.repositories.indicator.IndicatorCostRequestsCountMonthlyRepository;
import my.lokalix.planning.core.repositories.indicator.IndicatorCostRequestsLeadTimeMonthlyRepository;
import my.lokalix.planning.core.services.imports.CostRequestImportLine;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IndicatorService {

  private final AppConfigurationProperties appConfigurationProperties;
  private final IndicatorCostRequestsCountMonthlyRepository
      indicatorCostRequestsCountMonthlyRepository;
  private final IndicatorCostRequestsLeadTimeMonthlyRepository
      indicatorCostRequestsLeadTimeMonthlyRepository;
  private final CostRequestRepository costRequestRepository;
  private final CustomerRepository customerRepository;

  @Transactional
  public SWGraphWhisker buildCostRequestsLeadTimeIndicatorAsGraph(SWIndicatorsBody filters) {
    return buildMonthlyCostRequestsLeadTimeIndicatorAsGraph(filters);
  }

  private SWGraphWhisker buildMonthlyCostRequestsLeadTimeIndicatorAsGraph(
      SWIndicatorsBody filters) {
    List<IndicatorCostRequestsLeadTimeMonthlyEntity> entities;
    Sort sort = Sort.by(Sort.Direction.ASC, "id.firstDayOfMonth");

    List<String> customerNames = getCustomerNames(filters.getCustomerIds());
    entities =
        indicatorCostRequestsLeadTimeMonthlyRepository.findById_CustomerNameIn(customerNames, sort);
    List<WhiskerBarStatsProjection> summarizeForMonth =
        indicatorCostRequestsLeadTimeMonthlyRepository.summarizeForMonth(customerNames);
    if (CollectionUtils.isNotEmpty(entities)) {
      SWYAxisWhisker swYAxis = new SWYAxisWhisker();
      swYAxis.setName("Days");
      List<SWYAxisWhiskerData> yAxisData = new ArrayList<>();

      yAxisData.add(
          new SWYAxisWhiskerData()
              .name(buildCostRequestLegendLabel(customerNames))
              .data(
                  summarizeForMonth.stream()
                      .map(this::convertLeadTimeIndicatorEntityToDoubleList)
                      .toList()));
      swYAxis.setDataList(yAxisData);
      return new SWGraphWhisker()
          .name("Monthly costing LT")
          .xAxis(
              new SWXAxis()
                  .name("Months")
                  .data(
                      entities.stream()
                          .map(entity -> buildMonthForAxis(entity.getId().getFirstDayOfMonth()))
                          .distinct()
                          .toList()))
          .yAxis(swYAxis);
    } else {
      return new SWGraphWhisker();
    }
  }

  public List<BigDecimal> convertLeadTimeIndicatorEntityToDoubleList(
      WhiskerBarStatsProjection data) {
    List<BigDecimal> list = new ArrayList<>();
    list.add(data.getMinimumLeadTime());
    list.add(data.getFirstQuartileLeadTime());
    list.add(data.getMedianLeadTime());
    list.add(data.getThirdQuartileLeadTime());
    list.add(data.getMaximumLeadTime());
    return list;
  }

  private String buildMonthForAxis(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM''yy");
    return date.format(formatter);
  }

  private String buildCostRequestLegendLabel(List<String> customerNames) {
    return customerNames.size() > 1 ? "Multi customer select" : String.join(", ", customerNames);
  }

  private List<String> getCustomerNames(List<UUID> customerIds) {
    if (CollectionUtils.isEmpty(customerIds)) {
      return Collections.singletonList(GlobalConstants.ALL_CUSTOMERS);
    }
    Sort sort = Sort.by(Sort.Direction.ASC, "name");
    return customerRepository.findAllByCustomerIdInAndArchivedFalse(customerIds, sort).stream()
        .map(CustomerEntity::getName)
        .toList();
  }

  @Transactional
  public SWGraph buildCostRequestsCountIndicatorAsGraph(SWIndicatorsBody filters) {
    return buildMonthlyCsrRequestsCountIndicatorAsGraph(filters);
  }

  private SWGraph buildMonthlyCsrRequestsCountIndicatorAsGraph(SWIndicatorsBody filters) {
    List<IndicatorCostRequestsCountMonthlyEntity> entities;
    Sort sort = Sort.by(Sort.Direction.ASC, "id.firstDayOfMonth");

    List<String> customerNames = getCustomerNames(filters.getCustomerIds());
    entities =
        indicatorCostRequestsCountMonthlyRepository.findById_CustomerNameIn(customerNames, sort);
    List<Long> sumQuantityByMonth =
        indicatorCostRequestsCountMonthlyRepository.sumQuantityByMonth(customerNames);
    List<BigDecimal> sumQuantityByMonthConverted =
        sumQuantityByMonth.stream().map(BigDecimal::valueOf).toList();
    if (CollectionUtils.isNotEmpty(entities)) {
      SWYAxis swYAxis = new SWYAxis();
      swYAxis.setName("Number of requests");
      List<SWYAxisData> yAxisData = new ArrayList<>();
      yAxisData.add(
          new SWYAxisData()
              .name(buildCostRequestLegendLabel(customerNames))
              .data(sumQuantityByMonthConverted.stream().map(e -> (e)).toList()));
      swYAxis.setDataList(yAxisData);
      return new SWGraph()
          .name("Monthly costing count")
          .xAxis(
              new SWXAxis()
                  .name("Months")
                  .data(
                      entities.stream()
                          .map(entity -> buildMonthForAxis(entity.getId().getFirstDayOfMonth()))
                          .distinct()
                          .toList()))
          .yAxis(swYAxis);
    } else {
      return new SWGraph();
    }
  }

  @Transactional
  public void recalculateMonthlyCostRequestsLeadTimeAndCountIndicator() {
    LocalDate now = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());
    int currentYear = appConfigurationProperties.getStartingYear();
    LocalDate currentFirstDayOfMonth = LocalDate.of(currentYear, 1, 1);
    OffsetDateTime startOfCurrentMonthInUTC =
        TimeUtils.convertZonedFirstTimeOfMonthToUTC(
            currentFirstDayOfMonth, appConfigurationProperties.getAppTimezone());
    OffsetDateTime endOfCurrentMonthInUTC =
        TimeUtils.convertZonedLastTimeOfMonthToUTC(
            currentFirstDayOfMonth, appConfigurationProperties.getAppTimezone());
    Sort sort = Sort.by(Sort.Direction.DESC, "creationDate");
    List<CostRequestEntity> costRequestEntities =
        new ArrayList<>(
            costRequestRepository.findByDateBetweenAndActive(
                startOfCurrentMonthInUTC, endOfCurrentMonthInUTC, sort));
    List<CostRequestImportLine> costRequestImportLines = new ArrayList<>();
    for (int i = 0; i < costRequestEntities.size(); i++) {
      var f = costRequestEntities.get(i);
      costRequestImportLines.add(
          new CostRequestImportLine(
              i, f.getCustomer().getName(), f.getCreationDate(), f.getActiveStatusDate()));
    }
    importCostRequestLines(costRequestImportLines);
  }

  private void importCostRequestLines(List<CostRequestImportLine> costRequests) {
    if (CollectionUtils.isNotEmpty(costRequests)) {

      OffsetDateTime minEstimatedOn =
          costRequests.stream()
              .map(CostRequestImportLine::getEstimatedOn)
              .filter(Objects::nonNull)
              .min(Comparator.naturalOrder())
              .get();
      OffsetDateTime maxEstimatedOn =
          costRequests.stream()
              .map(CostRequestImportLine::getEstimatedOn)
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder())
              .get();

      // ******** Manage count *************
      // Create a special entry for all customers
      createMonthlyCostRequestTypeEntries(
          GlobalConstants.ALL_CUSTOMERS,
          costRequests,
          YearMonth.from(minEstimatedOn),
          YearMonth.from(maxEstimatedOn));
      createMonthlyCostRequestsLeadTimeEntries(
          GlobalConstants.ALL_CUSTOMERS,
          costRequests,
          YearMonth.from(minEstimatedOn),
          YearMonth.from(maxEstimatedOn));

      Map<String, List<CostRequestImportLine>> allMapByCustomer =
          costRequests.stream()
              .filter(line -> StringUtils.isNotBlank(line.getCustomer()))
              .collect(Collectors.groupingBy(CostRequestImportLine::getCustomer));
      for (Map.Entry<String, List<CostRequestImportLine>> customerEntry :
          allMapByCustomer.entrySet()) {
        createMonthlyCostRequestTypeEntries(
            customerEntry.getKey(),
            customerEntry.getValue(),
            YearMonth.from(minEstimatedOn),
            YearMonth.from(maxEstimatedOn));
        createMonthlyCostRequestsLeadTimeEntries(
            customerEntry.getKey(),
            customerEntry.getValue(),
            YearMonth.from(minEstimatedOn),
            YearMonth.from(maxEstimatedOn));
      }
    }
  }

  public void createMonthlyCostRequestTypeEntries(
      String customer, List<CostRequestImportLine> lines, YearMonth start, YearMonth end) {

    // Extract YearMonth from `createdOn` and count occurrences
    Map<YearMonth, Long> groupedByMonth =
        lines.stream()
            .filter(csrRequest -> csrRequest.getEstimatedOn() != null)
            .collect(
                Collectors.groupingBy(
                    csrRequest -> YearMonth.from(csrRequest.getEstimatedOn()), // Group by YearMonth
                    Collectors.counting()));

    // Prepare a complete range of YearMonth keys with 0 where missing
    // Populate all months in the range with default value 0
    Map<YearMonth, Long> groupedByMonthWithoutMissingMonths =
        Stream.iterate(start, month -> month.plusMonths(1))
            .limit(start.until(end, ChronoUnit.MONTHS) + 1)
            .collect(
                Collectors.toMap(month -> month, month -> groupedByMonth.getOrDefault(month, 0L)));
    for (Map.Entry<YearMonth, Long> entry : groupedByMonthWithoutMissingMonths.entrySet()) {
      IndicatorCostRequestsCountMonthlyId id = new IndicatorCostRequestsCountMonthlyId();
      id.setFirstDayOfMonth(entry.getKey().atDay(1));
      id.setCustomerName(customer);
      IndicatorCostRequestsCountMonthlyEntity indicator =
          new IndicatorCostRequestsCountMonthlyEntity();
      indicator.setId(id);
      indicator.setQuantity(entry.getValue().intValue());
      indicatorCostRequestsCountMonthlyRepository.save(indicator);
    }
  }

  public void createMonthlyCostRequestsLeadTimeEntries(
      String customer, List<CostRequestImportLine> csrRequests, YearMonth start, YearMonth end) {

    // Extract YearMonth from `createdOn` and count occurrences
    Map<YearMonth, WhiskerBarStats> groupedByMonth =
        csrRequests.stream()
            .filter(csrRequest -> csrRequest.getEstimatedOn() != null)
            .collect(
                Collectors.groupingBy(
                    order -> YearMonth.from(order.getEstimatedOn()), // Group by YearMonth
                    Collectors.collectingAndThen(
                        Collectors.toList(), // Collect the grouped elements into a list
                        this::calculateCostWhiskerBarStats // Convert the list to WhiskerBarStats
                        )));

    // Prepare a complete range of YearMonth keys with 0 where missing
    // Populate all months in the range with default value 0
    Map<YearMonth, WhiskerBarStats> groupedByMonthWithoutMissingMonths =
        Stream.iterate(start, month -> month.plusMonths(1))
            .limit(start.until(end, ChronoUnit.MONTHS) + 1)
            .collect(
                Collectors.toMap(
                    month -> month,
                    month ->
                        groupedByMonth.getOrDefault(
                            month,
                            new WhiskerBarStats(
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO))));
    for (Map.Entry<YearMonth, WhiskerBarStats> entry :
        groupedByMonthWithoutMissingMonths.entrySet()) {
      IndicatorCostRequestsLeadTimeMonthlyId id = new IndicatorCostRequestsLeadTimeMonthlyId();
      id.setFirstDayOfMonth(entry.getKey().atDay(1));
      id.setCustomerName(customer);
      IndicatorCostRequestsLeadTimeMonthlyEntity indicator =
          new IndicatorCostRequestsLeadTimeMonthlyEntity();
      indicator.setId(id);
      indicator.setMinimumLeadTime(entry.getValue().getMinimumLeadTime());
      indicator.setFirstQuartileLeadTime(entry.getValue().getFirstQuartileLeadTime());
      indicator.setMedianLeadTime(entry.getValue().getMedianLeadTime());
      indicator.setThirdQuartileLeadTime(entry.getValue().getThirdQuartileLeadTime());
      indicator.setMaximumLeadTime(entry.getValue().getMaximumLeadTime());
      indicator.setMeanLeadTime(entry.getValue().getMeanLeadTime());
      indicatorCostRequestsLeadTimeMonthlyRepository.save(indicator);
    }
  }

  private WhiskerBarStats calculateCostWhiskerBarStats(List<CostRequestImportLine> list) {
    return extractWhiskerBarStats(
        list.stream()
            .map(
                e -> {
                  long seconds = Duration.between(e.getCreatedOn(), e.getEstimatedOn()).toSeconds();

                  return BigDecimal.valueOf(seconds)
                      .divide(BigDecimal.valueOf(86400), 6, RoundingMode.HALF_UP);
                })
            .sorted()
            .toList());
  }

  private WhiskerBarStats extractWhiskerBarStats(List<BigDecimal> allLeadTimesInDays) {
    BigDecimal minimumLeadTime;
    BigDecimal firstQuartileLeadTime;
    BigDecimal medianLeadTime;
    BigDecimal thirdQuartileLeadTime;
    BigDecimal maximumLeadTime;
    BigDecimal meanLeadTime;
    if (CollectionUtils.isNotEmpty(allLeadTimesInDays)) {
      int size = allLeadTimesInDays.size();
      minimumLeadTime = allLeadTimesInDays.getFirst();
      maximumLeadTime = allLeadTimesInDays.getLast();
      BigDecimal sum = BigDecimal.ZERO;
      for (BigDecimal timeInDays : allLeadTimesInDays) {
        sum = sum.add(timeInDays);
      }
      meanLeadTime = sum.divide(BigDecimal.valueOf(size), RoundingMode.HALF_UP);

      if (size == 1) {
        firstQuartileLeadTime = minimumLeadTime;
        medianLeadTime = minimumLeadTime;
        thirdQuartileLeadTime = minimumLeadTime;
      } else if (size == 2 || size == 3) {
        firstQuartileLeadTime = minimumLeadTime;
        thirdQuartileLeadTime = maximumLeadTime;
        medianLeadTime = allLeadTimesInDays.get(size / 2);
      } else {
        firstQuartileLeadTime = allLeadTimesInDays.get(size / 4);
        thirdQuartileLeadTime = allLeadTimesInDays.get(3 * size / 4);
        if (size % 2 == 0) {
          medianLeadTime =
              allLeadTimesInDays
                  .get((size / 2) - 1)
                  .add(allLeadTimesInDays.get(size / 2))
                  .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        } else {
          medianLeadTime = allLeadTimesInDays.get(size / 2);
        }
      }
      return new WhiskerBarStats(
          minimumLeadTime,
          firstQuartileLeadTime,
          medianLeadTime,
          thirdQuartileLeadTime,
          maximumLeadTime,
          meanLeadTime);
    } else {
      return new WhiskerBarStats(
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO);
    }
  }
}
