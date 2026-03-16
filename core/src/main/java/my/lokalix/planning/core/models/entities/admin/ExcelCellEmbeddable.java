package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ExcelCellEmbeddable {
  @Column(nullable = false)
  private int sheetNumber;

  @Column(nullable = false)
  private int columnNumber;

  @Column(nullable = false)
  private int rowNumber;
}
