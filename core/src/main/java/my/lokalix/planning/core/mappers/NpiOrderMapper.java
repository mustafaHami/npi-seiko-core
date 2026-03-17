package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.NpiOrderEntity;
import my.zkonsulting.planning.generated.model.SWNpiOrder;
import my.zkonsulting.planning.generated.model.SWNpiOrderCreate;
import my.zkonsulting.planning.generated.model.SWNpiOrderUpdate;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class NpiOrderMapper {

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "archived", ignore = true)
  public abstract NpiOrderEntity toNpiOrderEntity(SWNpiOrderCreate dto);

  @Mapping(source = "npiOrderId", target = "uid")
  public abstract SWNpiOrder toSWNpiOrder(NpiOrderEntity entity);

  public abstract List<SWNpiOrder> toListSWNpiOrder(List<NpiOrderEntity> entities);

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "archived", ignore = true)
  @Mapping(target = "npiOrderId", ignore = true)
  @Mapping(target = "creationDate", ignore = true)
  public abstract void updateNpiOrderEntityFromDto(
      SWNpiOrderUpdate dto, @MappingTarget NpiOrderEntity entity);
}
