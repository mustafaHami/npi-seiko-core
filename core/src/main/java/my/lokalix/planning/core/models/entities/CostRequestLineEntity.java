package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.ProductNameEntity;
import my.lokalix.planning.core.models.enums.CostRequestStatus;
import my.lokalix.planning.core.models.enums.CostingMethodType;
import my.lokalix.planning.core.models.enums.OutsourcingStatus;
import my.lokalix.planning.core.models.enums.ToolingStrategy;
import my.lokalix.planning.core.models.interfaces.FileInterface;
import my.lokalix.planning.core.utils.TextUtils;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "cost_request_line")
@EqualsAndHashCode(of = "costRequestLineId")
public class CostRequestLineEntity implements FileInterface {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID costRequestLineId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_id", nullable = false)
  private CostRequestEntity costRequest;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private CostRequestStatus status = CostRequestStatus.PENDING_INFORMATION;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private CostingMethodType costingMethodType;

  @NotBlank
  @Column(nullable = false)
  private String customerPartNumber;

  @NotBlank
  @Column(nullable = false)
  private String customerPartNumberRevision;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_name_id")
  private ProductNameEntity productName;

  // separated by GlobalConstants.SEPARATOR
  @NotBlank
  @Column(nullable = false, columnDefinition = "TEXT")
  private String quantities;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ToolingStrategy toolingStrategy = ToolingStrategy.AMORTIZED;

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<FileInfoEntity> attachedFiles = new ArrayList<>();

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @SQLRestriction("is_substitute_material = false")
  @OrderBy("indexId ASC")
  private List<MaterialLineEntity> materialLines = new ArrayList<>();

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<MaterialLineDraftEntity> draftMaterialLines = new ArrayList<>();

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<ProcessLineEntity> processLines = new ArrayList<>();

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<ToolingCostLineEntity> toolingCostLines = new ArrayList<>();

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<OtherCostLineEntity> otherCostLines = new ArrayList<>();

  @OneToMany(mappedBy = "costRequestLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<MessageEntity> messages = new ArrayList<>();

  @Column(precision = 25, scale = 6)
  private BigDecimal toolingMarkup;

  @Column(precision = 25, scale = 6)
  private BigDecimal markup;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime updatedAt = TimeUtils.nowOffsetDateTimeUTC();

  @Column(columnDefinition = "TEXT")
  private String priceRejectReason;

  @Column(nullable = false)
  private boolean outsourced = false;

  @Enumerated(EnumType.STRING)
  private OutsourcingStatus outsourcingStatus;

  @Column(precision = 25, scale = 6)
  private BigDecimal outsourcedCostInSystemCurrency;

  @Column(columnDefinition = "TEXT")
  private String outsourcingRejectReason;

  @Column(nullable = false)
  private boolean reverted = false;

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  @Column(nullable = false, name = "index_id")
  private int indexId;

  public List<MaterialLineEntity> getOnlyMaterialLinesUsedForQuotation() {
    return this.materialLines.stream()
        .filter(ml -> !ml.isMarkedNotUsedForQuote())
        .collect(Collectors.toList());
  }

  public void addMessage(MessageEntity message) {
    message.setIndexId(messages.size());
    message.setCostRequestLine(this);
    messages.add(message);
  }

  public List<Integer> getQuantitiesAsList() {
    return TextUtils.splitConcatenatedListWithSeparator(this.quantities, Integer::valueOf);
  }

  public void addAttachedFile(FileInfoEntity fileInfoEntity) {
    fileInfoEntity.setIndexId(attachedFiles.size());
    fileInfoEntity.setCostRequestLine(this);
    attachedFiles.add(fileInfoEntity);
  }

  public void removeAttachedFile(FileInfoEntity file) {
    attachedFiles.remove(file);
    file.setCostRequestLine(null);
    reindexAttachedFiles();
  }

  private void reindexAttachedFiles() {
    for (int i = 0; i < attachedFiles.size(); i++) {
      attachedFiles.get(i).setIndexId(i);
    }
  }

  public void addProcessLine(ProcessLineEntity processLineEntity) {
    processLineEntity.setIndexId(processLines.size());
    processLineEntity.setCostRequestLine(this);
    processLines.add(processLineEntity);
  }

  public void removeProcessLine(ProcessLineEntity processLine) {
    processLines.remove(processLine);
    processLine.setCostRequestLine(null);
    reindexProcessLines();
  }

  private void reindexProcessLines() {
    for (int i = 0; i < processLines.size(); i++) {
      processLines.get(i).setIndexId(i);
    }
  }

  public void addOtherCostLine(OtherCostLineEntity otherCostLine) {
    otherCostLine.setIndexId(otherCostLines.size());
    otherCostLine.setCostRequestLine(this);
    otherCostLines.add(otherCostLine);
  }

  public void removeOtherCostLine(OtherCostLineEntity otherCostLine) {
    otherCostLines.remove(otherCostLine);
    otherCostLine.setCostRequestLine(null);
    reindexOtherCostLines();
  }

  private void reindexOtherCostLines() {
    for (int i = 0; i < otherCostLines.size(); i++) {
      otherCostLines.get(i).setIndexId(i);
    }
  }

  public void addToolingCostLine(ToolingCostLineEntity toolingCostLine) {
    toolingCostLine.setIndexId(toolingCostLines.size());
    toolingCostLine.setCostRequestLine(this);
    toolingCostLines.add(toolingCostLine);
  }

  public void removeToolingCostLine(ToolingCostLineEntity toolingCostLine) {
    toolingCostLines.remove(toolingCostLine);
    toolingCostLine.setCostRequestLine(null);
    reindexToolingCostLines();
  }

  private void reindexToolingCostLines() {
    for (int i = 0; i < toolingCostLines.size(); i++) {
      toolingCostLines.get(i).setIndexId(i);
    }
  }

  public void addMaterialLine(MaterialLineEntity materialLineEntity) {
    materialLineEntity.setIndexId(materialLines.size());
    materialLineEntity.setCostRequestLine(this);
    materialLines.add(materialLineEntity);
  }

  public void removeMaterialLine(MaterialLineEntity materialLineEntity) {
    materialLines.remove(materialLineEntity);
    materialLineEntity.setCostRequestLine(null);
    reindexMaterialLines();
  }

  private void reindexMaterialLines() {
    for (int i = 0; i < materialLines.size(); i++) {
      materialLines.get(i).setIndexId(i);
    }
  }

  public void addDraftMaterialLine(MaterialLineDraftEntity materialLineDraftEntity) {
    materialLineDraftEntity.setIndexId(draftMaterialLines.size());
    materialLineDraftEntity.setCostRequestLine(this);
    draftMaterialLines.add(materialLineDraftEntity);
  }

  public void removeDraftMaterialLine(MaterialLineDraftEntity materialLineDraftEntity) {
    draftMaterialLines.remove(materialLineDraftEntity);
    materialLineDraftEntity.setCostRequestLine(null);
    reindexDraftMaterialLines();
  }

  private void reindexDraftMaterialLines() {
    for (int i = 0; i < draftMaterialLines.size(); i++) {
      draftMaterialLines.get(i).setIndexId(i);
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
    sb.append(costRequest.getCostRequestReferenceNumber());
    sb.append(" ");
    sb.append(costRequest.getCostRequestRevision());
    sb.append(" ");
    if (costRequest.getCustomer() != null) {
      sb.append(costRequest.getCustomer().getName());
      sb.append(" ");
      sb.append(costRequest.getCustomer().getCode());
      sb.append(" ");
    }

    if (StringUtils.isNotBlank(customerPartNumber)) {
      sb.append(customerPartNumber);
      sb.append(" ");
    }
    if (StringUtils.isNotBlank(customerPartNumberRevision)) {
      sb.append(customerPartNumberRevision);
      sb.append(" ");
    }
    if (StringUtils.isNotBlank(description)) {
      sb.append(description);
      sb.append(" ");
    }
    if (productName != null) {
      sb.append(productName.getName());
      sb.append(" ");
    }
    if (status != null) {
      sb.append(status.getValue());
      sb.append(" ");
    }
    if (costingMethodType != null) {
      sb.append(costingMethodType.getValue());
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }

  public void clearAllPerQuantityData() {
    this.materialLines.forEach(MaterialLineEntity::clearMaterialLinePerCostRequestQuantities);
    this.processLines.forEach(ProcessLineEntity::clearProcessLinePerCostRequestQuantities);
    this.otherCostLines.forEach(OtherCostLineEntity::clearOtherCostLinePerCostRequestQuantities);
    this.toolingCostLines.forEach(
        ToolingCostLineEntity::clearToolingCostLinePerCostRequestQuantities);
  }

    public void clearOtherCostLines() {
      otherCostLines.clear();
    }
}
