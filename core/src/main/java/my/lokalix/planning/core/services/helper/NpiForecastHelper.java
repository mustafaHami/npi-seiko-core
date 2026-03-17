package my.lokalix.planning.core.services.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.repositories.NpiOrderRepository;
import my.lokalix.planning.core.repositories.ProcessLineRepository;
import my.lokalix.planning.core.utils.TimeUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NpiForecastHelper {

  private final NpiOrderRepository npiOrderRepository;
  private final ProcessLineRepository processLineRepository;
  private final AppConfigurationProperties appConfigurationProperties;

  public void recalculateForecastDeliveryDate(NpiOrderEntity npiOrder) {
    List<ProcessLineEntity> allLines =
        processLineRepository.findAllByNpiOrderOrderByIndexIdAsc(npiOrder);

    LocalDate today = TimeUtils.nowLocalDate(appConfigurationProperties.getAppTimezone());
    final BigDecimal hoursPerDay = BigDecimal.valueOf(24.0);

    BigDecimal totalForecastHours = BigDecimal.valueOf(0);
    for (ProcessLineEntity line : allLines) {
      ProcessLineStatus status = line.getStatus();
      BigDecimal remainingTimeInHours = line.getRemainingTimeInHours();
      BigDecimal planTimeInHours = line.getPlanTimeInHours();

      if (status == ProcessLineStatus.IN_PROGRESS) {
        if (planTimeInHours != null) {
          totalForecastHours = totalForecastHours.add(planTimeInHours);
        }
        if (remainingTimeInHours != null) {
          totalForecastHours = totalForecastHours.add(remainingTimeInHours);
        }
      } else if (status == ProcessLineStatus.NOT_STARTED && planTimeInHours != null) {
        totalForecastHours = totalForecastHours.add(planTimeInHours);
      }
    }
    long forecastDays = totalForecastHours.divide(hoursPerDay, 0, RoundingMode.CEILING).longValue();
    npiOrder.setForecastDeliveryDate(today.plusDays(forecastDays));
    npiOrderRepository.save(npiOrder);
  }

  public int recalculateForecastDeliveryDateForAllActiveNpiOrders() {
    List<NpiOrderEntity> npiOrders = npiOrderRepository.findAllByArchivedFalse();
    for (NpiOrderEntity npiOrder : npiOrders) {
      recalculateForecastDeliveryDate(npiOrder);
    }
    return npiOrders.size();
  }
}
