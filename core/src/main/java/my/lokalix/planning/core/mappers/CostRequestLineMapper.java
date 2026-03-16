package my.lokalix.planning.core.mappers;

import io.micrometer.common.util.StringUtils;
import java.util.List;
import my.lokalix.planning.core.models.entities.*;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.utils.TextUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {
      EnumMapper.class,
      CurrencyMapper.class,
      ExchangeRateMapper.class,
      SupplierAndManufacturerMapper.class,
      MaterialMapper.class,
      MaterialCategoryMapper.class,
      MaterialSupplierMapper.class,
      MaterialSupplierMoqLineMapper.class,
      ProductNameMapper.class,
      MessageMapper.class
    },
    imports = TextUtils.class)
public abstract class CostRequestLineMapper {

  @Mapping(source = "costRequestLineId", target = "uid")
  @Mapping(source = "costRequest.costRequestId", target = "parentCostRequestUid")
  @Mapping(target = "quantities", expression = "java(entity.getQuantitiesAsList())")
  @Mapping(
      source = "entity",
      target = "procurementStatus",
      qualifiedByName = "handleCostRequestLineProcurementStatus")
  @Mapping(source = "messages", target = "nbMessages", qualifiedByName = "countReceivedMessages")
  @Mapping(
      source = "toolingCostLines",
      target = "nbToolingLines",
      qualifiedByName = "countToolingLines")
  public abstract SWCostRequestLine toSwCostRequestLine(CostRequestLineEntity entity);

  public abstract List<SWCostRequestLine> toListSwCostRequestLine(
      List<CostRequestLineEntity> entities);

  public abstract List<SWCustomCostRequestLine> toListSWCustomCostRequestLine(
      List<CostRequestLineEntity> entities);

  @Mapping(source = "costRequestLineId", target = "uid")
  @Mapping(source = "costRequest.costRequestId", target = "parentCostRequestUid")
  @Mapping(source = "costRequest.costRequestReferenceNumber", target = "costRequestReferenceNumber")
  @Mapping(source = "costRequest.costRequestRevision", target = "costRequestRevision")
  @Mapping(source = "costRequest.customer", target = "customer")
  @Mapping(target = "quantities", expression = "java(entity.getQuantitiesAsList())")
  @Mapping(source = "messages", target = "nbMessages", qualifiedByName = "countReceivedMessages")
  public abstract SWCustomCostRequestLine toSWCustomCostRequestLine(CostRequestLineEntity entity);

  @Mapping(
      target = "quantities",
      expression = "java(TextUtils.concatenateListWithSeparator(dto.getQuantities()))")
  @Mapping(target = "materialLines", ignore = true)
  @Mapping(target = "draftMaterialLines", ignore = true)
  public abstract void updateCostRequestLineEntityFromDto(
      SWCostRequestLineUpdate dto, @MappingTarget CostRequestLineEntity entity);

  @AfterMapping
  protected void setNbFiles(CostRequestLineEntity entity, @MappingTarget SWCostRequestLine dto) {
    if (CollectionUtils.isNotEmpty(entity.getAttachedFiles())) {
      dto.setNbFiles(entity.getAttachedFiles().size());
    } else {
      dto.setNbFiles(0);
    }
  }

  @AfterMapping
  protected void countMaterialLinesMapping(
      CostRequestLineEntity entity, @MappingTarget SWCostRequestLine dto) {
    if (entity.getStatus().isPendingAndUpdatable()
        || (entity.getStatus() == CostRequestStatus.ABORTED
            && entity.getMaterialLines().isEmpty())) {
      // Second condition if to help as if aborted we do not know at what stage it was (before
      // READY TO/ ESTIMATE or after)
      dto.setNbMaterialLines(entity.getDraftMaterialLines().size());
    } else {
      dto.setNbMaterialLines(entity.getMaterialLines().size());
    }
  }

  @Named("handleCostRequestLineProcurementStatus")
  SWCostRequestLineProcurementStatus handleCostRequestLineProcurementStatus(
      CostRequestLineEntity entity) {
    if (entity.isOutsourced()) {
      return null;
    }
    if (entity.getStatus().isPendingAndUpdatable()) {
      if (CollectionUtils.isNotEmpty(entity.getDraftMaterialLines())) {
        return entity.getDraftMaterialLines().stream()
                .anyMatch(l -> StringUtils.isNotBlank(l.getMissingData()))
            ? SWCostRequestLineProcurementStatus.MISSING
            : null;
      }
      return null;
    } else {
      List<MaterialLineEntity> materialLines = entity.getOnlyMaterialLinesUsedForQuotation();
      if (CollectionUtils.isNotEmpty(materialLines)) {
        if (materialLines.stream().anyMatch(ml -> ml.getChosenMaterialSupplier() == null)) {
          return SWCostRequestLineProcurementStatus.PENDING;
        }
      }
      if (CollectionUtils.isNotEmpty(entity.getToolingCostLines())) {
        if (entity.getToolingCostLines().stream()
            .anyMatch(t -> t.isOutsourced() && !t.getOutsourcingStatus().isEstimated()))
          return SWCostRequestLineProcurementStatus.PENDING;
      }
      return SWCostRequestLineProcurementStatus.OK;
    }
  }

  @Named("countToolingLines")
  int countToolingLines(List<ToolingCostLineEntity> toolingCostLines) {
    if (CollectionUtils.isNotEmpty(toolingCostLines)) {
      return toolingCostLines.size();
    }
    return 0;
  }
}
