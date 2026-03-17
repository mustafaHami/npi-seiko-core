package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "process_line_status_history")
@EqualsAndHashCode(of = "processLineStatusHistoryId")
public class ProcessLineStatusHistoryEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private final UUID processLineStatusHistoryId = UUID.randomUUID();

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "process_line_id", nullable = false)
  private ProcessLineEntity processLine;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ProcessLineStatus status;

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime startDate;

  private OffsetDateTime endDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "by_user_id")
  private UserEntity user;
}
