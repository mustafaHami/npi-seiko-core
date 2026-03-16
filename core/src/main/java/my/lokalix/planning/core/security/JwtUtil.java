package my.lokalix.planning.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import my.lokalix.planning.core.configurations.AppConfigurationProperties;
import my.lokalix.planning.core.models.entities.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final AppConfigurationProperties appConfigurationProperties;

  private final JwtParser jwtParser;
  private final String TOKEN_HEADER = "Authorization";
  private final String TOKEN_PREFIX = "Bearer ";
  private final long accessTokenValidityInMilliseconds = 24 * 60 * 60 * 1000; // 24 hours

  public JwtUtil(AppConfigurationProperties appConfigurationProperties) {
    this.appConfigurationProperties = appConfigurationProperties;
    this.jwtParser = Jwts.parser().verifyWith(getSigningKey()).build();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(appConfigurationProperties.getJwtSecretKey());
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String createToken(UserEntity user) {
    Claims claims = Jwts.claims().add("role", user.getRole()).subject(user.getLogin()).build();
    Date tokenCreateTime = new Date();
    Date tokenValidity =
        new Date(
            tokenCreateTime.getTime()
                + TimeUnit.MILLISECONDS.toMillis(accessTokenValidityInMilliseconds));
    return Jwts.builder()
        .claims(claims)
        .expiration(tokenValidity)
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  private Claims parseJwtClaims(String token) {
    return jwtParser.parseSignedClaims(token).getPayload();
  }

  public Claims resolveClaims(HttpServletRequest request) {
    try {
      String token = resolveToken(request);
      if (token != null) {
        return parseJwtClaims(token);
      }
      return null;
    } catch (ExpiredJwtException ex) {
      request.setAttribute("expired", ex.getMessage());
      throw ex;
    } catch (Exception ex) {
      request.setAttribute("invalid", ex.getMessage());
      throw ex;
    }
  }

  public String resolveToken(HttpServletRequest request) {

    String bearerToken = request.getHeader(TOKEN_HEADER);
    if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
      return bearerToken.substring(TOKEN_PREFIX.length());
    }
    return null;
  }

  public boolean validateClaims(Claims claims) {
    return claims.getExpiration().after(new Date());
  }

  public String getEmail(Claims claims) {
    return claims.getSubject();
  }

  private List<String> getRole(Claims claims) {
    return (List<String>) claims.get("role");
  }
}
