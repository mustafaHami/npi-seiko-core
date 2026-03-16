package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class MaterialCategoryMapper {

  public abstract MaterialCategoryEntity toAdminMaterialCategory(
      SWMaterialCategoryCreate materialCategoryCreate);

  public abstract List<SWMaterialCategory> toListSwMaterialCategory(
      List<MaterialCategoryEntity> materialCategoryEntities);

  @Mapping(source = "materialCategoryId", target = "uid")
  public abstract SWMaterialCategory toSwMaterialCategory(
      MaterialCategoryEntity materialCategoryEntity);

  public abstract void updateAdminMaterialCategoryEntityFromDto(
      SWMaterialCategoryUpdate dto, @MappingTarget MaterialCategoryEntity entity);
}
