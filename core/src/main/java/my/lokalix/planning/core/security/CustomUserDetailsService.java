package my.lokalix.planning.core.security;

import jakarta.annotation.Resource;
import my.lokalix.planning.core.exceptions.UserIsInactiveException;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserType;
import my.lokalix.planning.core.repositories.UserRepository;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Resource private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {
    UserEntity user =
        userRepository
            .findByLoginIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException(email));
    if (BooleanUtils.isNotTrue(user.isActive())) {
      throw new UserIsInactiveException(
          "This account has been deactivated", SWCustomErrorCode.MUST_BE_DISCONNECTED);
    }
    if (user.getType().equals(UserType.EMAIL_ADDRESS)
        && BooleanUtils.isNotTrue(user.isRegisteredEmail())) {
      throw new UserIsInactiveException(
          "This account has not been registered", SWCustomErrorCode.MUST_BE_DISCONNECTED);
    }
    return User.builder()
        .username(user.getLogin())
        .password(user.getPassword())
        .roles(user.getRole().getValue())
        .build();
  }
}
