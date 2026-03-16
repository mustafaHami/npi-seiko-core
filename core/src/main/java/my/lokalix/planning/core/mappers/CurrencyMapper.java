package my.lokalix.planning.core.mappers;

import jakarta.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {ExchangeRateMapper.class})
public abstract class CurrencyMapper {

  @Resource private AppConfigurationProperties appConfigurationProperties;

  @Mapping(target = "exchangeRates", ignore = true)
  public abstract CurrencyEntity toCurrencyEntity(SWCurrencyCreate dto);

  @Named("currencyFullList")
  public List<SWCurrency> toListSwCurrency(List<CurrencyEntity> entities) {
    if (CollectionUtils.isNotEmpty(appConfigurationProperties.getCurrencyListOrdering())) {
      Map<String, Integer> orderIndex =
          IntStream.range(0, appConfigurationProperties.getCurrencyListOrdering().size())
              .boxed()
              .collect(
                  Collectors.toMap(
                      i -> appConfigurationProperties.getCurrencyListOrdering().get(i), i -> i));

      return entities.stream()
          .map(this::toSWCurrency)
          .sorted(
              Comparator.comparingInt(c -> orderIndex.getOrDefault(c.getCode(), Integer.MAX_VALUE)))
          .toList();
    } else {
      return entities.stream().map(this::toSWCurrency).toList();
    }
  }

  @Named("currencyFull")
  @Mapping(source = "currencyId", target = "uid")
  @Mapping(source = "exchangeRates", target = "exchangeRates")
  public abstract SWCurrency toSWCurrency(CurrencyEntity entity);

  @Named("currencySummaryList")
  public List<SWCurrency> toListSWCurrencySummary(List<CurrencyEntity> entities) {
    if (CollectionUtils.isNotEmpty(appConfigurationProperties.getCurrencyListOrdering())) {
      Map<String, Integer> orderIndex =
          IntStream.range(0, appConfigurationProperties.getCurrencyListOrdering().size())
              .boxed()
              .collect(
                  Collectors.toMap(
                      i -> appConfigurationProperties.getCurrencyListOrdering().get(i), i -> i));

      return entities.stream()
          .map(this::toSWCurrencySummary)
          .sorted(
              Comparator.comparingInt(c -> orderIndex.getOrDefault(c.getCode(), Integer.MAX_VALUE)))
          .toList();
    } else {
      return entities.stream().map(this::toSWCurrencySummary).toList();
    }
  }

  @Named("currencySummarySet")
  public List<SWCurrency> toSetSWCurrencySummary(Set<CurrencyEntity> entities) {
    if (CollectionUtils.isNotEmpty(appConfigurationProperties.getCurrencyListOrdering())) {
      Map<String, Integer> orderIndex =
          IntStream.range(0, appConfigurationProperties.getCurrencyListOrdering().size())
              .boxed()
              .collect(
                  Collectors.toMap(
                      i -> appConfigurationProperties.getCurrencyListOrdering().get(i), i -> i));

      return entities.stream()
          .map(this::toSWCurrencySummary)
          .sorted(
              Comparator.comparingInt(c -> orderIndex.getOrDefault(c.getCode(), Integer.MAX_VALUE)))
          .toList();
    } else {
      return entities.stream().map(this::toSWCurrencySummary).toList();
    }
  }

  @Named("currencySummary")
  @Mapping(source = "currencyId", target = "uid")
  @Mapping(target = "exchangeRates", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  public abstract SWCurrency toSWCurrencySummary(CurrencyEntity entity);

  @Mapping(target = "exchangeRates", ignore = true)
  public abstract void updateCurrencyEntityFromDto(
      SWCurrencyUpdate dto, @MappingTarget CurrencyEntity entity);
}
