package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.OtherCostLineEntity;
import my.zkonsulting.planning.generated.model.SWOtherCostLine;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {ShipmentLocationMapper.class, CurrencyMapper.class})
public abstract class OtherCostLineMapper {

  @Mapping(source = "otherCostLineId", target = "uid")
  @Mapping(source = "name", target = "description")
  @Mapping(source = "currency", target = "currency", qualifiedByName = "currencySummary")
  public abstract SWOtherCostLine toSWOtherCostLine(OtherCostLineEntity entity);

  public abstract List<SWOtherCostLine> toListSWOtherCostLine(List<OtherCostLineEntity> entities);
}
