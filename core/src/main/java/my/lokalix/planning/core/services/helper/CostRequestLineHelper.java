package my.lokalix.planning.core.services.helper;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostRequestLineHelper {

  private final EntityRetrievalHelper entityRetrievalHelper;

  public BigDecimal resolveAdditionalProcessRate(
      CostRequestLineEntity lineEntity, GlobalConfigEntity globalConfig) {
    switch (lineEntity.getCostingMethodType()) {
      case HV, LV -> {
        return BigDecimal.ZERO;
      }
      case BUDGETARY -> {
        if (lineEntity.getStatus().isDataFreezeStatus()) {
          return lineEntity.getCostRequest().getArchivedGlobalConfig().getBudgetaryAdditionalRate()
                  != null
              ? lineEntity.getCostRequest().getArchivedGlobalConfig().getBudgetaryAdditionalRate()
              : BigDecimal.ZERO;
        } else {
          return globalConfig.getBudgetaryAdditionalRate() != null
              ? globalConfig.getBudgetaryAdditionalRate()
              : BigDecimal.ZERO;
        }
      }
      case NPI -> {
        if (lineEntity.getStatus().isDataFreezeStatus()) {
          return lineEntity
                      .getCostRequest()
                      .getArchivedGlobalConfig()
                      .getNpiProcessesAdditionalRate()
                  != null
              ? lineEntity
                  .getCostRequest()
                  .getArchivedGlobalConfig()
                  .getNpiProcessesAdditionalRate()
              : BigDecimal.ZERO;
        } else {
          return globalConfig.getNpiProcessesAdditionalRate() != null
              ? globalConfig.getNpiProcessesAdditionalRate()
              : BigDecimal.ZERO;
        }
      }
      default ->
          throw new IllegalStateException("Unexpected value: " + lineEntity.getCostingMethodType());
    }
  }

  public BigDecimal resolveYield(
      CostRequestLineEntity lineEntity, GlobalConfigEntity globalConfig) {
    if (lineEntity.getStatus().isDataFreezeStatus()) {
      return lineEntity.getCostRequest().getArchivedGlobalConfig().getYieldPercentage();
    } else {
      return globalConfig.getYieldPercentage();
    }
  }

  public BigDecimal resolveCurrencyExchangeRate(
      CostRequestLineEntity lineEntity,
      @NotBlank String fromCurrencyCode,
      @NotBlank String targetCurrencyCode) {
    if (lineEntity.getStatus().isDataFreezeStatus()) {
      if (lineEntity.getCostRequest().getCurrency().getCode().equals(fromCurrencyCode)) {
        return lineEntity.getCostRequest().getCurrency().findExchangeRate(targetCurrencyCode);
      } else {
        return lineEntity.getCostRequest().getCurrency().getExchangeRates().stream()
            .filter(er -> er.getToCurrency().getCode().equals(fromCurrencyCode))
            .findFirst()
            .orElseThrow()
            .getToCurrency()
            .findExchangeRate(targetCurrencyCode);
      }
    } else {
      return entityRetrievalHelper
          .getMustExistCurrencyByCode(fromCurrencyCode)
          .findExchangeRate(targetCurrencyCode);
    }
  }

  public CurrencyEntity resolveCurrency(
      CostRequestLineEntity lineEntity, @NotBlank String currencyCode) {
    if (lineEntity.getStatus().isDataFreezeStatus()) {
      if (lineEntity.getCostRequest().getCurrency().getCode().equals(currencyCode)) {
        return lineEntity.getCostRequest().getCurrency();
      } else {
        return lineEntity.getCostRequest().getCurrency().getExchangeRates().stream()
            .filter(er -> er.getToCurrency().getCode().equals(currencyCode))
            .findFirst()
            .orElseThrow()
            .getToCurrency();
      }
    } else {
      return entityRetrievalHelper.getMustExistCurrencyByCode(currencyCode);
    }
  }
}
