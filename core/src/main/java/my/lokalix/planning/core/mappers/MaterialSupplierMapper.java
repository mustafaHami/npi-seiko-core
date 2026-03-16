package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.MaterialSupplierEntity;
import my.zkonsulting.planning.generated.model.SWMaterialSupplier;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierCreate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {
      MaterialSupplierMoqLineMapper.class,
      ShipmentMethodMapper.class,
      CurrencyMapper.class,
      SupplierAndManufacturerMapper.class
    })
public abstract class MaterialSupplierMapper {

  @Mapping(target = "purchasingCurrency", ignore = true)
  @Mapping(target = "supplier", ignore = true)
  @Mapping(target = "moqLines", ignore = true)
  public abstract MaterialSupplierEntity toMaterialSupplierEntity(SWMaterialSupplierCreate dto);

  @Mapping(
      source = "purchasingCurrency",
      target = "purchasingCurrency",
      qualifiedByName = "currencySummary")
  @Mapping(source = "materialSupplierId", target = "uid")
  public abstract SWMaterialSupplier toSwMaterialSupplier(MaterialSupplierEntity entity);

  public abstract List<SWMaterialSupplier> toListSwMaterialSupplier(
      List<MaterialSupplierEntity> entities);

  @Mapping(target = "supplier", ignore = true)
  @Mapping(target = "material", ignore = true)
  @Mapping(target = "purchasingCurrency", ignore = true)
  @Mapping(target = "moqLines", ignore = true)
  public abstract MaterialSupplierEntity toCopyMaterialSupplier(
      MaterialSupplierEntity activeSupplier);
}
