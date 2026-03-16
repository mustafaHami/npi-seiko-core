package my.lokalix.planning.core.mappers;

import my.lokalix.planning.core.models.enums.*;
import my.zkonsulting.planning.generated.model.*;
import my.zkonsulting.planning.generated.model.SWCostingMethodType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EnumMapper {

  public SWCostingMethodType asSwCostingMethodType(CostingMethodType type) {
    if (type == null) return null;
    return SWCostingMethodType.fromValue(type.getValue());
  }

  public CostingMethodType asCostingMethodType(SWCostingMethodType type) {
    if (type == null) return null;
    return CostingMethodType.valueOf(type.getValue());
  }

  public SWCostRequestStatus asSwCostRequestStatus(CostRequestStatus status) {
    if (status == null) return null;
    return SWCostRequestStatus.fromValue(status.getValue());
  }

  public CostRequestStatus asCostRequestStatus(SWCostRequestStatus status) {
    if (status == null) return null;
    return CostRequestStatus.valueOf(status.getValue());
  }

  public SWMarkupApprovalStrategy asSwMarkupApprovalStrategy(MarkupApprovalStrategy strategy) {
    if (strategy == null) return null;
    return SWMarkupApprovalStrategy.fromValue(strategy.getValue());
  }

  public MarkupApprovalStrategy asMarkupApprovalStrategy(SWMarkupApprovalStrategy strategy) {
    if (strategy == null) return null;
    return MarkupApprovalStrategy.valueOf(strategy.getValue());
  }

  public SWCurrencyExchangeRateStrategy asSwCurrencyExchangeRateStrategy(
      CurrencyExchangeRateStrategy strategy) {
    if (strategy == null) return null;
    return SWCurrencyExchangeRateStrategy.fromValue(strategy.getValue());
  }

  public CurrencyExchangeRateStrategy asCurrencyExchangeRateStrategy(
      SWCurrencyExchangeRateStrategy strategy) {
    if (strategy == null) return null;
    return CurrencyExchangeRateStrategy.valueOf(strategy.getValue());
  }

  public SWAutomaticExchangeRateFrequency asSwAutomaticExchangeRateFrequency(
      AutomaticExchangeRateFrequency frequency) {
    if (frequency == null) return null;
    return SWAutomaticExchangeRateFrequency.fromValue(frequency.getValue());
  }

  public AutomaticExchangeRateFrequency asAutomaticExchangeRateFrequency(
      SWAutomaticExchangeRateFrequency frequency) {
    if (frequency == null) return null;
    return AutomaticExchangeRateFrequency.valueOf(frequency.getValue());
  }

  public SupplierAndManufacturerType asSupplierAndManufacturerType(
      SWSupplierAndManufacturerType type) {
    if (type == null) return null;
    return SupplierAndManufacturerType.valueOf(type.getValue());
  }

  public OtherCostLineCalculationStrategy asOtherCostLineCalculationStrategy(
      SWOtherCostLineCalculationStrategy calculationStrategy) {
    if (calculationStrategy == null) return null;
    return OtherCostLineCalculationStrategy.valueOf(calculationStrategy.getValue());
  }

  public PackagingSize asPackagingSize(SWPackagingSize packagingSize) {
    if (packagingSize == null) return null;
    return PackagingSize.valueOf(packagingSize.getValue());
  }

  public ToolingStrategy asToolingStrategy(SWToolingStrategy strategy) {
    if (strategy == null) return null;
    return ToolingStrategy.valueOf(strategy.getValue());
  }
}
