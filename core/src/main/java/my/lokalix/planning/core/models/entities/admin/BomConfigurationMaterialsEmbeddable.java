package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class BomConfigurationMaterialsEmbeddable {
  @Column(nullable = false)
  private int sheetNumber;

  @Column(nullable = false)
  private int startAtRowNumber;

  private Integer manufacturerNameColumnNumber;
  private Integer manufacturerPartNumberColumnNumber;
  private Integer descriptionColumnNumber;
  private Integer quantityColumnNumber;
  private Integer unitColumnNumber;
}
