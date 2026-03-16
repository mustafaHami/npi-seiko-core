package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "process")
@EqualsAndHashCode(of = "processId")
public class ProcessEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID processId = UUID.randomUUID();

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "currency_id", nullable = false)
  private CurrencyEntity currency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal costPerMinute;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal dysonCycleTimeInSeconds;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal nonDysonCycleTimeInSeconds;

  @NotNull
  @Column(nullable = false)
  private boolean setupProcess = false;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archiveDate;

  public void setArchived(boolean archived) {
    this.archived = archived;
    if (archived) {
      archiveDate = TimeUtils.nowOffsetDateTimeUTC();
    }
  }

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(name)) {
      sb.append(name);
      sb.append(" ");
    }
    if (currency != null && StringUtils.isNotBlank(currency.getCode())) {
      sb.append(currency.getCode());
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
