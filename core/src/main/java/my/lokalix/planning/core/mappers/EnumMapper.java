package my.lokalix.planning.core.mappers;

import my.lokalix.planning.core.models.enums.*;
import my.zkonsulting.planning.generated.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EnumMapper {

  public SWNpiOrderStatus asSwNpiOrderStatus(NpiOrderStatus type) {
    if (type == null) return null;
    return SWNpiOrderStatus.fromValue(type.getValue());
  }
}
