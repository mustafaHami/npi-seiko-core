package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import my.lokalix.planning.core.models.enums.MaterialStatus;
import my.lokalix.planning.core.models.enums.MaterialType;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "material")
@EqualsAndHashCode(of = "materialId")
public class MaterialEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID materialId = UUID.randomUUID();

  // Can be null when coming from draft
  private String systemId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manufacturer_id")
  private SupplierManufacturerEntity manufacturer;

  private String draftManufacturerName;
  private String draftCategoryName;
  private String draftUnitName;

  @NotBlank
  @Column(nullable = false)
  private String manufacturerPartNumber;

  @Column(columnDefinition = "TEXT")
  private String description;

  // Can be null when coming from draft
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private MaterialCategoryEntity category;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MaterialType materialType;

  @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MaterialSupplierEntity> suppliers = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id")
  private UnitEntity unit;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MaterialStatus status = MaterialStatus.TO_BE_ESTIMATED;

  private String estimatedBy;

  @Column(nullable = false)
  private Integer nbMessages = 0;

  @Column(columnDefinition = "TEXT")
  private String remarks;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archiveDate;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @NotNull
  @Column(nullable = false)
  private OffsetDateTime updatedAt = TimeUtils.nowOffsetDateTimeUTC();

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  public void setArchived(boolean archived) {
    this.archived = archived;
    if (archived) {
      archiveDate = TimeUtils.nowOffsetDateTimeUTC();
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
    sb.append(manufacturerPartNumber);
    sb.append(" ");
    sb.append(status.getHumanReadableValue());
    sb.append(" ");
    if (StringUtils.isNotBlank(systemId)) {
      sb.append(systemId);
      sb.append(" ");
    }
    if (category != null) {
      sb.append(category.getName());
      sb.append(" ");
    } else if (StringUtils.isNotBlank(draftCategoryName)) {
      sb.append(draftCategoryName);
      sb.append(" ");
    }
    if (manufacturer != null) {
      sb.append(manufacturer.getName());
      sb.append(" ");
    } else if (StringUtils.isNotBlank(draftManufacturerName)) {
      sb.append(draftManufacturerName);
      sb.append(" ");
    }
    if (StringUtils.isNotBlank(description)) {
      sb.append(description);
      sb.append(" ");
    }
    if (unit != null) {
      sb.append(unit.getName());
      sb.append(" ");
    } else if (StringUtils.isNotBlank(draftUnitName)) {
      sb.append(draftUnitName);
      sb.append(" ");
    }
    if (CollectionUtils.isNotEmpty(suppliers)) {
      for (MaterialSupplierEntity supplier : suppliers) {
        sb.append(supplier.getSupplier().getName());
        sb.append(" ");
      }
    }
    searchableConcatenatedFields = sb.toString().trim();
  }

  public void addMaterialSupplier(MaterialSupplierEntity supplierEntity) {
    supplierEntity.setIndexId(suppliers.size());
    supplierEntity.setMaterial(this);
    suppliers.add(supplierEntity);
  }

  public void removeMaterialSupplier(@NotNull MaterialSupplierEntity entity) {
    suppliers.remove(entity);
    entity.setMaterial(null);
    reindexLines();
  }

  private void reindexLines() {
    for (int i = 0; i < suppliers.size(); i++) {
      suppliers.get(i).setIndexId(i);
    }
  }
}
