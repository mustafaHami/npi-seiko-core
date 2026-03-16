package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.zkonsulting.planning.generated.model.SWSupplierAndManufacturer;
import my.zkonsulting.planning.generated.model.SWSupplierAndManufacturerCreate;
import my.zkonsulting.planning.generated.model.SWSupplierAndManufacturerUpdate;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {ShipmentMethodMapper.class})
public abstract class SupplierAndManufacturerMapper {

  public abstract List<SWSupplierAndManufacturer> toListSwSupplierManufacturer(
      List<SupplierManufacturerEntity> manufacturerEntities);

  @Mapping(source = "supplierManufacturerId", target = "uid")
  public abstract SWSupplierAndManufacturer toSwSupplierManufacturer(
      SupplierManufacturerEntity supplierManufacturerEntity);

  @Mapping(target = "shipmentMethod", ignore = true)
  public abstract void updateAdminSupplierManufacturerEntityFromDto(
      SWSupplierAndManufacturerUpdate dto, @MappingTarget SupplierManufacturerEntity entity);

  @Mapping(target = "shipmentMethod", ignore = true)
  public abstract SupplierManufacturerEntity toSupplierManufacturerEntity(
      SWSupplierAndManufacturerCreate body);
}
