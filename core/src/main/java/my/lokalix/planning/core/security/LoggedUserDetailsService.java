package my.lokalix.planning.core.security;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.repositories.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LoggedUserDetailsService {

  @Resource private UserRepository userRepository;

  public UUID getLoggedUserId() {
    return ((CustomUsernamePasswordAuthenticationToken)
            SecurityContextHolder.getContext().getAuthentication())
        .getUserId();
  }

  public String getLoggedUserLogin() {
    return ((CustomUsernamePasswordAuthenticationToken)
            SecurityContextHolder.getContext().getAuthentication())
        .getUserLogin();
  }

  public UserEntity getLoggedUserReference() {
    return userRepository.getReferenceById(getLoggedUserId());
  }

  public UserEntity getLoggedUserEntity() {
    return userRepository.findById(getLoggedUserId()).orElseThrow(EntityNotFoundException::new);
  }

  public boolean hasRole(UserRole role) {
    return ((CustomUsernamePasswordAuthenticationToken)
            SecurityContextHolder.getContext().getAuthentication())
        .hasRole(role);
  }
}
