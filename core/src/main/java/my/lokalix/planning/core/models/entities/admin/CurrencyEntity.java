package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.exceptions.GenericWithMessageException;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "currency")
@EqualsAndHashCode(of = "currencyId")
public class CurrencyEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID currencyId = UUID.randomUUID();

  @NotBlank
  @Column(nullable = false)
  private String code;

  @OneToMany(mappedBy = "fromCurrency", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExchangeRateEntity> exchangeRates = new ArrayList<>();

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archivedAt;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  public BigDecimal findExchangeRate(String targetCurrencyCode) {
    if (code.equalsIgnoreCase(targetCurrencyCode)) {
      return BigDecimal.ONE;
    }
    return exchangeRates.stream()
        .filter(er -> er.getToCurrency().getCode().equalsIgnoreCase(targetCurrencyCode))
        .findFirst()
        .map(ExchangeRateEntity::getRate)
        .orElseThrow(
            () ->
                new GenericWithMessageException(
                    "Exchange rate not found from " + code + " to " + targetCurrencyCode));
  }

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(code)) {
      sb.append(code);
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
