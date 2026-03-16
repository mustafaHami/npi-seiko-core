package my.lokalix.planning.core.services.imports;

import java.time.OffsetDateTime;
import lombok.Value;

@Value
public class CostRequestImportLine {
  int index;
  String customer;
  OffsetDateTime createdOn;
  OffsetDateTime estimatedOn;
}
