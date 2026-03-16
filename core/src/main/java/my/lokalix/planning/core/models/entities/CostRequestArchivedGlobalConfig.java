package my.lokalix.planning.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.enums.MarkupApprovalStrategy;

@Getter
@Setter
@Embeddable
public class CostRequestArchivedGlobalConfig {

  @Column(precision = 25, scale = 6)
  private BigDecimal laborCost;

  @Column(precision = 25, scale = 6)
  private BigDecimal overheadCost;

  @Column(precision = 25, scale = 6)
  private BigDecimal internalTransportation;

  @Column(precision = 25, scale = 6)
  private BigDecimal depreciationCost;

  @Column(precision = 25, scale = 6)
  private BigDecimal administrationCost;

  @Column(precision = 25, scale = 6)
  private BigDecimal standardJigsAndFixturesCost;

  @Column(precision = 25, scale = 6)
  private BigDecimal smallPackagingCost;

  @Column(precision = 25, scale = 6)
  private BigDecimal largePackagingCost;

  @Enumerated(EnumType.STRING)
  private MarkupApprovalStrategy markupApprovalStrategy;

  @Column(precision = 25, scale = 6)
  private BigDecimal baseMarkup;

  @Column(precision = 25, scale = 6)
  private BigDecimal markupRange;

  @Column(precision = 25, scale = 6)
  private BigDecimal budgetaryAdditionalRate;

  @Column(precision = 25, scale = 6)
  private BigDecimal npiProcessesAdditionalRate;

  @Column(precision = 25, scale = 6)
  private BigDecimal yieldPercentage;
}
