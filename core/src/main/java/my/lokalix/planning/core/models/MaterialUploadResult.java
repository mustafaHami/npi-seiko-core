package my.lokalix.planning.core.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MaterialUploadResult {
  private final int totalCreated;
  private final int newSuppliersAdded;
  private final int newMoqLinesAdded;
  private final int updatedMoqLines;
  private final String manufacturerNotFoundCsvPath;
  private final String suppliersNotFoundCsvPath;
}
