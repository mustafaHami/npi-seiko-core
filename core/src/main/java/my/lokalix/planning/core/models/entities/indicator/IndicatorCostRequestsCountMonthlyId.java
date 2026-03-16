package my.lokalix.planning.core.models.entities.indicator;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Embeddable
@ToString(of = {"firstDayOfMonth"})
@EqualsAndHashCode(of = {"firstDayOfMonth", "customerName"})
public class IndicatorCostRequestsCountMonthlyId implements Serializable {

  @NotNull
  @Column(nullable = false)
  private LocalDate firstDayOfMonth;

  @NotBlank
  @Column(nullable = false)
  private String customerName;
}
