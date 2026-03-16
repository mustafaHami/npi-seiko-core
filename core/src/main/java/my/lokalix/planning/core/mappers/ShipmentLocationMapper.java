package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.zkonsulting.planning.generated.model.SWShipmentLocation;
import my.zkonsulting.planning.generated.model.SWShipmentLocationCreate;
import my.zkonsulting.planning.generated.model.SWShipmentLocationUpdate;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {CurrencyMapper.class})
public abstract class ShipmentLocationMapper {

  public abstract ShipmentLocationEntity toShipmentLocationEntity(SWShipmentLocationCreate dto);

  @Named("shipmentLocationFull")
  @Mapping(source = "shipmentLocationId", target = "uid")
  @Mapping(
      source = "entity.acceptedCurrencies",
      target = "acceptedCurrencies",
      qualifiedByName = "currencySummarySet")
  public abstract SWShipmentLocation toSWShipmentLocation(ShipmentLocationEntity entity);

  @Named("shipmentLocationSummary")
  @Mapping(source = "shipmentLocationId", target = "uid")
  @Mapping(target = "acceptedCurrencies", ignore = true)
  public abstract SWShipmentLocation toSWShipmentLocationSummary(ShipmentLocationEntity entity);

  @IterableMapping(qualifiedByName = "shipmentLocationFull")
  public abstract List<SWShipmentLocation> toListSWShipmentLocation(
      List<ShipmentLocationEntity> entities);

  public abstract void updateEntityFromDto(
      SWShipmentLocationUpdate dto, @MappingTarget ShipmentLocationEntity entity);
}
