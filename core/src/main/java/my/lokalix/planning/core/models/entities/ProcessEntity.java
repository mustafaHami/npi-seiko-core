package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "process")
@EqualsAndHashCode(of = "processId")
public class ProcessEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private final UUID processId = UUID.randomUUID();

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @Column(nullable = false)
  private Boolean hasPlanTime = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isMaterialPurchase = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isMaterialReceiving = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isProduction = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isTesting = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isShipment = false;

  @NotNull
  @Column(nullable = false)
  private Boolean isCustomerApproval = false;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();
}
