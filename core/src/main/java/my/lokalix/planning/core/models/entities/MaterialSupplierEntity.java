package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.entities.admin.CurrencyEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "material_supplier")
@EqualsAndHashCode(of = "materialSupplierId")
public class MaterialSupplierEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID materialSupplierId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_id", nullable = false)
  private MaterialEntity material;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  private SupplierManufacturerEntity supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchasing_currency_id", nullable = false)
  private CurrencyEntity purchasingCurrency;

  @OneToMany(mappedBy = "materialSupplier", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<MaterialSupplierMoqLineEntity> moqLines = new ArrayList<>();

  private boolean defaultSupplier = false;

  @NotNull
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields = "";

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @Column(nullable = false, name = "index_id")
  private int indexId;

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (supplier != null && StringUtils.isNotBlank(supplier.getName())) {
      sb.append(supplier.getName());
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }

  public void addMoqLine(MaterialSupplierMoqLineEntity moqLine) {
    moqLine.setIndexId(moqLines.size());
    moqLine.setMaterialSupplier(this);
    moqLines.add(moqLine);
  }
}
