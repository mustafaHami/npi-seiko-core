package my.lokalix.planning.core.mappers;

import java.util.List;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.models.enums.UserType;
import my.zkonsulting.planning.generated.model.SWUser;
import my.zkonsulting.planning.generated.model.SWUserRole;
import my.zkonsulting.planning.generated.model.SWUserType;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
  @ValueMapping(source = MappingConstants.NULL, target = UserRole.Constants.ADMINISTRATOR)
  UserRole asUserRole(SWUserRole role);

  SWUserType asSwUserType(UserType type);

  UserType asUserType(SWUserType type);

  List<SWUser> toListSwUser(List<UserEntity> users);

  @Mapping(source = "userId", target = "uid")
  SWUser toSwUser(UserEntity userEntity);
}
