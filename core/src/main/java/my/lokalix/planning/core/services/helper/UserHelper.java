package my.lokalix.planning.core.services.helper;

import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.models.enums.UserType;
import my.lokalix.planning.core.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserHelper {

  @Resource private UserRepository userRepository;

  public List<UserEntity> getAllActiveUsersByRole(UserRole role) {
    return userRepository.findByRoleAndTypeAndActiveTrue(role, UserType.EMAIL_ADDRESS);
  }
}
