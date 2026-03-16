package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "cost_request_frozen_shipment_location")
@EqualsAndHashCode(of = "costRequestFrozenShipmentLocationId")
public class CostRequestFrozenShipmentLocationEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private UUID costRequestFrozenShipmentLocationId = UUID.randomUUID();

  @NotNull
  @ManyToOne
  @JoinColumn(name = "cost_request_id", nullable = false)
  private CostRequestEntity costRequest;

  @NotBlank
  @Column(nullable = false)
  private String currencyCode;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shipment_location_id", nullable = false)
  private ShipmentLocationEntity shipmentLocation;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archiveDate;

  @Column(nullable = false)
  private boolean masked = false;

  public void setArchived(boolean archived) {
    this.archived = archived;
    if (archived) {
      archiveDate = TimeUtils.nowOffsetDateTimeUTC();
    }
  }
}
