package my.lokalix.planning.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberUtils {
  private static final DecimalFormat dfAlwaysTwoDigits =
      new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
  private static final DecimalFormat dfTwoDigits =
      new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.ENGLISH));

  public static String formatDoubleToAlwaysTwoDigits(Double value) {
    if (value == null) {
      return null;
    }
    return dfAlwaysTwoDigits.format(value);
  }

  public static String formatDoubleToTwoDigits(Double value) {
    if (value == null) {
      return null;
    }
    String str = dfTwoDigits.format(value);
    if (str.endsWith(".0")) {
      str = str.substring(0, str.length() - 2);
    }
    return str;
  }

  public static String shortenQuantity(Integer qty) {
    if (qty == null) {
      return null;
    }
    if (qty % 1_000_000 == 0 && qty >= 1_000_000) {
      return (qty / 1_000_000) + "M";
    } else if (qty % 1_000 == 0 && qty >= 1_000) {
      return (qty / 1_000) + "K";
    } else {
      return Integer.toString(qty);
    }
  }

  public static double minutesToHours(double minutes) {
    return minutes / 60;
  }

  public static BigDecimal doubleToBigDecimalWithTwoDecimals(double value) {
    return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
  }

  public static boolean isNullOrNotStrictlyPositive(BigDecimal value) {
    return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
  }
}
