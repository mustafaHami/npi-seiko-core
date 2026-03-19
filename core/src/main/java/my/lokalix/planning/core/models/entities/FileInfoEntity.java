package my.lokalix.planning.core.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import my.lokalix.planning.core.models.enums.FileType;

@Getter
@Setter
@Entity
@Table(name = "file_info")
@EqualsAndHashCode(of = "fileId")
public class FileInfoEntity {

  @Setter(AccessLevel.NONE)
  @Id
  private UUID fileId = UUID.randomUUID();

  @NotBlank
  @Column(columnDefinition = "TEXT", nullable = false)
  private String fileName;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private FileType type = FileType.ANY;

  @ManyToOne
  @JoinColumn(name = "process_line_id")
  private ProcessLineEntity processLine;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "npi_order_id")
  private NpiOrderEntity npiOrder;

  @Column(nullable = false, name = "index_id")
  private int indexId;
}
