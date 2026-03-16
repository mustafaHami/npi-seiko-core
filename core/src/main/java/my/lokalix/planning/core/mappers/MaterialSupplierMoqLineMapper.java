package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.MaterialSupplierMoqLineEntity;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierMoqLine;
import my.zkonsulting.planning.generated.model.SWMaterialSupplierMoqLineCreate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class MaterialSupplierMoqLineMapper {

  public abstract MaterialSupplierMoqLineEntity toMaterialSupplierMoqLineEntity(
      SWMaterialSupplierMoqLineCreate dto);

  @Mapping(source = "materialSupplierMoqLineId", target = "uid")
  public abstract SWMaterialSupplierMoqLine toSwMaterialSupplierMoqLine(
      MaterialSupplierMoqLineEntity entity);

  public abstract List<SWMaterialSupplierMoqLine> toListSwMaterialSupplierMoqLine(
      List<MaterialSupplierMoqLineEntity> entities);
}
