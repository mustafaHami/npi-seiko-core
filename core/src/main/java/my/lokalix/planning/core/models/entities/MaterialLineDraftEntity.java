package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.models.entities.admin.UnitEntity;
import my.lokalix.planning.core.models.enums.MaterialType;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "material_line_draft")
@EqualsAndHashCode(of = "materialLineDraftId")
public class MaterialLineDraftEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID materialLineDraftId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_line_id", nullable = false)
  private CostRequestLineEntity costRequestLine;

  // Material field, need to save for copy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manufacturer_id")
  private SupplierManufacturerEntity manufacturer;

  private String draftManufacturerName;
  private String draftCategoryName;
  private String draftUnitName;

  private String manufacturerPartNumber;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private MaterialCategoryEntity category;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MaterialType materialType;

  @Column(precision = 25, scale = 6)
  private BigDecimal quantity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id")
  private UnitEntity unit;

  @Column(columnDefinition = "TEXT")
  private String missingData;

  @Column(nullable = false)
  private boolean markedNotUsedForQuote = false;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @Column(nullable = false, name = "index_id")
  private int indexId;
}
