package my.lokalix.planning.core.services.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.services.helper.NpiOrderHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NpiForecastDeliveryDateCron {

  private final NpiOrderHelper npiOrderHelper;

  @Scheduled(cron = "0 0 0 * * ?", zone = "${app-timezone:Asia/Singapore}")
  public void recalculateForecastDeliveryDates() {
    int updatedNpiCount = npiOrderHelper.recalculateForecastDeliveryDateForAllActiveNpiOrders();
    log.info("Recalculated forecast delivery dates for {} NPI order(s)", updatedNpiCount);
  }
}
