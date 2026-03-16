package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.zkonsulting.planning.generated.model.SWProcessCostLine;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ProcessLineMapper {

  public abstract List<SWProcessCostLine> toListSwProcessCostLine(List<ProcessLineEntity> entities);

  @Mapping(source = "processLineId", target = "uid")
  @Mapping(source = "process.processId", target = "processId")
  @Mapping(source = "process.name", target = "processName")
  @Mapping(source = "process.costPerMinute", target = "processCostPerMinute")
  @Mapping(source = "process.setupProcess", target = "processIsSetup")
  @Mapping(source = "process.currency.code", target = "processCurrencyCode")
  public abstract SWProcessCostLine toSwProcessCostLine(ProcessLineEntity entity);
}
