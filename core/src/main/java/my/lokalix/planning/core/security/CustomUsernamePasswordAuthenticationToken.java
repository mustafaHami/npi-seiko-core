package my.lokalix.planning.core.security;

import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import my.lokalix.planning.core.models.enums.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class CustomUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

  private final UUID userId;
  private final String userLogin;

  public CustomUsernamePasswordAuthenticationToken(
      Object principal,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities,
      UUID userId,
      String userLogin) {
    super(principal, credentials, authorities);
    this.userId = userId;
    this.userLogin = userLogin;
  }

  public boolean hasRole(UserRole role) {
    return getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role.getSecurityValue()::equals);
  }
}
