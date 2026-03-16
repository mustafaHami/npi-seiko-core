package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;
import my.lokalix.planning.core.models.enums.UserRole;
import my.lokalix.planning.core.models.enums.UserType;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "users")
@ToString(of = "login")
@EqualsAndHashCode(of = "userId")
public class UserEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private UUID userId = UUID.randomUUID();

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserType type;

  @Column(nullable = false, unique = true)
  private String login;

  private String password;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserRole role;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean registeredEmail = false;

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(login)) {
      sb.append(login);
      sb.append(" ");
    }
    if (role != null) {
      sb.append(role.getValue());
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
