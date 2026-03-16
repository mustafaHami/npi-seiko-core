package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "shipment_location")
@EqualsAndHashCode(of = "shipmentLocationId")
public class ShipmentLocationEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID shipmentLocationId = UUID.randomUUID();

  @NotBlank
  @Column(nullable = false)
  private String name;

  @ManyToMany
  @JoinTable(
      name = "shipment_location_accepted_currencies",
      joinColumns = @JoinColumn(name = "shipment_location_id"),
      inverseJoinColumns = @JoinColumn(name = "currency_id"))
  private Set<CurrencyEntity> acceptedCurrencies = new HashSet<>();

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

  public void clearAcceptedCurrencies() {
    acceptedCurrencies.clear();
  }

  public void addAcceptedCurrency(CurrencyEntity currencyEntity) {
    acceptedCurrencies.add(currencyEntity);
  }

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
    searchableConcatenatedFields = sb.toString().trim();
  }
}
