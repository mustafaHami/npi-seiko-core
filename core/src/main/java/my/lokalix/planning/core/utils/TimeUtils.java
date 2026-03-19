package my.lokalix.planning.core.utils;

import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

public final class TimeUtils {
  public static OffsetDateTime toOffsetDateTimeStartOfDay(LocalDate localDate, String zone) {
    OffsetDateTime offsetDateTime =
        localDate.atStartOfDay().atZone(ZoneId.of(zone)).toOffsetDateTime();
    // Ensure time is set to midnight
    if (!offsetDateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      offsetDateTime = offsetDateTime.with(LocalTime.MIDNIGHT);
    }
    return offsetDateTime;
  }

  public static OffsetDateTime toOffsetDateTimeEndOfDay(LocalDate localDate, String zone) {
    OffsetDateTime offsetDateTime =
        localDate.atTime(23, 59, 59, 999999999).atZone(ZoneId.of(zone)).toOffsetDateTime();
    return offsetDateTime;
  }

  public static OffsetDateTime nowOffsetDateTimeUTC() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  public static double daysToMinutes(double timeInDays) {
    return timeInDays * 24 * 60;
  }

  public static double daysToHours(double timeInDays) {
    return timeInDays * 24;
  }

  public static double minutesToDays(long timeInMinutes) {
    return timeInMinutes / (60 * 24d);
  }

  public static OffsetDateTime nowOffsetDateTime(String zone) {
    return OffsetDateTime.now(ZoneId.of(zone));
  }

  public static LocalDate nowLocalDate(String zone) {
    return LocalDate.now(ZoneId.of(zone));
  }

  public static LocalDateTime nowLocalDateTime(String zone) {
    return LocalDateTime.now(ZoneId.of(zone));
  }

  public static LocalDate toLocalDate(OffsetDateTime dateTime, String zone) {
    return dateTime.toInstant().atZone(ZoneId.of(zone)).toLocalDate();
  }

  public static OffsetDateTime convertZonedFirstTimeOfMonthToUTC(LocalDate date, String zone) {
    ZonedDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.of(zone));
    return startOfMonth.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
  }

  public static OffsetDateTime convertZonedLastTimeOfMonthToUTC(LocalDate date, String zone) {
    ZonedDateTime startOfMonth =
        LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.of(zone));
    return startOfMonth.minusSeconds(1).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
  }

  public static OffsetDateTime startingDayOfWeeksAgoFromNow(int numberOfWeeks, String zone) {
    LocalDate today = LocalDate.now(ZoneId.of(zone));
    int currentWeekNumber = weekNumberFromDate(today);
    int weekNumber =
        currentWeekNumber
            - numberOfWeeks
            + 1; // +1 to not consider the last week, as the current week already count for one

    LocalDate startOfYear = today.with(TemporalAdjusters.firstDayOfYear());
    LocalDate startOfWeek =
        startOfYear
            .plusWeeks(weekNumber - 1)
            .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    return startOfWeek.atStartOfDay(ZoneId.of(zone)).toOffsetDateTime();
  }

  public static Integer weekNumberFromDate(OffsetDateTime date, String zone) {
    if (date == null) {
      return null;
    }
    ZonedDateTime zonedDateTime = date.atZoneSameInstant(ZoneId.of(zone));
    return zonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
  }

  public static Integer weekNumberFromDate(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
  }

  public static String formatAsStringInZone(OffsetDateTime date, String zone) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of(zone));
    return date.format(formatter);
  }

  public static OffsetDateTime toOffsetDateTime(LocalDate localDate, String zone) {
    OffsetDateTime offsetDateTime =
        localDate.atStartOfDay().atZone(ZoneId.of(zone)).toOffsetDateTime();
    // Ensure time is set to midnight
    if (!offsetDateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      offsetDateTime = offsetDateTime.with(LocalTime.MIDNIGHT);
    }
    return offsetDateTime;
  }

  public static OffsetDateTime replaceOffset(OffsetDateTime offsetDateTime, String zone) {
    return offsetDateTime.atZoneSameInstant(ZoneId.of(zone)).toOffsetDateTime();
  }

  public static double numberOfDaysBetweenDates(
      @NotNull OffsetDateTime creationDate, OffsetDateTime finalizationDate) {
    Duration duration = Duration.between(creationDate, finalizationDate);
    // total seconds, including fractions of days
    double seconds = duration.toSeconds();
    return seconds / 86400.0; // 24 * 60 * 60
  }

  /**
   * Adds the given number of business days (Monday–Friday) to a start date.
   * If the start date itself is a weekend, moves to the next Monday first.
   */
  public static LocalDate addBusinessDays(LocalDate startDate, long businessDays) {
    LocalDate date = skipWeekend(startDate);
    long added = 0;
    while (added < businessDays) {
      date = date.plusDays(1);
      if (!isWeekend(date)) {
        added++;
      }
    }
    return date;
  }

  /**
   * Counts the number of business days (Monday–Friday) between two dates (exclusive of end).
   * Returns a non-negative value; if end is before start, returns 0.
   */
  public static long businessDaysBetween(LocalDate start, LocalDate end) {
    if (!end.isAfter(start)) return 0;
    long count = 0;
    LocalDate date = start;
    while (date.isBefore(end)) {
      if (!isWeekend(date)) {
        count++;
      }
      date = date.plusDays(1);
    }
    return count;
  }

  private static boolean isWeekend(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
  }

  private static LocalDate skipWeekend(LocalDate date) {
    while (isWeekend(date)) {
      date = date.plusDays(1);
    }
    return date;
  }
}
