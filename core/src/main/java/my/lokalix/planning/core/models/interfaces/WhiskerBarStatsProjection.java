package my.lokalix.planning.core.models.interfaces;

import java.math.BigDecimal;

public interface WhiskerBarStatsProjection {
  BigDecimal getMinimumLeadTime();

  BigDecimal getFirstQuartileLeadTime();

  BigDecimal getMedianLeadTime();

  BigDecimal getThirdQuartileLeadTime();

  BigDecimal getMaximumLeadTime();

  BigDecimal getMeanLeadTime();
}
