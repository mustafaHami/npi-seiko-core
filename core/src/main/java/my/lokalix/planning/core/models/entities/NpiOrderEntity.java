package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.enums.NpiOrderStatus;
import my.lokalix.planning.core.models.enums.ProcessLineStatus;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "npi_order")
@EqualsAndHashCode(of = "npiOrderId")
public class NpiOrderEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private final UUID npiOrderId = UUID.randomUUID();

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private final OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotBlank
  @Column(nullable = false)
  private String purchaseOrderNumber;

  @NotBlank
  @Column(nullable = false)
  private String workOrderId;

  @NotBlank
  @Column(nullable = false)
  private String partNumber;

  @NotNull
  @Column(nullable = false)
  private Integer quantity;

  private LocalDate orderDate;
  private LocalDate targetDeliveryDate;
  private String customerName;
  private String productName;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private NpiOrderStatus status = NpiOrderStatus.READY_TO_PRODUCTION;

  @Column(nullable = false)
  private OffsetDateTime updatedAt = TimeUtils.nowOffsetDateTimeUTC();

  private OffsetDateTime finalizationDate;

  private LocalDate plannedDeliveryDate;
  private LocalDate forecastDeliveryDate;

  @Column(columnDefinition = "TEXT")
  private String customerRejectReason;

  private OffsetDateTime statusDate;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archivedAt;

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  @OneToMany(mappedBy = "npiOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<ProcessLineEntity> processLines = new ArrayList<>();

  public void checkIfAllLinesIsCompleted() {
    if (CollectionUtils.isEmpty(processLines)) return;
    if (processLines.stream()
        .allMatch((pl) -> pl.getStatus().equals(ProcessLineStatus.COMPLETED))) {
      setStatus(NpiOrderStatus.COMPLETED);
    }
  }

  public void setStatus(NpiOrderStatus status) {
    this.statusDate = TimeUtils.nowOffsetDateTimeUTC();
    this.status = status;
    if (status.isFinalStatus()) {
      setFinalizationDate(TimeUtils.nowOffsetDateTimeUTC());
    }
  }

  public void addProcessLine(@NotNull ProcessLineEntity line) {
    line.setIndexId(processLines.size());
    line.setNpiOrder(this);
    processLines.add(line);
  }

  public void removeProcessLine(@NotNull ProcessLineEntity line) {
    processLines.remove(line);
    line.setNpiOrder(null);
    reindexLines();
  }

  private void reindexLines() {
    for (int i = 0; i < processLines.size(); i++) {
      processLines.get(i).setIndexId(i);
    }
  }

  @PrePersist
  @PreUpdate
  private void onSave() {
    updatedAt = TimeUtils.nowOffsetDateTimeUTC();
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(purchaseOrderNumber)) {
      sb.append(purchaseOrderNumber).append(" ");
    }
    if (StringUtils.isNotBlank(workOrderId)) {
      sb.append(workOrderId).append(" ");
    }
    if (StringUtils.isNotBlank(partNumber)) {
      sb.append(partNumber).append(" ");
    }
    if (StringUtils.isNotBlank(customerName)) {
      sb.append(customerName).append(" ");
    }
    if (StringUtils.isNotBlank(productName)) {
      sb.append(productName).append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
