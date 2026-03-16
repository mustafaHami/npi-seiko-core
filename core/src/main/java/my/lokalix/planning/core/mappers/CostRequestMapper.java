package my.lokalix.planning.core.mappers;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import my.lokalix.planning.core.models.entities.CostRequestEntity;
import my.lokalix.planning.core.models.entities.CostRequestFrozenShipmentLocationEntity;
import my.lokalix.planning.core.models.entities.FileInfoEntity;
import my.lokalix.planning.core.models.enums.FileType;
import my.lokalix.planning.core.utils.TextUtils;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {
      CostRequestLineMapper.class,
      EnumMapper.class,
      CustomerMapper.class,
      CurrencyMapper.class,
      MaterialLineMapper.class,
      ProcessLineMapper.class,
      ToolingCostLineMapper.class,
      OtherCostLineMapper.class,
      MessageMapper.class,
      ShipmentLocationMapper.class,
    })
public abstract class CostRequestMapper {

  @Mapping(target = "lines", ignore = true)
  @Mapping(target = "customerEmails", ignore = true)
  public abstract CostRequestEntity toCostRequestEntity(SWCostRequestCreate dto);

  @Mapping(source = "costRequestId", target = "uid")
  @Mapping(source = "currency", target = "currency", qualifiedByName = "currencySummary")
  @Mapping(
      target = "customerEmails",
      expression = "java(mapCustomerEmails(entity.getCustomerEmails()))")
  @Mapping(source = "messages", target = "nbMessages", qualifiedByName = "countReceivedMessages")
  @Mapping(target = "expired", expression = "java(entity.isExpired())")
  public abstract SWCostRequest toSWCostRequest(CostRequestEntity entity);

  public abstract List<SWCostRequest> toListSWCostRequest(List<CostRequestEntity> entities);

  @Mapping(target = "customerEmails", ignore = true)
  public abstract void updateCostRequestEntityFromDto(
      SWCostRequestUpdate dto, @MappingTarget CostRequestEntity entity);

  @AfterMapping
  protected void setNbFiles(CostRequestEntity entity, @MappingTarget SWCostRequest dto) {
    List<FileInfoEntity> attachedFiles = entity.getAttachedFilesPerFileType(FileType.ANY);
    if (CollectionUtils.isNotEmpty(attachedFiles)) {
      dto.setNbFiles(attachedFiles.size());
    } else {
      dto.setNbFiles(0);
    }
  }

  public List<String> mapCustomerEmails(String customerEmails) {
    if (StringUtils.isBlank(customerEmails)) {
      return new ArrayList<>();
    }
    return TextUtils.splitConcatenatedListWithSeparator(customerEmails, Function.identity());
  }

  @IterableMapping(qualifiedByName = "toSwCostRequestForEngineering")
  public abstract List<@Valid SWCostRequest> toListSwCostRequestForEngineering(
      List<CostRequestEntity> entities);

  @Named("toSwCostRequestForEngineering")
  @Mapping(source = "costRequestId", target = "uid")
  @Mapping(target = "customerEmails", ignore = true)
  @Mapping(target = "requestorName", ignore = true)
  @Mapping(target = "projectName", ignore = true)
  @Mapping(target = "purchaseOrderExpectedDate", ignore = true)
  @Mapping(source = "currency", target = "currency", qualifiedByName = "currencySummary")
  @Mapping(source = "messages", target = "nbMessages", qualifiedByName = "countReceivedMessages")
  @Mapping(target = "expired", constant = "false")
  public abstract SWCostRequest toSwCostRequestForEngineering(CostRequestEntity entity);

  @Mapping(source = "costRequestFrozenShipmentLocationId", target = "uid")
  @Mapping(
      source = "shipmentLocation",
      target = "shipmentLocation",
      qualifiedByName = "shipmentLocationSummary")
  public abstract SWCostRequestShipmentLocation toSwCostRequestShipmentLocation(
      CostRequestFrozenShipmentLocationEntity entity);
}
