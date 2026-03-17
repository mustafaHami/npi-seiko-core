package my.lokalix.planning.core.mappers;

import java.math.BigDecimal;
import java.util.List;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.zkonsulting.planning.generated.model.SWNpiOrder;
import my.zkonsulting.planning.generated.model.SWNpiOrderCreate;
import my.zkonsulting.planning.generated.model.SWNpiOrderUpdate;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class NpiOrderMapper {

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "archived", ignore = true)
  public abstract NpiOrderEntity toNpiOrderEntity(SWNpiOrderCreate dto);

  @Mapping(source = "npiOrderId", target = "uid")
  @Mapping(target = "currentProcessName", expression = "java(entity.getCurrentProcessName())")
  @Mapping(
      source = "processLines",
      target = "materialPurchasePlanTimeInHours",
      qualifiedByName = "getMaterialPurchasePlanTime")
  @Mapping(
      source = "processLines",
      target = "materialReceivingPlanTimeInHours",
      qualifiedByName = "getMaterialReceivingPlanTime")
  @Mapping(
      source = "processLines",
      target = "productionPlanTimeInHours",
      qualifiedByName = "getProductionPlanTime")
  @Mapping(
      source = "processLines",
      target = "testingPlanTimeInHours",
      qualifiedByName = "getTestingPlanTime")
  @Mapping(
      source = "processLines",
      target = "shippingPlanTimeInHours",
      qualifiedByName = "getShippingPlanTime")
  @Mapping(
      source = "processLines",
      target = "customerApprovalPlanTimeInHours",
      qualifiedByName = "getCustomerApprovalPlanTime")
  public abstract SWNpiOrder toSWNpiOrder(NpiOrderEntity entity);

  public abstract List<SWNpiOrder> toListSWNpiOrder(List<NpiOrderEntity> entities);

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "archived", ignore = true)
  @Mapping(target = "npiOrderId", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "processLines", ignore = true)
  public abstract void updateNpiOrderEntityFromDto(
      SWNpiOrderUpdate dto, @MappingTarget NpiOrderEntity entity);

  @Named("getMaterialPurchasePlanTime")
  BigDecimal getMaterialPurchasePlanTime(List<ProcessLineEntity> processLines) {
    return getPlanTimeByType(processLines, LineType.MATERIAL_PURCHASE);
  }

  @Named("getMaterialReceivingPlanTime")
  BigDecimal getMaterialReceivingPlanTime(List<ProcessLineEntity> processLines) {
    return getPlanTimeByType(processLines, LineType.MATERIAL_RECEIVING);
  }

  @Named("getProductionPlanTime")
  BigDecimal getProductionPlanTime(List<ProcessLineEntity> processLines) {
    return getPlanTimeByType(processLines, LineType.PRODUCTION);
  }

  @Named("getTestingPlanTime")
  BigDecimal getTestingPlanTime(List<ProcessLineEntity> processLines) {
    return getPlanTimeByType(processLines, LineType.TESTING);
  }

  @Named("getShippingPlanTime")
  BigDecimal getShippingPlanTime(List<ProcessLineEntity> processLines) {
    return getPlanTimeByType(processLines, LineType.SHIPPING);
  }

  @Named("getCustomerApprovalPlanTime")
  BigDecimal getCustomerApprovalPlanTime(List<ProcessLineEntity> processLines) {
    return getPlanTimeByType(processLines, LineType.CUSTOMER_APPROVAL);
  }

  private BigDecimal getPlanTimeByType(List<ProcessLineEntity> processLines, LineType lineType) {
    if (CollectionUtils.isEmpty(processLines)) {
      return null;
    }
    for (ProcessLineEntity line : processLines) {
      if (isMatchingLineType(line, lineType)) {
        return line.getPlanTimeInHours();
      }
    }
    return null;
  }

  private boolean isMatchingLineType(ProcessLineEntity line, LineType lineType) {
    return switch (lineType) {
      case MATERIAL_PURCHASE -> Boolean.TRUE.equals(line.getIsMaterialPurchase());
      case MATERIAL_RECEIVING -> Boolean.TRUE.equals(line.getIsMaterialReceiving());
      case PRODUCTION -> Boolean.TRUE.equals(line.getIsProduction());
      case TESTING -> Boolean.TRUE.equals(line.getIsTesting());
      case SHIPPING -> Boolean.TRUE.equals(line.getIsShipment());
      case CUSTOMER_APPROVAL -> Boolean.TRUE.equals(line.getIsCustomerApproval());
    };
  }

  private enum LineType {
    MATERIAL_PURCHASE,
    MATERIAL_RECEIVING,
    PRODUCTION,
    TESTING,
    SHIPPING,
    CUSTOMER_APPROVAL
  }
}
