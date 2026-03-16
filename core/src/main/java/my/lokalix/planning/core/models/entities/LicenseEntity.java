package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "license")
@ToString(of = "licenseId")
public class LicenseEntity {

  // Assumption: always only 1 row in this table
  @Setter(AccessLevel.NONE)
  @Id
  private UUID licenseId = UUID.randomUUID();

  @Column(nullable = false)
  private String allowedNumberOfActiveUsers;

  @Column(nullable = false)
  private OffsetDateTime lastUpdate;
}
