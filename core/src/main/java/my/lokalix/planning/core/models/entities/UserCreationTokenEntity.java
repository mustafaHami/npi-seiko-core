package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "account_creation_token")
@ToString(of = "token")
public class UserCreationTokenEntity {

  @Id
  @Column(columnDefinition = "TEXT", nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private OffsetDateTime creationDate;
}
