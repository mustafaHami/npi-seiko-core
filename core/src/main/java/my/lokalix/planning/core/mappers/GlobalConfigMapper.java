package my.lokalix.planning.core.mappers;

import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;
import my.zkonsulting.planning.generated.model.SWGlobalConfig;
import my.zkonsulting.planning.generated.model.SWGlobalConfigPatch;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = {EnumMapper.class})
public abstract class GlobalConfigMapper {

  public abstract SWGlobalConfig toSWGlobalConfig(GlobalConfigEntity entity);

  public abstract void updateGlobalConfigEntityFromDto(
      SWGlobalConfigPatch dto, @MappingTarget GlobalConfigEntity entity);
}
