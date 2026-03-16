package my.lokalix.planning.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

@Data
public class ExternalExchangeRateResponse {
  private String result;
  @JsonProperty("base_code")
  private String baseCode;
  private Map<String, BigDecimal> rates;
}
