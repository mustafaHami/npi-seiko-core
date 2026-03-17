package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "process_line")
@EqualsAndHashCode(of = "processLineId")
public class ProcessLineEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private final UUID processLineId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "npi_order_id", nullable = false)
  private NpiOrderEntity npiOrder;

  @NotNull
  @Column(nullable = false)
  private Boolean isMaterialPurchase = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isProduction = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isTesting = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isShipment = false;

  @NotBlank
  @Column(nullable = false)
  private String processName;

  @NotNull
  @Column(nullable = false)
  private Integer orderIndex;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ProcessLineStatus status = ProcessLineStatus.NOT_STARTED;

  @Column(precision = 25, scale = 6)
  private BigDecimal planTime;

  @Column(precision = 25, scale = 6)
  private BigDecimal remainingDuration;

  private LocalDate materialLatestDeliveryDate;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();
}
