package my.lokalix.planning.core.services;

import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.ExternalExchangeRateResponse;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateHistoryEntity;
import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;
import my.lokalix.planning.core.models.enums.CurrencyExchangeRateStrategy;
import my.lokalix.planning.core.repositories.CurrencyRepository;
import my.lokalix.planning.core.repositories.ExchangeRateHistoryRepository;
import my.lokalix.planning.core.repositories.ExchangeRateRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Service
public class ExternalExchangeRateService {
  private WebClient webClient;
  @Resource private AppConfigurationProperties appConfigurationProperties;
  @Resource private CurrencyRepository currencyRepository;
  @Resource private ExchangeRateRepository exchangeRateRepository;
  @Resource private ExchangeRateHistoryRepository exchangeRateHistoryRepository;
  @Resource private EntityRetrievalHelper entityRetrievalHelper;

  @PostConstruct
  private void init() {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(Duration.ofSeconds(10));
    webClient =
        WebClient.builder()
            .baseUrl(appConfigurationProperties.getExternalExchangeRateUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
  }

  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Singapore")
  @Transactional
  public void fetchDailyRates() {
    GlobalConfigEntity globalConfigEntity = entityRetrievalHelper.getMustExistGlobalConfig();
    if (!globalConfigEntity
        .getCurrencyExchangeRateStrategy()
        .equals(CurrencyExchangeRateStrategy.AUTOMATICALLY_UPDATED)) {
      return;
    }

    List<CurrencyEntity> currencies = currencyRepository.findAllByArchivedFalse(Sort.unsorted());
    if (CollectionUtils.isEmpty(currencies)) {
      return;
    }

    ExternalExchangeRateResponse response =
        getLatestRates(appConfigurationProperties.getTargetCurrencyCode());
    if (response == null || response.getRates() == null) {
      log.warn("Failed to fetch exchange rates from external API");
      return;
    }

    Map<String, BigDecimal> rates = response.getRates();

    for (CurrencyEntity fromCurrency : currencies) {
      BigDecimal fromRate = rates.get(fromCurrency.getCode());
      if (fromRate == null || fromRate.compareTo(BigDecimal.ZERO) == 0) {
        log.warn("No rate found for currency: {}", fromCurrency.getCode());
        continue;
      }

      for (CurrencyEntity toCurrency : currencies) {
        if (fromCurrency.equals(toCurrency)) {
          continue;
        }
        BigDecimal toRate = rates.get(toCurrency.getCode());
        if (toRate == null) {
          log.warn("No rate found for currency: {}", toCurrency.getCode());
          continue;
        }

        // Derive cross-rate: rate(from→to) = rates[to] / rates[from]
        BigDecimal crossRate = toRate.divide(fromRate, 6, RoundingMode.HALF_UP);

        Optional<ExchangeRateEntity> existingRate =
            exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);

        if (existingRate.isPresent()) {
          ExchangeRateEntity exchangeRate = existingRate.get();
          BigDecimal oldRate = exchangeRate.getRate();

          if (oldRate.compareTo(crossRate) != 0) {
            ExchangeRateHistoryEntity history = new ExchangeRateHistoryEntity();
            history.setFromCurrency(fromCurrency.getCode());
            history.setToCurrency(toCurrency.getCode());
            history.setOldRate(oldRate);
            history.setNewRate(crossRate);
            exchangeRateHistoryRepository.save(history);

            log.info(
                "Exchange rate changed: {} -> {} from {} to {}",
                fromCurrency.getCode(),
                toCurrency.getCode(),
                oldRate,
                crossRate);
          }

          exchangeRate.setRate(crossRate);
          exchangeRateRepository.save(exchangeRate);
        } else {
          ExchangeRateEntity newRate = new ExchangeRateEntity();
          newRate.setFromCurrency(fromCurrency);
          newRate.setToCurrency(toCurrency);
          newRate.setRate(crossRate);
          exchangeRateRepository.save(newRate);
          log.info(
              "Created exchange rate: {} -> {} = {}",
              fromCurrency.getCode(),
              toCurrency.getCode(),
              crossRate);
        }
      }
    }
  }

  public ExternalExchangeRateResponse getLatestRates(String baseCurrency) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/latest/{base}").build(baseCurrency))
        .retrieve()
        .bodyToMono(ExternalExchangeRateResponse.class)
        .block(); // OK for sync / scheduled jobs
  }
}
