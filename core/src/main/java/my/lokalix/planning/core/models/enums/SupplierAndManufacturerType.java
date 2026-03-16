package my.lokalix.planning.core.models.enums;

import java.util.List;
import lombok.Getter;

public enum SupplierAndManufacturerType {
  SUPPLIER("SUPPLIER", "Supplier"),

  MANUFACTURER("MANUFACTURER", "Manufacturer"),

  BOTH("BOTH", "Supplier and Manufacturer");

  private final String value;
  @Getter private final String humanReadableValue;

  SupplierAndManufacturerType(String value, String humanReadableValue) {
    this.value = value;
    this.humanReadableValue = humanReadableValue;
  }

  public static SupplierAndManufacturerType fromValue(String value) {
    for (SupplierAndManufacturerType b : SupplierAndManufacturerType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

    public static List<SupplierAndManufacturerType> getManufacturerTypes() {
      return List.of(MANUFACTURER, BOTH);
    }

  public static List<SupplierAndManufacturerType> getSupplierTypes() {
    return List.of(SUPPLIER, BOTH);
  }

    public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
