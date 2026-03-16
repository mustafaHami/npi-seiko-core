package my.lokalix.planning.core.models.indicator;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class WhiskerBarStats {
  final BigDecimal minimumLeadTime;
  final BigDecimal firstQuartileLeadTime;
  final BigDecimal medianLeadTime;
  final BigDecimal thirdQuartileLeadTime;
  final BigDecimal maximumLeadTime;
  final BigDecimal meanLeadTime;
}
