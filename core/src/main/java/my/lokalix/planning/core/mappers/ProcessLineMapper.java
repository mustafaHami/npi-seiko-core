package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.ProcessLineEntity;
import my.zkonsulting.planning.generated.model.SWProcessLine;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ProcessLineMapper {

  @Mapping(source = "processLineId", target = "uid")
  @Mapping(source = "isMaterialPurchase", target = "isMaterialPurchase")
  @Mapping(source = "isMaterialReceiving", target = "isMaterialReceiving")
  @Mapping(source = "isProduction", target = "isProduction")
  @Mapping(source = "isTesting", target = "isTesting")
  @Mapping(source = "isShipment", target = "isShipment")
  @Mapping(source = "isCustomerApproval", target = "isCustomerApproval")
  public abstract SWProcessLine toSWProcessLine(ProcessLineEntity entity);

  public abstract List<SWProcessLine> toListSWProcessLine(List<ProcessLineEntity> entities);
}
