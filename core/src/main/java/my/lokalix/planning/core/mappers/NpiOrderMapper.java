package my.lokalix.planning.core.mappers;

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
  @Mapping(
      source = "processLines",
      target = "currentProcessName",
      qualifiedByName = "getCurrentProcessName")
  public abstract SWNpiOrder toSWNpiOrder(NpiOrderEntity entity);

  public abstract List<SWNpiOrder> toListSWNpiOrder(List<NpiOrderEntity> entities);

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "archived", ignore = true)
  @Mapping(target = "npiOrderId", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "processLines", ignore = true)
  public abstract void updateNpiOrderEntityFromDto(
      SWNpiOrderUpdate dto, @MappingTarget NpiOrderEntity entity);

  @Named("getCurrentProcessName")
  String getCurrentProcessName(List<ProcessLineEntity> processLines) {
    if (CollectionUtils.isNotEmpty(processLines)) {
      for (ProcessLineEntity line : processLines) {
        if (!line.getStatus().isFinalStatus()) {
          return line.getProcessName();
        }
      }
    }
    return processLines.getLast().getProcessName();
  }
}
