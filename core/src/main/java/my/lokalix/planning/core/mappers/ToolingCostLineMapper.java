package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.ToolingCostLineEntity;
import my.zkonsulting.planning.generated.model.SWToolingCostLine;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {
      MessageMapper.class
    })
public abstract class ToolingCostLineMapper {

  @Mapping(source = "toolingCostLineId", target = "uid")
  @Mapping(source = "name", target = "description")
  @Mapping(source = "messages", target = "nbMessages", qualifiedByName = "countReceivedMessages")
  public abstract SWToolingCostLine toSwToolingCostLine(ToolingCostLineEntity entity);

  public abstract List<SWToolingCostLine> toListSWToolingCostLine(
      List<ToolingCostLineEntity> entities);

  @AfterMapping
  protected void setNbFiles(ToolingCostLineEntity entity, @MappingTarget SWToolingCostLine dto) {
    if (CollectionUtils.isNotEmpty(entity.getAttachedFiles())) {
      dto.setNbFiles(entity.getAttachedFiles().size());
    } else {
      dto.setNbFiles(0);
    }
  }
}
