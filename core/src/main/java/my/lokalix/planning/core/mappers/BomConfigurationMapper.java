package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.BomConfigurationEntity;
import my.lokalix.planning.core.models.entities.admin.BomConfigurationMaterialsEmbeddable;
import my.lokalix.planning.core.models.entities.admin.ExcelCellEmbeddable;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class BomConfigurationMapper {

  public abstract ExcelCellEmbeddable toExcelCellEmbeddable(SWExcelCell dto);

  public abstract SWExcelCell toSwExcelCell(ExcelCellEmbeddable entity);

  public abstract BomConfigurationMaterialsEmbeddable toBomConfigurationMaterialsEmbeddable(
      SWBomConfigurationMaterials dto);

  public abstract SWBomConfigurationMaterials toSwBomConfigurationMaterials(
      BomConfigurationMaterialsEmbeddable entity);

  public abstract BomConfigurationEntity toBomConfigurationEntity(SWBomConfigurationCreate dto);

  @Mapping(source = "bomConfigurationId", target = "uid")
  public abstract SWBomConfiguration toSwBomConfiguration(BomConfigurationEntity entity);

  public abstract List<SWBomConfiguration> toListSwBomConfiguration(
      List<BomConfigurationEntity> entities);

  public abstract void updateBomConfigurationEntityFromDto(
      SWBomConfigurationUpdate dto, @MappingTarget BomConfigurationEntity entity);
}
