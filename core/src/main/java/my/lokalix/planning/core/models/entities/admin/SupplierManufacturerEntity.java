package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.enums.SupplierAndManufacturerType;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "supplier_manufacturer")
@EqualsAndHashCode(of = "supplierManufacturerId")
public class SupplierManufacturerEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID supplierManufacturerId = UUID.randomUUID();

  private String name;

  private String code;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SupplierAndManufacturerType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shipment_method_id")
  private ShipmentMethodEntity shipmentMethod;

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
    sb.append(type.getValue());
    sb.append(" ");
    if (StringUtils.isNotBlank(name)) {
      sb.append(name);
      sb.append(" ");
    }
    if (StringUtils.isNotBlank(code)) {
      sb.append(code);
      sb.append(" ");
    }
    if (shipmentMethod != null) {
      sb.append(shipmentMethod.getName());
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
