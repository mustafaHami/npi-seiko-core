package my.lokalix.planning.core.services.helper;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import my.lokalix.planning.core.models.entities.admin.MaterialCategoryEntity;
import my.lokalix.planning.core.models.entities.admin.SupplierManufacturerEntity;
import my.lokalix.planning.core.repositories.MaterialRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemIdHelper {

  private final MaterialRepository materialRepository;

  /**
   * Generates a unique 14-character system ID with the format: [manufacturerCode ≤5][catAbbrev
   * 3][mpn 3 non-space]#[sequential digits]
   *
   * <p>The sequential number is determined by fetching the highest existing systemId sharing the
   * same prefix from the database and incrementing it.
   */
  public String generateSystemId(
      SupplierManufacturerEntity manufacturer,
      MaterialCategoryEntity category,
      String manufacturerPartNumber) {
    if (manufacturer == null || category == null || StringUtils.isBlank(manufacturerPartNumber)) {
      return null;
    }
    String prefix = buildSystemIdPrefix(manufacturer, category, manufacturerPartNumber);
    int numDigits = 14 - prefix.length();

    int nextNumber = findNextSequentialNumber(prefix);
    String paddedNumber = String.format("%0" + numDigits + "d", nextNumber);

    return prefix + paddedNumber;
  }

  public String buildSystemIdPrefix(
      SupplierManufacturerEntity manufacturer,
      MaterialCategoryEntity category,
      String manufacturerPartNumber) {
    String manufacturerCode =
        manufacturer
            .getCode()
            .toUpperCase()
            .substring(0, Math.min(5, manufacturer.getCode().length()));

    String catAbbrev =
        category
            .getAbbreviation()
            .toUpperCase()
            .substring(0, Math.min(3, category.getAbbreviation().length()));

    String mpnPart = extractFirstNonSpaceChars(manufacturerPartNumber, 3).toUpperCase();

    return manufacturerCode + catAbbrev + mpnPart + "#";
  }

  private int findNextSequentialNumber(String prefix) {
    List<String> result =
        materialRepository.findHighestSystemIdByPrefix(prefix, PageRequest.of(0, 1));
    Optional<String> highest = result.stream().findFirst();
    if (highest.isEmpty()) {
      return 1;
    }
    try {
      return Integer.parseInt(highest.get().substring(prefix.length())) + 1;
    } catch (NumberFormatException ignored) {
      return 1;
    }
  }

  private String extractFirstNonSpaceChars(String input, int count) {
    if (StringUtils.isBlank(input)) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (char c : input.toCharArray()) {
      if (c != ' ' && sb.length() < count) {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
