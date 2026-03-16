package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.ShipmentMethodEntity;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ShipmentMethodMapper {

  public abstract ShipmentMethodEntity toShipmentMethodEntity(
      SWShipmentMethodCreate shipmentMethodCreate);

  public abstract List<SWShipmentMethod> toListSwShipmentMethod(
      List<ShipmentMethodEntity> shipmentMethodEntities);

  @Mapping(source = "shipmentMethodId", target = "uid")
  public abstract SWShipmentMethod toSwShipmentMethod(ShipmentMethodEntity shipmentMethodEntity);

  public abstract void updateShipmentMethodEntityFromDto(
      SWShipmentMethodUpdate dto, @MappingTarget ShipmentMethodEntity entity);
}
