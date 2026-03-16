package my.lokalix.planning.core.models.entities.admin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Entity
@Table(name = "bom_configuration")
@EqualsAndHashCode(of = "bomConfigurationId")
public class BomConfigurationEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private UUID bomConfigurationId = UUID.randomUUID();

  @NotBlank
  @Column(nullable = false)
  private String name;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "sheetNumber",
        column = @Column(name = "part_number_cell_sheet_number")),
    @AttributeOverride(
        name = "columnNumber",
        column = @Column(name = "part_number_cell_column_number")),
    @AttributeOverride(name = "rowNumber", column = @Column(name = "part_number_cell_row_number"))
  })
  private ExcelCellEmbeddable partNumberCell;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "sheetNumber", column = @Column(name = "revision_cell_sheet_number")),
    @AttributeOverride(
        name = "columnNumber",
        column = @Column(name = "revision_cell_column_number")),
    @AttributeOverride(name = "rowNumber", column = @Column(name = "revision_cell_row_number"))
  })
  private ExcelCellEmbeddable revisionCell;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(
        name = "sheetNumber",
        column = @Column(name = "description_cell_sheet_number")),
    @AttributeOverride(
        name = "columnNumber",
        column = @Column(name = "description_cell_column_number")),
    @AttributeOverride(name = "rowNumber", column = @Column(name = "description_cell_row_number"))
  })
  private ExcelCellEmbeddable descriptionCell;

  @Embedded private BomConfigurationMaterialsEmbeddable materials;

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String searchableConcatenatedFields;

  @Column(nullable = false)
  private boolean archived = false;

  private OffsetDateTime archiveDate;

  @Setter(AccessLevel.NONE)
  @NotNull
  @Column(nullable = false)
  private OffsetDateTime creationDate = TimeUtils.nowOffsetDateTimeUTC();

  public void setArchived(boolean archived) {
    this.archived = archived;
    if (archived) {
      archiveDate = TimeUtils.nowOffsetDateTimeUTC();
    }
  }

  @PrePersist
  @PreUpdate
  private void onSave() {
    buildSearchableConcatenation();
  }

  public void buildSearchableConcatenation() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(name)) {
      sb.append(name);
      sb.append(" ");
    }
    searchableConcatenatedFields = sb.toString().trim();
  }
}
