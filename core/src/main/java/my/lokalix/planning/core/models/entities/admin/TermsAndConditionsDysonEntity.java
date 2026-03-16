package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
@Table(name = "terms_and_conditions_dyson")
@EqualsAndHashCode(of = "customerId")
public class TermsAndConditionsDysonEntity {
  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @Setter(AccessLevel.NONE)
  @Id
  @Column(name = "customer_id")
  private UUID customerId;

  @NotNull
  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "customer_id", nullable = false)
  private CustomerEntity customer;

  private Integer validityNumberOfDays;

  @Column(precision = 25, scale = 6)
  private BigDecimal currencyExchangeRate;

  private Integer minimumDeliveryQuantity;
  private Integer storageAcceptDeliveryNumberMonths;

  @Column(precision = 25, scale = 6)
  private BigDecimal storageMinimumStorageFee;

  private Integer nonCancellationNumberWorkingDays;
  private Integer nonRescheduledNumberWeeks;

  @Column(precision = 25, scale = 6)
  private BigDecimal claimsPackagingDamageNumberDays;

  private String forecastLeadTime;

  @Column(precision = 25, scale = 6)
  private BigDecimal latePaymentPenalty;

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime updatedAt = TimeUtils.nowOffsetDateTimeUTC();

  @PreUpdate
  private void onUpdate() {
    updatedAt = TimeUtils.nowOffsetDateTimeUTC();
  }
}
