package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerShipmentLocationEntity;
import my.zkonsulting.planning.generated.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {ShipmentMethodMapper.class, ShipmentLocationMapper.class, CurrencyMapper.class})
public abstract class CustomerMapper {

  @Mapping(target = "shipmentLocations", ignore = true)
  public abstract CustomerEntity toAdminCustomer(SWCustomerCreate customerConfigCreate);

  public abstract List<SWCustomer> toListSwCustomer(List<CustomerEntity> customerEntities);

  @Mapping(source = "customerId", target = "uid")
  public abstract SWCustomer toSWCustomer(CustomerEntity customerEntity);

  @Mapping(target = "shipmentLocations", ignore = true)
  public abstract void updateAdminCustomerEntityFromDto(
      SWCustomerUpdate dto, @MappingTarget CustomerEntity entity);

  @Mapping(target = "shipmentLocations", ignore = true)
  public abstract CustomerEntity toCopyCostumerEntity(CustomerEntity customer);

  @Mapping(source = "customerShipmentLocationId", target = "uid")
  @Mapping(source = "currency", target = "currency", qualifiedByName = "currencySummary")
  public abstract SWCustomerShipmentLocation toSWCustomerShipmentLocation(
      CustomerShipmentLocationEntity entity);

  public abstract List<SWCustomerShipmentLocation> toListSWCustomerShipmentLocation(
      List<CustomerShipmentLocationEntity> entities);

  @AfterMapping
  protected void setNbShipmentLocations(CustomerEntity entity, @MappingTarget SWCustomer dto) {
    if (CollectionUtils.isNotEmpty(entity.getShipmentLocations())) {
      dto.setNbShipmentLocations(entity.getShipmentLocations().size());
    } else {
      dto.setNbShipmentLocations(0);
    }
  }
}
