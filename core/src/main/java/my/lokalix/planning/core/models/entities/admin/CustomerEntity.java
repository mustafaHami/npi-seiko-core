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
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "customer")
@EqualsAndHashCode(of = "customerId")
public class CustomerEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID customerId = UUID.randomUUID();

  @NotBlank
  @Column(nullable = false)
  private String code;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @Column(nullable = false)
  private boolean dyson = false;

  @Column(precision = 25, scale = 6)
  private BigDecimal markup;

  private String paymentTerms;

  // separated by GlobalConstants.SEPARATOR
  @Column(columnDefinition = "TEXT")
  private String requestorNames;

  // separated by GlobalConstants.SEPARATOR
  @Column(columnDefinition = "TEXT")
  private String customerEmails;

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

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<CustomerShipmentLocationEntity> shipmentLocations = new ArrayList<>();

  public void addShipmentLocation(CustomerShipmentLocationEntity customerShipmentLocation) {
    customerShipmentLocation.setIndexId(shipmentLocations.size());
    customerShipmentLocation.setCustomer(this);
    shipmentLocations.add(customerShipmentLocation);
  }

  public void removeShipmentLocation(CustomerShipmentLocationEntity entity) {
    shipmentLocations.remove(entity);
    entity.setCustomer(null);
    reindexShipmentLocations();
  }

  private void reindexShipmentLocations() {
    for (int i = 0; i < shipmentLocations.size(); i++) {
      shipmentLocations.get(i).setIndexId(i);
    }
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
    if (StringUtils.isNotBlank(code)) {
      sb.append(code);
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
