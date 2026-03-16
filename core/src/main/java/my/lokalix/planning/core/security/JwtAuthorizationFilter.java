package my.lokalix.planning.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.models.entities.AuthTokenEntity;
import my.lokalix.planning.core.models.entities.UserEntity;
import my.lokalix.planning.core.models.enums.UserType;
import my.lokalix.planning.core.repositories.AuthTokenRepository;
import my.lokalix.planning.core.repositories.UserRepository;
import my.zkonsulting.planning.generated.model.SWCustomError;
import my.zkonsulting.planning.generated.model.SWCustomErrorCode;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final AuthTokenRepository authTokenRepository;
  private final ObjectMapper objectMapper;

  private void sendCustomMustBeDisconnectedApiResponse(HttpServletResponse response, String s)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Set custom HTTP status
    response.setContentType("application/json"); // Set content type to JSON
    SWCustomError customError =
        new SWCustomError().code(SWCustomErrorCode.MUST_BE_DISCONNECTED).message(s);
    response.getWriter().write(objectMapper.writeValueAsString(customError));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Skip filtering for /public/** endpoints
    String requestURI = request.getRequestURI();
    if (requestURI.startsWith("/public/")) {
      filterChain.doFilter(request, response);
      return;
    }

    String accessToken = jwtUtil.resolveToken(request);
    log.debug("token={} ", accessToken);
    Optional<AuthTokenEntity> authTokenEntity = authTokenRepository.findByToken(accessToken);
    if (accessToken == null || authTokenEntity.isEmpty()) {
      sendCustomMustBeDisconnectedApiResponse(response, "Forbidden access");
      return; // Stop further processing in the filter chain
    }
    if (!authTokenEntity.get().isTokenValid()) {
      switch (authTokenEntity.get().getDisconnectionReasonType()) {
        case ROLE_CHANGED -> {
          sendCustomMustBeDisconnectedApiResponse(
              response, "Your role has been updated by an administrator, please login again");
          return; // Stop further processing in the filter chain
        }
        case DEACTIVATE -> {
          sendCustomMustBeDisconnectedApiResponse(
              response, "Your access has been deactivated by an administrator");
          return; // Stop further processing in the filter chain
        }
        case CONNECTED_OTHER_DEVICE -> {
          sendCustomMustBeDisconnectedApiResponse(
              response, "Your access has been used on another media, please login again");
          return; // Stop further processing in the filter chain
        }
        case LOGOUT -> {
          sendCustomMustBeDisconnectedApiResponse(
              response, "You have been logged out, please login again");
          return; // Stop further processing in the filter chain
        }
        default ->
            throw new IllegalStateException(
                "Unexpected value: " + authTokenEntity.get().getDisconnectionReasonType());
      }
    }

    Claims claims;
    try {
      claims = jwtUtil.resolveClaims(request);
    } catch (Exception e) {
      sendCustomMustBeDisconnectedApiResponse(
          response, "Your access has expired, please login again");
      return; // Stop further processing in the filter chain
    }
    if (claims != null) {
      try {
        jwtUtil.validateClaims(claims);
      } catch (Exception e) {
        sendCustomMustBeDisconnectedApiResponse(
            response, "Your access has expired, please login again");
        return; // Stop further processing in the filter chain
      }
      String email = claims.getSubject();
      log.debug("Requesting as: " + email);
      Optional<UserEntity> user = userRepository.findByLoginIgnoreCase(email);
      if (user.isPresent()) {
        if (BooleanUtils.isTrue(user.get().isActive())) {
          if (user.get().getType().equals(UserType.USERNAME)
              || (user.get().getType().equals(UserType.EMAIL_ADDRESS)
                  && BooleanUtils.isTrue(user.get().isRegisteredEmail()))) {
            Authentication authentication =
                new CustomUsernamePasswordAuthenticationToken(
                    email,
                    "",
                    List.of(new SimpleGrantedAuthority(user.get().getRole().getSecurityValue())),
                    user.get().getUserId(),
                    user.get().getLogin());
            SecurityContextHolder.getContext().setAuthentication(authentication);
          }
        }
      } else {
        sendCustomMustBeDisconnectedApiResponse(response, "Your access does not exist");
        return; // Stop further processing in the filter chain
      }
    } else {
      sendCustomMustBeDisconnectedApiResponse(response, "Your access is invalid");
      return; // Stop further processing in the filter chain
    }
    filterChain.doFilter(request, response);
  }
}
