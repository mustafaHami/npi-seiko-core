package my.lokalix.planning.core.handlers;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import my.lokalix.planning.core.security.LoggedUserDetailsService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
@Component
public class RequestLoggingFilter extends GenericFilterBean {

  private final String LOGGING_ALREADY_DONE = "loggingAlreadyDone";
  private final LoggedUserDetailsService loggedUserDetailsService;

  public RequestLoggingFilter(LoggedUserDetailsService loggedUserDetailsService) {
    this.loggedUserDetailsService = loggedUserDetailsService;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;

    // Check if the request was already processed by this logging filter
    if (request.getAttribute(LOGGING_ALREADY_DONE) != null) {
      chain.doFilter(req, res);
      return;
    }
    String requestURI = request.getRequestURI();
    String method = request.getMethod();

    // Check if there is authenticated user first
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
      // Log the details
      log.info(
          "User {} made {} request to {}",
          loggedUserDetailsService.getLoggedUserLogin(),
          method,
          requestURI);
    } else {
      log.info("Unauthenticated user made {} request to {}", method, requestURI);
    }
    // Mark this request as already processed by logger, to avoid re-logging the same request
    request.setAttribute(LOGGING_ALREADY_DONE, true);
    chain.doFilter(req, res);
  }
}
