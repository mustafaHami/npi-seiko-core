package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import my.lokalix.planning.core.models.enums.ConnectionType;
import my.lokalix.planning.core.models.enums.DisconnectionReasonType;

@Getter
@Setter
@Entity
@Table(name = "auth_token")
@ToString(of = "token")
public class AuthTokenEntity {

  @Id
  @Column(columnDefinition = "TEXT", nullable = false, unique = true)
  private String token;

  @Column(name = "user_login", nullable = false)
  private String userLogin;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ConnectionType connectionType;

  @Enumerated(EnumType.STRING)
  private DisconnectionReasonType disconnectionReasonType;

  @Column(nullable = false)
  private boolean tokenValid = true;

  @Column(nullable = false)
  private LocalDate creationDate;
}
