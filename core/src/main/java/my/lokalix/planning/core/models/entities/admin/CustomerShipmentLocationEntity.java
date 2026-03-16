package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "customer_shipment_location")
@EqualsAndHashCode(of = "customerShipmentLocationId")
public class CustomerShipmentLocationEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID customerShipmentLocationId = UUID.randomUUID();

  @NotNull
  @ManyToOne
  @JoinColumn(name = "customer_id", nullable = false)
  private CustomerEntity customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shipment_location_id")
  private ShipmentLocationEntity shipmentLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "currency_id")
  private CurrencyEntity currency;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archiveDate;

  public void setArchived(boolean archived) {
    this.archived = archived;
    if (archived) {
      archiveDate = TimeUtils.nowOffsetDateTimeUTC();
    }
  }
}
