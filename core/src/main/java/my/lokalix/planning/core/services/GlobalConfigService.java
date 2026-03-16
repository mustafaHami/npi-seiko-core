package my.lokalix.planning.core.services;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.mappers.GlobalConfigMapper;
import my.lokalix.planning.core.models.entities.CostRequestLineEntity;
import my.lokalix.planning.core.models.entities.OtherCostLineEntity;
import my.lokalix.planning.core.models.entities.OtherCostLinePerCostRequestQuantityEntity;
import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;
import my.lokalix.planning.core.models.enums.AutomaticExchangeRateFrequency;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.CurrencyExchangeRateStrategy;
import my.lokalix.planning.core.models.enums.MarkupApprovalStrategy;
import my.lokalix.planning.core.repositories.CostRequestLineRepository;
import my.lokalix.planning.core.repositories.GlobalConfigRepository;
import my.lokalix.planning.core.utils.GlobalConstants;
import my.zkonsulting.planning.generated.model.SWCurrency;
import my.zkonsulting.planning.generated.model.SWGlobalConfig;
import my.zkonsulting.planning.generated.model.SWGlobalConfigPatch;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class GlobalConfigService {

  private final GlobalConfigMapper globalConfigMapper;
  private final GlobalConfigRepository globalConfigRepository;
  private final AppConfigurationProperties appConfigurationProperties;
  private final CurrencyService currencyService;
  private final CostRequestLineRepository costRequestLineRepository;

  @Transactional
  public SWGlobalConfig getGlobalConfig() {
    GlobalConfigEntity entity =
        globalConfigRepository.findAll().stream()
            .findFirst()
            .orElseGet(() -> globalConfigRepository.save(buildDefaultGlobalConfig()));
    return globalConfigMapper.toSWGlobalConfig(entity);
  }

  @Transactional
  public SWGlobalConfig patchGlobalConfig(SWGlobalConfigPatch body) {
    GlobalConfigEntity entity =
        globalConfigRepository.findAll().stream()
            .findFirst()
            .orElseGet(() -> globalConfigRepository.save(buildDefaultGlobalConfig()));

    globalConfigMapper.updateGlobalConfigEntityFromDto(body, entity);
    GlobalConfigEntity savedEntity = globalConfigRepository.save(entity);
    syncOtherCostLinesFromGlobalConfig(savedEntity);
    return globalConfigMapper.toSWGlobalConfig(savedEntity);
  }

  public GlobalConfigEntity buildDefaultGlobalConfig() {
    GlobalConfigEntity entity = new GlobalConfigEntity();
    entity.setLaborCost(BigDecimal.ZERO);
    entity.setOverheadCost(BigDecimal.ZERO);
    entity.setInternalTransportation(BigDecimal.ZERO);
    entity.setDepreciationCost(BigDecimal.ZERO);
    entity.setAdministrationCost(BigDecimal.ZERO);
    entity.setStandardJigsAndFixturesCost(BigDecimal.ZERO);
    entity.setSmallPackagingCost(BigDecimal.ZERO);
    entity.setLargePackagingCost(BigDecimal.ZERO);
    entity.setMarkupApprovalStrategy(MarkupApprovalStrategy.FOR_ALL_QUOTATIONS);
    entity.setBaseMarkup(BigDecimal.ZERO);
    entity.setMarkupRange(BigDecimal.ZERO);
    entity.setCostChangeAlert(BigDecimal.ZERO);
    entity.setBudgetaryAdditionalRate(BigDecimal.ZERO);
    entity.setYieldPercentage(BigDecimal.valueOf(5.0));
    entity.setCurrencyExchangeRateStrategy(CurrencyExchangeRateStrategy.AUTOMATICALLY_UPDATED);
    entity.setAutomaticExchangeRateFrequency(AutomaticExchangeRateFrequency.EVERY_DAY);
    return entity;
  }

  @Transactional
  public SWCurrency getSystemTargetCurrencyCode() {
    String targetCurrencyCode = appConfigurationProperties.getTargetCurrencyCode();
    return currencyService.retrieveCurrencySummaryByCode(targetCurrencyCode);
  }

  private void syncOtherCostLinesFromGlobalConfig(GlobalConfigEntity globalConfig) {
    List<CostRequestLineEntity> lines =
        costRequestLineRepository.findAllNotFreezedAndHavingOtherCosts(
            CostRequestStatus.getDataFreezeStatuses());

    if (CollectionUtils.isEmpty(lines)) {
      return;
    }
    Map<String, BigDecimal> nameToValue =
        GlobalConstants.extractOtherCostNamesFromGlobalConfig(globalConfig);
    for (CostRequestLineEntity line : lines) {
      for (OtherCostLineEntity ocl : line.getOtherCostLines()) {
        if (!ocl.isFixedLine() || ocl.isEditableLine()) {
          continue;
        }
        if (ocl.isPackagingLine()) {
          switch (ocl.getPackagingSize()) {
            case SMALL -> ocl.setUnitCostInCurrency(globalConfig.getSmallPackagingCost());
            case LARGE -> ocl.setUnitCostInCurrency(globalConfig.getLargePackagingCost());
          }
        } else {
          BigDecimal newValue = nameToValue.get(ocl.getName());
          ocl.setUnitCostInCurrency(newValue);
        }
        ocl.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());
        for (OtherCostLinePerCostRequestQuantityEntity perQty :
            ocl.getOtherCostLineForCostRequestQuantities()) {
          perQty.buildCalculatedFields(appConfigurationProperties.getTargetCurrencyCode());
        }
      }
    }
  }
}
