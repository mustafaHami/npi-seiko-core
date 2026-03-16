package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.admin.ProductNameEntity;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ProductNameMapper {

  public abstract ProductNameEntity toAdminProductName(SWProductNameCreate productNameCreate);

  public abstract List<SWProductName> toListSwProductName(
      List<ProductNameEntity> productNameEntities);

  @Mapping(source = "productNameId", target = "uid")
  public abstract SWProductName toSwProductName(ProductNameEntity productNameEntity);

  public abstract void updateAdminProductNameEntityFromDto(
      SWProductNameUpdate dto, @MappingTarget ProductNameEntity entity);
}
