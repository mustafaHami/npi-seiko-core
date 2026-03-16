package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.CustomerEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.FileType;
import my.lokalix.planning.core.models.interfaces.FileInterface;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.collections4.CollectionUtils;

@Getter
@Setter
@Entity
@Table(name = "cost_request")
@EqualsAndHashCode(of = "costRequestId")
public class CostRequestEntity implements FileInterface {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID costRequestId = UUID.randomUUID();

  // when cloning or creating new revision, this field
  // allows me to know which quotation is, is unique
  @NotBlank
  @Column(nullable = false)
  private String costRequestReferenceNumber;

  @Column(nullable = false)
  private int costRequestRevision = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private CustomerEntity customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "currency_id")
  private CurrencyEntity currency;

  private String requestorName;

  // separated by GlobalConstants.SEPARATOR
  @Column(columnDefinition = "TEXT")
  private String customerEmails;

  @NotBlank
  @Column(nullable = false)
  private String projectName;

  @NotNull
  @Column(nullable = false)
  private LocalDate purchaseOrderExpectedDate;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private CostRequestStatus status = CostRequestStatus.PENDING_INFORMATION;

  @OneToMany(mappedBy = "costRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<CostRequestLineEntity> lines = new ArrayList<>();

  @OneToMany(mappedBy = "costRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<FileInfoEntity> attachedFiles = new ArrayList<>();

  @Column(precision = 25, scale = 6)
  private BigDecimal totalLinesCostInSystemCurrency;

  @OneToMany(mappedBy = "costRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<MessageEntity> messages = new ArrayList<>();

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  private OffsetDateTime finalizationDate;

  private OffsetDateTime activeStatusDate;

  private LocalDate expirationDate;

  private String rejectByCustomerReason;

  private String clonedFromReferenceNumber;

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archiveDate;

  @Embedded
  private CostRequestArchivedGlobalConfig archivedGlobalConfig =
      new CostRequestArchivedGlobalConfig();

  @OneToMany(mappedBy = "costRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<CostRequestFrozenShipmentLocationEntity> frozenShipmentLocations = new ArrayList<>();

  public void addFrozenShipmentLocation(
      CostRequestFrozenShipmentLocationEntity frozenShipmentLocation) {
    frozenShipmentLocation.setIndexId(frozenShipmentLocations.size());
    frozenShipmentLocation.setCostRequest(this);
    frozenShipmentLocations.add(frozenShipmentLocation);
  }

  public boolean isExpired() {
    if (status.isFinalStatus()) return false;
    return expirationDate != null && LocalDate.now().isAfter(expirationDate);
  }

  public CostRequestArchivedGlobalConfig getArchivedGlobalConfig() {
    if (archivedGlobalConfig == null) {
      archivedGlobalConfig = new CostRequestArchivedGlobalConfig();
    }
    return archivedGlobalConfig;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
    if (archived) {
      archiveDate = TimeUtils.nowOffsetDateTimeUTC();
    }
  }

  public void addMessage(MessageEntity message) {
    message.setIndexId(messages.size());
    message.setCostRequest(this);
    messages.add(message);
  }

  public void addLine(@NotNull CostRequestLineEntity line) {
    line.setIndexId(lines.size());
    line.setCostRequest(this);
    lines.add(line);
  }

  public void removeLine(@NotNull CostRequestLineEntity line) {
    lines.remove(line);
    line.setCostRequest(null);
    reindexLines();
  }

  private void reindexLines() {
    for (int i = 0; i < lines.size(); i++) {
      lines.get(i).setIndexId(i);
    }
  }

  public void addAttachedFile(FileInfoEntity fileInfoEntity) {
    fileInfoEntity.setIndexId(attachedFiles.size());
    fileInfoEntity.setCostRequest(this);
    attachedFiles.add(fileInfoEntity);
  }

  public void removeAttachedFile(FileInfoEntity file) {
    attachedFiles.remove(file);
    file.setCostRequest(null);
    reindexAttachedFiles();
  }

  private void reindexAttachedFiles() {
    for (int i = 0; i < attachedFiles.size(); i++) {
      attachedFiles.get(i).setIndexId(i);
    }
  }

  public void setStatus(CostRequestStatus status) {
    if (this.status == status) return;
    this.status = status;
    if (status == CostRequestStatus.ACTIVE) {
      this.activeStatusDate = TimeUtils.nowOffsetDateTimeUTC();
    }
    if (status == CostRequestStatus.ABORTED
        || status == CostRequestStatus.WON
        || status == CostRequestStatus.LOST
        || status == CostRequestStatus.NEW_REVISION_CREATED) {
      finalizationDate = TimeUtils.nowOffsetDateTimeUTC();
    }
    if (status == CostRequestStatus.ABORTED
        || status == CostRequestStatus.READY_FOR_REVIEW
        || status == CostRequestStatus.READY_TO_QUOTE
        || status == CostRequestStatus.WON
        || status == CostRequestStatus.LOST
        || status == CostRequestStatus.NEW_REVISION_CREATED
        || status == CostRequestStatus.ACTIVE) {
      if (CollectionUtils.isNotEmpty(lines)) {
        lines.stream()
            .filter(line -> line.getStatus() != CostRequestStatus.ABORTED)
            .forEach(line -> line.setStatus(status));
      }
    }
  }

  /** Check if all lines of a cost request are in the same status or aborted */
  public boolean checkIfAllLinesHaveTheStatusesOrAreAborted(List<CostRequestStatus> lineStatuses) {
    if (CollectionUtils.isEmpty(this.getLines())) return true;
    return this.getLines().stream()
        .filter(line -> line.getStatus() != CostRequestStatus.ABORTED)
        .allMatch(line -> lineStatuses.contains(line.getStatus()));
  }

  public boolean areAllLinesAborted() {
    if (CollectionUtils.isEmpty(this.getLines())) return true;
    return this.getLines().stream().allMatch(line -> line.getStatus() == CostRequestStatus.ABORTED);
  }

  /** Check if any line of a cost request is in the same status */
  public boolean isOneLineWithStatus(List<CostRequestStatus> lineStatuses) {
    if (CollectionUtils.isEmpty(this.getLines())) return false;
    return this.getLines().stream().anyMatch(line -> lineStatuses.contains(line.getStatus()));
  }

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    sb.append(costRequestReferenceNumber);
    sb.append(" ");
    sb.append(costRequestRevision);
    sb.append(" ");
    if (customer != null) {
      sb.append(customer.getCode());
      sb.append(" ");
    }
    if (currency != null) {
      sb.append(currency.getCode());
      sb.append(" ");
    }
    if (status != null) {
      sb.append(status.getValue());
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }

  public void buildCalculatedFields() {
    // TODO
    totalLinesCostInSystemCurrency = BigDecimal.ZERO;
  }

  public List<FileInfoEntity> getAttachedFilesPerFileType(FileType fileType) {
    return attachedFiles.stream().filter(fileInfo -> fileInfo.getType().equals(fileType)).toList();
  }

  public void clearFrozenShipmentLocations() {
    frozenShipmentLocations.clear();
  }
}
