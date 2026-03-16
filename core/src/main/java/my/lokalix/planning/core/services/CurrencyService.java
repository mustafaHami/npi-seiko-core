package my.lokalix.planning.core.services;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.mappers.CurrencyMapper;
import my.lokalix.planning.core.mappers.ExchangeRateMapper;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateEntity;
import my.lokalix.planning.core.models.entities.admin.ExchangeRateHistoryEntity;
import my.lokalix.planning.core.repositories.CurrencyRepository;
import my.lokalix.planning.core.repositories.ExchangeRateHistoryRepository;
import my.lokalix.planning.core.services.helper.EntityRetrievalHelper;
import my.lokalix.planning.core.services.validator.CurrencyValidator;
import my.lokalix.planning.core.utils.ExcelUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Service
public class CurrencyService {

  private final CurrencyMapper currencyMapper;
  private final ExchangeRateMapper exchangeRateMapper;
  private final CurrencyRepository currencyRepository;
  private final ExchangeRateHistoryRepository exchangeRateHistoryRepository;
  private final EntityRetrievalHelper entityRetrievalHelper;
  private final CurrencyValidator currencyValidator;

  @Transactional
  public SWCurrency createCurrency(SWCurrencyCreate body) {
    if (currencyRepository.existsByCodeAndArchivedFalse(body.getCode())) {
      throw new EntityExistsException(
          "A currency with the same code '" + body.getCode() + "' already exists");
    }

    CurrencyEntity currencyEntity = currencyMapper.toCurrencyEntity(body);
    CurrencyEntity savedCurrency = currencyRepository.save(currencyEntity);

    // Create exchange rates
    if (CollectionUtils.isNotEmpty(body.getExchangeRates())) {
      for (SWExchangeRateCreate exchangeRateDto : body.getExchangeRates()) {
        // Find toCurrency by code using EntityRetrievalHelper
        CurrencyEntity toCurrency =
            entityRetrievalHelper.getMustExistCurrencyById(exchangeRateDto.getToCurrencyId());

        ExchangeRateEntity exchangeRateEntity = new ExchangeRateEntity();
        exchangeRateEntity.setFromCurrency(savedCurrency);
        exchangeRateEntity.setToCurrency(toCurrency);
        exchangeRateEntity.setRate(exchangeRateDto.getRate());
        savedCurrency.getExchangeRates().add(exchangeRateEntity);
      }
      // Save currency with cascade to persist exchange rates
      currencyRepository.save(savedCurrency);

      // Create or update inverse exchange rates for bidirectional conversion
      createOrUpdateInverseExchangeRates(savedCurrency);
    }

    return currencyMapper.toSWCurrency(
        entityRetrievalHelper.getMustExistCurrencyById(savedCurrency.getCurrencyId()));
  }

  @Transactional
  public SWCurrency retrieveCurrency(UUID uid) {
    CurrencyEntity entity = entityRetrievalHelper.getMustExistCurrencyById(uid);
    return currencyMapper.toSWCurrency(entity);
  }

  @Transactional
  public SWCurrency updateCurrency(UUID uid, SWCurrencyUpdate body) {
    CurrencyEntity entity = entityRetrievalHelper.getMustExistCurrencyById(uid);
    if (currencyRepository.existsByCodeAndCurrencyIdNotAndArchivedFalse(body.getCode(), uid)) {
      throw new EntityExistsException(
          "A currency with the same code '" + body.getCode() + "' already exists");
    }
    currencyMapper.updateCurrencyEntityFromDto(body, entity);

    // Build map of existing rates by toCurrencyId for quick lookup
    Map<UUID, ExchangeRateEntity> existingRatesMap =
        entity.getExchangeRates().stream()
            .collect(
                Collectors.toMap(
                    rate -> rate.getToCurrency().getCurrencyId(), rate -> rate, (a, b) -> a));

    // Track rates to delete
    Set<UUID> ratesToKeep = new HashSet<>();

    // Update or create new rates
    if (CollectionUtils.isNotEmpty(body.getExchangeRates())) {
      for (SWExchangeRateUpdate exchangeRateDto : body.getExchangeRates()) {
        UUID toCurrencyId = exchangeRateDto.getToCurrencyId();
        CurrencyEntity toCurrency = entityRetrievalHelper.getMustExistCurrencyById(toCurrencyId);
        BigDecimal newRate = exchangeRateDto.getRate();

        ratesToKeep.add(toCurrencyId);

        ExchangeRateEntity existingRate = existingRatesMap.get(toCurrencyId);

        if (existingRate != null) {
          // Update existing rate if changed
          if (existingRate.getRate().compareTo(newRate) != 0) {
            BigDecimal oldRate = existingRate.getRate();
            existingRate.setRate(newRate);

            // Track history for this currency (source -> target)
            trackExchangeRateHistory(entity, toCurrency, oldRate, newRate);

            // Update inverse rate and track history for target currency
            updateInverseRateWithHistory(entity, toCurrency, newRate);
          }
        } else {
          // Create new rate
          ExchangeRateEntity newRateEntity = new ExchangeRateEntity();
          newRateEntity.setFromCurrency(entity);
          newRateEntity.setToCurrency(toCurrency);
          newRateEntity.setRate(newRate);
          entity.getExchangeRates().add(newRateEntity);

          // Create inverse rate
          createInverseRate(entity, toCurrency, newRate);
        }
      }
    }

    // Remove rates that are no longer in the update
    List<ExchangeRateEntity> ratesToRemove =
        entity.getExchangeRates().stream()
            .filter(rate -> !ratesToKeep.contains(rate.getToCurrency().getCurrencyId()))
            .toList();

    for (ExchangeRateEntity rateToRemove : ratesToRemove) {
      // Remove inverse rate
      removeInverseRate(entity, rateToRemove.getToCurrency());
    }
    entity.getExchangeRates().removeAll(ratesToRemove);

    // Save currency with cascade to persist/delete exchange rates
    CurrencyEntity savedEntity = currencyRepository.save(entity);

    return currencyMapper.toSWCurrency(savedEntity);
  }

  @Transactional
  public SWCurrency archiveCurrency(UUID uid) {
    CurrencyEntity entity = entityRetrievalHelper.getMustExistCurrencyById(uid);
    currencyValidator.validateNotInUse(entity);
    entity.setArchived(true);
    entity.setArchivedAt(TimeUtils.nowOffsetDateTimeUTC());
    return currencyMapper.toSWCurrency(currencyRepository.save(entity));
  }

  @Transactional
  public List<SWCurrency> listCurrencies() {
    Sort sort = Sort.by(Sort.Direction.ASC, "code");
    List<CurrencyEntity> allCurrencies = currencyRepository.findAllByArchivedFalse(sort);
    return currencyMapper.toListSwCurrency(allCurrencies);
  }

  @Transactional
  public SWCurrenciesPaginated searchCurrencies(int offset, int limit, SWBasicSearch search) {
    Sort sort = Sort.by(Sort.Direction.ASC, "code");
    Pageable pageable = PageRequest.of(offset / limit, limit, sort);
    Page<CurrencyEntity> paginatedCurrencies;

    if (StringUtils.isBlank(search.getSearchText())) {
      paginatedCurrencies = currencyRepository.findByArchivedFalse(pageable);
    } else {
      paginatedCurrencies =
          currencyRepository.findBySearchAndArchivedFalse(pageable, search.getSearchText());
    }

    return populateCurrenciesPaginatedResults(paginatedCurrencies);
  }

  private SWCurrenciesPaginated populateCurrenciesPaginatedResults(
      Page<CurrencyEntity> paginatedCurrencies) {
    SWCurrenciesPaginated currenciesPaginated = new SWCurrenciesPaginated();
    currenciesPaginated.setResults(
        currencyMapper.toListSwCurrency(paginatedCurrencies.getContent()));
    currenciesPaginated.setPage(paginatedCurrencies.getNumber());
    currenciesPaginated.setPerPage(paginatedCurrencies.getSize());
    currenciesPaginated.setTotal((int) paginatedCurrencies.getTotalElements());
    currenciesPaginated.setHasPrev(paginatedCurrencies.hasPrevious());
    currenciesPaginated.setHasNext(paginatedCurrencies.hasNext());
    return currenciesPaginated;
  }

  /**
   * Creates or updates inverse exchange rates for bidirectional currency conversion. For each
   * exchange rate (source → target with rate R), this method ensures that the inverse rate (target
   * → source with rate 1/R) exists and is up to date.
   *
   * @param sourceCurrency the currency whose exchange rates should have inverse rates created or
   *     updated
   */
  private void createOrUpdateInverseExchangeRates(CurrencyEntity sourceCurrency) {
    // Reload to ensure we have the latest data with exchange rates
    CurrencyEntity reloadedSource =
        entityRetrievalHelper.getMustExistCurrencyById(sourceCurrency.getCurrencyId());

    for (ExchangeRateEntity rate : reloadedSource.getExchangeRates()) {
      // Get target currency with its exchange rates
      CurrencyEntity targetCurrency =
          entityRetrievalHelper.getMustExistCurrencyById(rate.getToCurrency().getCurrencyId());

      // Force lazy loading of exchange rates
      List<ExchangeRateEntity> targetRates = targetCurrency.getExchangeRates();

      // Calculate inverse rate (1/rate with 6 decimal precision)
      BigDecimal inverseRateValue = BigDecimal.ONE.divide(rate.getRate(), 6, RoundingMode.HALF_UP);

      // Find if inverse rate already exists
      Optional<ExchangeRateEntity> existingInverseRate =
          targetRates.stream()
              .filter(r -> r.getToCurrency().getCurrencyId().equals(reloadedSource.getCurrencyId()))
              .findFirst();

      if (existingInverseRate.isPresent()) {
        // Update existing inverse rate
        existingInverseRate.get().setRate(inverseRateValue);
      } else {
        // Create new inverse rate
        ExchangeRateEntity newInverseRate = new ExchangeRateEntity();
        newInverseRate.setFromCurrency(targetCurrency);
        newInverseRate.setToCurrency(reloadedSource);
        newInverseRate.setRate(inverseRateValue);
        targetRates.add(newInverseRate);
      }

      // Save target currency to persist changes
      currencyRepository.save(targetCurrency);
    }
  }

  /**
   * Removes inverse exchange rates when the original rates are being deleted. For each rate to
   * remove (source → target), this method finds and removes the inverse rate (target → source).
   *
   * @param sourceCurrency the currency whose rates are being removed
   * @param ratesToRemove the list of exchange rates being removed
   */
  private void removeInverseExchangeRates(
      CurrencyEntity sourceCurrency, List<ExchangeRateEntity> ratesToRemove) {
    for (ExchangeRateEntity oldRate : ratesToRemove) {
      // Get target currency with its exchange rates
      CurrencyEntity targetCurrency =
          entityRetrievalHelper.getMustExistCurrencyById(oldRate.getToCurrency().getCurrencyId());

      // Force lazy loading of exchange rates
      List<ExchangeRateEntity> targetRates = targetCurrency.getExchangeRates();

      // Remove inverse rate if exists
      boolean removed =
          targetRates.removeIf(
              r -> r.getToCurrency().getCurrencyId().equals(sourceCurrency.getCurrencyId()));

      // Save target currency if any rate was removed
      if (removed) {
        currencyRepository.save(targetCurrency);
      }
    }
  }

  /**
   * Tracks exchange rate history when a rate changes.
   *
   * @param fromCurrency the source currency
   * @param toCurrency the target currency
   * @param oldRate the old rate value
   * @param newRate the new rate value
   */
  private void trackExchangeRateHistory(
      CurrencyEntity fromCurrency,
      CurrencyEntity toCurrency,
      BigDecimal oldRate,
      BigDecimal newRate) {
    ExchangeRateHistoryEntity history = new ExchangeRateHistoryEntity();
    history.setFromCurrency(fromCurrency.getCode());
    history.setToCurrency(toCurrency.getCode());
    history.setOldRate(oldRate);
    history.setNewRate(newRate);
    exchangeRateHistoryRepository.save(history);
  }

  /**
   * Updates the inverse rate and tracks history for the target currency.
   *
   * @param sourceCurrency the source currency
   * @param targetCurrency the target currency
   * @param newDirectRate the new direct rate
   */
  private void updateInverseRateWithHistory(
      CurrencyEntity sourceCurrency, CurrencyEntity targetCurrency, BigDecimal newDirectRate) {
    CurrencyEntity reloadedTarget =
        entityRetrievalHelper.getMustExistCurrencyById(targetCurrency.getCurrencyId());

    // Calculate new inverse rates
    BigDecimal newInverseRate = BigDecimal.ONE.divide(newDirectRate, 6, RoundingMode.HALF_UP);

    // Find and update inverse rate
    Optional<ExchangeRateEntity> inverseRateOpt =
        reloadedTarget.getExchangeRates().stream()
            .filter(r -> r.getToCurrency().getCurrencyId().equals(sourceCurrency.getCurrencyId()))
            .findFirst();

    if (inverseRateOpt.isPresent()) {
      BigDecimal oldInverseRate = inverseRateOpt.get().getRate();
      inverseRateOpt.get().setRate(newInverseRate);

      // Track history for target currency (target -> source)
      trackExchangeRateHistory(reloadedTarget, sourceCurrency, oldInverseRate, newInverseRate);

      currencyRepository.save(reloadedTarget);
    }
  }

  /**
   * Creates a new inverse rate for bidirectional conversion.
   *
   * @param sourceCurrency the source currency
   * @param targetCurrency the target currency
   * @param directRate the direct rate value
   */
  private void createInverseRate(
      CurrencyEntity sourceCurrency, CurrencyEntity targetCurrency, BigDecimal directRate) {
    CurrencyEntity reloadedTarget =
        entityRetrievalHelper.getMustExistCurrencyById(targetCurrency.getCurrencyId());

    BigDecimal inverseRate = BigDecimal.ONE.divide(directRate, 6, RoundingMode.HALF_UP);

    ExchangeRateEntity newInverseRate = new ExchangeRateEntity();
    newInverseRate.setFromCurrency(reloadedTarget);
    newInverseRate.setToCurrency(sourceCurrency);
    newInverseRate.setRate(inverseRate);
    reloadedTarget.getExchangeRates().add(newInverseRate);

    currencyRepository.save(reloadedTarget);
  }

  /**
   * Removes the inverse rate from the target currency.
   *
   * @param sourceCurrency the source currency
   * @param targetCurrency the target currency
   */
  private void removeInverseRate(CurrencyEntity sourceCurrency, CurrencyEntity targetCurrency) {
    CurrencyEntity reloadedTarget =
        entityRetrievalHelper.getMustExistCurrencyById(targetCurrency.getCurrencyId());

    boolean removed =
        reloadedTarget
            .getExchangeRates()
            .removeIf(
                r -> r.getToCurrency().getCurrencyId().equals(sourceCurrency.getCurrencyId()));

    if (removed) {
      currencyRepository.save(reloadedTarget);
    }
  }

  /**
   * Retrieves the exchange rate history for a specific currency.
   *
   * @param currencyId the currency ID
   * @return list of exchange rate history
   */
  @Transactional
  public List<SWExchangeRateHistory> getCurrencyHistory(UUID currencyId) {
    // Verify currency exists
    CurrencyEntity fromCurrency = entityRetrievalHelper.getMustExistCurrencyById(currencyId);

    List<ExchangeRateHistoryEntity> history =
        exchangeRateHistoryRepository.findByFromCurrencyOrderByChangeDateDesc(
            fromCurrency.getCode());

    return exchangeRateMapper.toListSWExchangeRateHistory(history);
  }

  @Transactional
  public int uploadCurrenciesFromExcel(MultipartFile file) throws IOException {
    List<CurrencyEntity> currenciesToCreate = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      log.info("Processing currencies sheet: {}", sheet.getSheetName());

      // Process each row starting from row 1 (index 1)
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
          continue;
        }

        // Get code from first column (index 0)
        String code = ExcelUtils.loadStringCell(row.getCell(0));

        // Skip if code is blank
        if (StringUtils.isBlank(code)) {
          continue;
        }

        // Check if currency already exists by code
        if (currencyRepository.existsByCodeAndArchivedFalse(code)) {
          log.info("Currency '{}' already exists, skipping", code);
          continue;
        }

        // Create new currency
        CurrencyEntity currency = new CurrencyEntity();
        currency.setCode(code);
        currenciesToCreate.add(currency);
        log.info("Prepared currency '{}'", code);
      }

      // Save all currencies in batch
      if (!currenciesToCreate.isEmpty()) {
        currencyRepository.saveAll(currenciesToCreate);
        log.info("Successfully created {} currencies from Excel file", currenciesToCreate.size());
      } else {
        log.info("No new currencies to create from Excel file");
      }
    }

    return currenciesToCreate.size();
  }

  public SWCurrency retrieveCurrencySummaryByCode(String targetCurrencyCode) {
    Optional<CurrencyEntity> optCurrency =
        currencyRepository.findByCodeAndArchivedFalse(targetCurrencyCode);
    if (optCurrency.isPresent()) {
      return currencyMapper.toSWCurrencySummary(optCurrency.get());
    } else {
      throw new EntityNotFoundException(
          "System currency is set to "
              + targetCurrencyCode
              + ", but no currency found for this code");
    }
  }
}
