package my.lokalix.planning.core.mappers;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import my.lokalix.planning.core.models.entities.MaterialEntity;
import my.lokalix.planning.core.models.enums.MaterialType;
import my.zkonsulting.planning.generated.model.SWMaterial;
import my.zkonsulting.planning.generated.model.SWMaterialCreate;
import my.zkonsulting.planning.generated.model.SWMaterialType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {
      MaterialSupplierMapper.class,
      SupplierAndManufacturerMapper.class,
      MaterialCategoryMapper.class,
      UnitMapper.class
    })
public abstract class MaterialMapper {

  @Mapping(target = "manufacturer", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "suppliers", ignore = true)
  @Mapping(target = "unit", ignore = true)
  public abstract MaterialEntity toMaterialEntity(SWMaterialCreate dto);

  @Mapping(source = "materialId", target = "uid")
  @Mapping(source = "unit.name", target = "unit")
  public abstract SWMaterial toSwMaterial(MaterialEntity entity);

  public abstract List<SWMaterial> toListSwMaterial(List<MaterialEntity> entities);

  public abstract SWMaterialType toSwMaterialType(@NotNull MaterialType materialType);

  @Mapping(target = "manufacturer", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "suppliers", ignore = true)
  @Mapping(target = "unit", ignore = true)
  public abstract MaterialEntity toCopyMaterial(@NotNull MaterialEntity material);

  public abstract MaterialType toMaterialType(SWMaterialType materialType);
}
