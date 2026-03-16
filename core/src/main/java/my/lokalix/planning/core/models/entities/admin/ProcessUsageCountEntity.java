package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "process_usage_count")
@EqualsAndHashCode(of = "processId")
public class ProcessUsageCountEntity {
  @Setter(AccessLevel.NONE)
  @Id
  @Column(name = "process_id")
  private UUID processId;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "process_id", nullable = false)
  private ProcessEntity process;

  @Column(nullable = false)
  private int usageCount = 0;
}
