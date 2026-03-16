package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class UnitMapper {

  public abstract UnitEntity toUnitEntity(SWUnitCreate unitCreate);

  public abstract List<SWUnit> toListSwUnit(List<UnitEntity> unitEntities);

  @Mapping(source = "unitId", target = "uid")
  public abstract SWUnit toSWUnit(UnitEntity unitEntity);

  public abstract void updateUnitEntityFromDto(SWUnitUpdate dto, @MappingTarget UnitEntity entity);
}
