package my.lokalix.planning.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class TextUtils {
  public static <T> String concatenateListWithSeparator(List<T> list) {
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }

    return list.stream()
        .map(
            item -> {
              if (item == null) {
                throw new IllegalArgumentException("List contains null item");
              }
              return item.toString();
            })
        .collect(Collectors.joining(GlobalConstants.SEPARATOR));
  }

  public static <T> List<T> splitConcatenatedListWithSeparator(
      String concatenatedList, Function<String, T> converter) {

    if (StringUtils.isBlank(concatenatedList)) {
      return new ArrayList<>();
    }

    return Arrays.stream(concatenatedList.split(GlobalConstants.SEPARATOR_REGEX))
        .map(converter)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Compare naturally two strings, meaning A060 would be before A70 for example.
   *
   * @param s1
   * @param s2
   * @return
   */
  public static int compareNaturally(String s1, String s2) {
    int i = 0, j = 0;
    while (i < s1.length() && j < s2.length()) {
      char c1 = s1.charAt(i);
      char c2 = s2.charAt(j);
      if (Character.isDigit(c1) && Character.isDigit(c2)) {
        int start1 = i, start2 = j;
        while (i < s1.length() && Character.isDigit(s1.charAt(i))) i++;
        while (j < s2.length() && Character.isDigit(s2.charAt(j))) j++;
        long num1 = Long.parseLong(s1.substring(start1, i));
        long num2 = Long.parseLong(s2.substring(start2, j));
        if (num1 != num2) return Long.compare(num1, num2);
      } else {
        int cmp = Character.compare(c1, c2);
        if (cmp != 0) return cmp;
        i++;
        j++;
      }
    }
    return Integer.compare(s1.length() - i, s2.length() - j);
  }
}
