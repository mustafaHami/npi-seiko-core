package my.lokalix.planning.core.models.entities;

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
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.enums.OutsourcingStatus;
import my.lokalix.planning.core.models.interfaces.FileInterface;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "tooling_cost_line")
@EqualsAndHashCode(of = "toolingCostLineId")
public class ToolingCostLineEntity implements FileInterface {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID toolingCostLineId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_line_id", nullable = false)
  private CostRequestLineEntity costRequestLine;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal quantity;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "currency_id", nullable = false)
  private CurrencyEntity currency;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @Column(nullable = false, name = "index_id")
  private int indexId;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal unitCostInCurrency;

  @NotNull
  @Column(nullable = false, precision = 25, scale = 6)
  private BigDecimal totalCostInSystemCurrency;

  @Column(nullable = false)
  private boolean outsourced = false;

  @Enumerated(EnumType.STRING)
  private OutsourcingStatus outsourcingStatus;

  private String toolingPartNumber;

  @Column(columnDefinition = "TEXT")
  private String rejectReason;

  @Column(columnDefinition = "TEXT")
  private String searchableConcatenatedFields;

  @OneToMany(mappedBy = "toolingCostLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<FileInfoEntity> attachedFiles = new ArrayList<>();

  @OneToMany(mappedBy = "toolingCostLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<ToolingCostLinePerCostRequestQuantityEntity>
      toolingCostLineForCostRequestQuantities = new ArrayList<>();

  @OneToMany(mappedBy = "toolingCostLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<MessageEntity> messages = new ArrayList<>();

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(name)) {
      sb.append(name).append(" ");
    }
    if (StringUtils.isNotBlank(toolingPartNumber)) {
      sb.append(toolingPartNumber).append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }

  public void addMessage(MessageEntity message) {
    message.setIndexId(messages.size());
    message.setToolingCostLine(this);
    messages.add(message);
  }

  public void addToolingCostLinePerCostRequestQuantity(
      ToolingCostLinePerCostRequestQuantityEntity entity) {
    entity.setIndexId(toolingCostLineForCostRequestQuantities.size());
    entity.setToolingCostLine(this);
    toolingCostLineForCostRequestQuantities.add(entity);
  }

  public void clearToolingCostLinePerCostRequestQuantities() {
    this.toolingCostLineForCostRequestQuantities.clear();
  }

  public void addAttachedFile(FileInfoEntity fileInfoEntity) {
    fileInfoEntity.setIndexId(attachedFiles.size());
    fileInfoEntity.setToolingCostLine(this);
    attachedFiles.add(fileInfoEntity);
  }

  public void removeAttachedFile(FileInfoEntity file) {
    attachedFiles.remove(file);
    file.setToolingCostLine(null);
    reindexAttachedFiles();
  }

  private void reindexAttachedFiles() {
    for (int i = 0; i < attachedFiles.size(); i++) {
      attachedFiles.get(i).setIndexId(i);
    }
  }

  public void buildCalculatedFields(String systemCurrencyCode) {
    BigDecimal exchangeRateToSystemCurrency = currency.findExchangeRate(systemCurrencyCode);
    totalCostInSystemCurrency =
        unitCostInCurrency.multiply(quantity).multiply(exchangeRateToSystemCurrency);
  }
}
