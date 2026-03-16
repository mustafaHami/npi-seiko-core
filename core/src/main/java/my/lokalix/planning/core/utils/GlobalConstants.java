package my.lokalix.planning.core.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import my.lokalix.planning.core.models.entities.admin.GlobalConfigEntity;

public final class GlobalConstants {
  public static final String ZIP_FILE = "all_files.zip";
  public static final String HEADER_UID = "uid";
  public static final List<String> ALLOWED_FILE_EXTENSIONS =
      Arrays.asList(
          "dwg", "pdf", "stpz", "steps", "stp", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg",
          "gif");
  public static final String SEPARATOR = " ||| ";
  public static final String SEPARATOR_REGEX = Pattern.quote(SEPARATOR);
  public static final String ALL_CUSTOMERS = "ALL_CUSTOMERS";

  // Other cost line fixed names
  public static final String OTHER_COST_LINE_SHIPMENT_TO_CUSTOMER = "Shipment to Customer";
  public static final String OTHER_COST_LINE_EXTRA_PROCESS_COST = "Extra Process Cost";
  public static final String OTHER_COST_LINE_LABOR_COST = "Labor Cost";
  public static final String OTHER_COST_LINE_OVERHEAD_COST = "Overhead Cost";
  public static final String OTHER_COST_LINE_INTERNAL_TRANSPORTATION = "Internal Transportation";
  public static final String OTHER_COST_LINE_DEPRECIATION_COST = "Depreciation Cost";
  public static final String OTHER_COST_LINE_ADMINISTRATION_COST = "Administration Cost";
  public static final String OTHER_COST_LINE_STANDARD_JIGS_AND_FIXTURES =
      "Standard Jigs And Fixtures";
  public static final String OTHER_COST_LINE_PACKAGING_COST = "Packaging Cost";

  public static Map<String, BigDecimal> extractOtherCostNamesFromGlobalConfig(
      GlobalConfigEntity globalConfig) {
    Map<String, BigDecimal> nameToValue = new HashMap<>();
    nameToValue.put(GlobalConstants.OTHER_COST_LINE_LABOR_COST, globalConfig.getLaborCost());
    nameToValue.put(GlobalConstants.OTHER_COST_LINE_OVERHEAD_COST, globalConfig.getOverheadCost());
    nameToValue.put(
        GlobalConstants.OTHER_COST_LINE_INTERNAL_TRANSPORTATION,
        globalConfig.getInternalTransportation());
    nameToValue.put(
        GlobalConstants.OTHER_COST_LINE_DEPRECIATION_COST, globalConfig.getDepreciationCost());
    nameToValue.put(
        GlobalConstants.OTHER_COST_LINE_ADMINISTRATION_COST, globalConfig.getAdministrationCost());
    nameToValue.put(
        GlobalConstants.OTHER_COST_LINE_STANDARD_JIGS_AND_FIXTURES,
        globalConfig.getStandardJigsAndFixturesCost());
    nameToValue.put(
        GlobalConstants.OTHER_COST_LINE_PACKAGING_COST, globalConfig.getSmallPackagingCost());
    return nameToValue;
  }
}
