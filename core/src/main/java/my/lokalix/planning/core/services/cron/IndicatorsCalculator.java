package my.lokalix.planning.core.services.cron;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.services.indicator.IndicatorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class IndicatorsCalculator {

  private final IndicatorService indicatorService;

  @Transactional
  @Scheduled(cron = "0 10 1 * * ?", zone = "Asia/Singapore")
  public void recalculateMonthlyCostRequestsLeadTimeAndCountIndicator() {
    log.info("Recalculating monthly lead time & count indicators...");
    indicatorService.recalculateMonthlyCostRequestsLeadTimeAndCountIndicator();
    log.info("Recalculated monthly lead time & count indicators...");
  }
}
