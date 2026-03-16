package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "message")
public class MessageEntity {
  @Setter(AccessLevel.NONE)
  @Id
  @GeneratedValue
  private UUID messageId;

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = OffsetDateTime.now(ZoneOffset.UTC);

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(nullable = false)
  private boolean deleted = false;

  @Column(nullable = false)
  private boolean updated = false;

  private OffsetDateTime lastModificationDate;

  /**
   * ID to link duplicate messages between different parents All messages created at the same time
   * will have the same correlationId
   */
  @Column(name = "correlation_id")
  private UUID correlationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_id")
  private CostRequestEntity costRequest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_line_id")
  private CostRequestLineEntity costRequestLine;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tooling_cost_line_id")
  private ToolingCostLineEntity toolingCostLine;

  @Column(nullable = false, name = "index_id")
  private int indexId;
}
