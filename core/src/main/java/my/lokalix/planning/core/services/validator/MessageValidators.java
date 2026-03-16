package my.lokalix.planning.core.services.validator;

import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.entities.MessageEntity;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageValidators {
  private final LoggedUserDetailsService loggedUserDetailsService;

  public void checkIsMessageOwner(MessageEntity entity) {
    if (!entity.getUser().getUserId().equals(loggedUserDetailsService.getLoggedUserId())) {
      throw new AccessDeniedException("You do not have the necessary permissions");
    }
  }
}
