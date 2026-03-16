package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "material_supplier_moq_line")
@EqualsAndHashCode(of = "materialSupplierMoqLineId")
public class MaterialSupplierMoqLineEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID materialSupplierMoqLineId = UUID.randomUUID();

  @NotNull
  @DecimalMin(value = "0", inclusive = false)
  @Column(precision = 25, scale = 6, nullable = false)
  private BigDecimal minimumOrderQuantity;

  @NotNull
  @DecimalMin(value = "0", inclusive = false)
  @Column(precision = 25, scale = 6, nullable = false)
  private BigDecimal unitPurchasingPriceInPurchasingCurrency;

  @DecimalMin(value = "0", inclusive = false)
  @Column(precision = 25, scale = 6)
  private BigDecimal standardPackagingQuantity;

  @Column(length = 500)
  private String leadTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_supplier_id", nullable = false)
  private MaterialSupplierEntity materialSupplier;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  @Column(nullable = false, name = "index_id")
  private int indexId;
}
