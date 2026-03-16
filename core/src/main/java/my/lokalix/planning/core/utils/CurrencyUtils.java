package my.lokalix.planning.core.utils;

import org.apache.commons.lang3.StringUtils;

public class CurrencyUtils {

  public static String getCurrencyPrefix(String currencyCode) {
    if (StringUtils.isBlank(currencyCode)) return "";
    return switch (currencyCode) {
      case "MYR" -> "RM ";
      case "USD" -> "US$ ";
      case "EUR" -> "\u20AC ";
      case "GBP" -> "\u00A3 ";
      case "SGD" -> "SGD ";
      case "JPY" -> "JP\u00A5 ";
      case "CNY" -> "CN\u00A5 ";
      case "HKD" -> "HK$ ";
      default -> currencyCode + " ";
    };
  }
}
