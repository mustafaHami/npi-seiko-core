package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.ProcessEntity;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {CurrencyMapper.class})
public abstract class ProcessMapper {

  @Mapping(target = "currency", ignore = true)
  public abstract ProcessEntity toAdminProcess(SWProcessCreate processCreate);

  public abstract List<SWProcess> toListSwProcess(List<ProcessEntity> processEntities);

  @Mapping(source = "processId", target = "uid")
  @Mapping(source = "currency", target = "currency", qualifiedByName = "currencySummary")
  public abstract SWProcess toSWProcess(ProcessEntity processEntity);

  @Mapping(target = "currency", ignore = true)
  public abstract void updateAdminProcessEntityFromDto(
      SWProcessUpdate dto, @MappingTarget ProcessEntity entity);

  @Mapping(target = "currency", ignore = true)
  public abstract ProcessEntity toCopyProcessEntity(ProcessEntity entity);
}
