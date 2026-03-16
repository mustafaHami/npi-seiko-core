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
import my.lokalix.planning.core.models.entities.admin.ShipmentLocationEntity;
import my.lokalix.planning.core.models.enums.OtherCostLineCalculationStrategy;
import my.lokalix.planning.core.models.enums.PackagingSize;
import my.lokalix.planning.core.utils.TimeUtils;

@Getter
@Setter
@Entity
@Table(name = "other_cost_line")
@EqualsAndHashCode(of = "otherCostLineId")
public class OtherCostLineEntity {
  @Setter(AccessLevel.NONE)
  @Id
  private UUID otherCostLineId = UUID.randomUUID();

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_request_line_id", nullable = false)
  private CostRequestLineEntity costRequestLine;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private boolean fixedLine = false;

  @Column(nullable = false)
  private boolean editableLine = false;

  @Column(nullable = false)
  private boolean packagingLine = false;

  @Column(nullable = false)
  private boolean shipmentToCustomerLine = false;

  @Column(nullable = false)
  private boolean masked = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shipment_location_id")
  private ShipmentLocationEntity shipmentLocation;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "currency_id", nullable = false)
  private CurrencyEntity currency;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OtherCostLineCalculationStrategy calculationStrategy;

  @Enumerated(EnumType.STRING)
  private PackagingSize packagingSize;

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

  @OneToMany(mappedBy = "otherCostLine", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexId ASC")
  private List<OtherCostLinePerCostRequestQuantityEntity> otherCostLineForCostRequestQuantities =
      new ArrayList<>();

  public void addOtherCostLinePerCostRequestQuantity(
      OtherCostLinePerCostRequestQuantityEntity entity) {
    entity.setIndexId(otherCostLineForCostRequestQuantities.size());
    entity.setOtherCostLine(this);
    otherCostLineForCostRequestQuantities.add(entity);
  }

  public void clearOtherCostLinePerCostRequestQuantities() {
    this.otherCostLineForCostRequestQuantities.clear();
  }

  public void buildCalculatedFields(String systemCurrencyCode) {
    BigDecimal exchangeRateToSystemCurrency = currency.findExchangeRate(systemCurrencyCode);
    totalCostInSystemCurrency = unitCostInCurrency.multiply(exchangeRateToSystemCurrency);
  }
}
