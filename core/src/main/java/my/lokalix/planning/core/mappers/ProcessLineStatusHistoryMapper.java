package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.ProcessLineStatusHistoryEntity;
import my.zkonsulting.planning.generated.model.SWProcessLineStatusHistory;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ProcessLineStatusHistoryMapper {

  @Mapping(source = "user.userId", target = "byUser.uid")
  @Mapping(source = "user.login", target = "byUser.login")
  @Mapping(source = "user.role", target = "byUser.role")
  @Mapping(source = "user.active", target = "byUser.active")
  public abstract SWProcessLineStatusHistory toSWProcessLineStatusHistory(
      ProcessLineStatusHistoryEntity entity);

  public abstract List<SWProcessLineStatusHistory> toListSWProcessLineStatusHistory(
      List<ProcessLineStatusHistoryEntity> entities);
}
