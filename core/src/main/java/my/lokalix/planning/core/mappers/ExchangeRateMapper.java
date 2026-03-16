package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateHistoryEntity;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {CurrencyMapper.class})
public abstract class ExchangeRateMapper {

  @Mapping(source = "toCurrency.code", target = "toCurrencyCode")
  public abstract SWExchangeRate toSWExchangeRate(ExchangeRateEntity entity);

  public abstract List<SWExchangeRate> toListSWExchangeRate(List<ExchangeRateEntity> entities);

  @Mapping(source = "toCurrency", target = "toCurrencyCode")
  @Mapping(source = "oldRate", target = "oldRate")
  @Mapping(source = "newRate", target = "newRate")
  @Mapping(source = "changeDate", target = "changeDate")
  public abstract SWExchangeRateHistory toSWExchangeRateHistory(ExchangeRateHistoryEntity entity);

  public abstract List<SWExchangeRateHistory> toListSWExchangeRateHistory(
      List<ExchangeRateHistoryEntity> entities);
}
