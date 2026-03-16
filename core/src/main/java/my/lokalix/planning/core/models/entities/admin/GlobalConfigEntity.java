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
import my.lokalix.planning.core.models.enums.AutomaticExchangeRateFrequency;
import my.lokalix.planning.core.models.enums.CurrencyExchangeRateStrategy;
import my.lokalix.planning.core.models.enums.MarkupApprovalStrategy;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "global_config")
@EqualsAndHashCode(of = "globalConfigId")
public class GlobalConfigEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private final UUID globalConfigId = UUID.randomUUID();

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal laborCost;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal overheadCost;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal internalTransportation;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal depreciationCost;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal administrationCost;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal standardJigsAndFixturesCost;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal smallPackagingCost;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal largePackagingCost;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MarkupApprovalStrategy markupApprovalStrategy;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal baseMarkup;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal markupRange;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal costChangeAlert;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal budgetaryAdditionalRate;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal npiProcessesAdditionalRate = new BigDecimal(10);

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CurrencyExchangeRateStrategy currencyExchangeRateStrategy;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AutomaticExchangeRateFrequency automaticExchangeRateFrequency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal yieldPercentage;

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime updatedAt = TimeUtils.nowOffsetDateTimeUTC();

  @PreUpdate
  private void onUpdate() {
    updatedAt = TimeUtils.nowOffsetDateTimeUTC();
  }
}
