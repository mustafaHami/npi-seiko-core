package my.lokalix.planning.core.services.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.services.helper.NpiForecastHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NpiForecastDeliveryDateCron {

  private final NpiForecastHelper npiForecastHelper;

  @Scheduled(cron = "0 0 0 * * ?", zone = "${app-timezone:Asia/Singapore}")
  public void recalculateForecastDeliveryDates() {
    int updatedNpiCount = npiForecastHelper.recalculateForecastDeliveryDateForAllActiveNpiOrders();
    log.info("Recalculated forecast delivery dates for {} NPI order(s)", updatedNpiCount);
  }
}
