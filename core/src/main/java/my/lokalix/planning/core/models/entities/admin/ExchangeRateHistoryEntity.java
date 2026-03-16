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
@Table(name = "exchange_rate_history")
@EqualsAndHashCode(of = "exchangeRateHistoryId")
public class ExchangeRateHistoryEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private final UUID exchangeRateHistoryId = UUID.randomUUID();

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime changeDate = TimeUtils.nowOffsetDateTimeUTC();

  private String fromCurrency;
  private String toCurrency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal oldRate;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal newRate;
}
